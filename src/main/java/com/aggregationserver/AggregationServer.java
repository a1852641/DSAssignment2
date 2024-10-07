package com.aggregationserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The AggregationServer class handles weather data aggregation and communication with clients.
 * It provides endpoints for clients to GET weather data and PUT new data entries.
 * Weather data expires after 30 seconds, and expired entries are periodically removed.
 * Lamport clocks are used to maintain a consistent ordering of events across distributed systems.
 */
public class AggregationServer {

    protected static final String FILE_PATH = "weatherData.json";  // Path to persist weather data
    protected static final int MAX_ENTRIES = 20;  // Maximum number of weather data entries to store
    protected static final int EXPIRATION_TIME = 30;  // Time in seconds after which entries expire
    protected static Map<String, WeatherEntry> weatherData = new LinkedHashMap<>();  // Store weather data with timestamps
    protected static LamportClock lamportClock = new LamportClock();  // Lamport clock for synchronization

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4567;

        // Schedule periodic removal of expired entries every 30 seconds
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            removeExpiredEntries();
        }, 30, 30, TimeUnit.SECONDS);

        // Load existing weather data from file
        loadDataFromFile();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server running on port " + port + "...");

            // Continuously accept client connections and handle requests
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    String requestLine = in.readLine();
                    if (requestLine != null) {
                        System.out.println("Received request: " + requestLine);  // Log the incoming request
                    }

                    String stationID = null;  // Initialize station ID as null

                    // Handle GET request
                    if (requestLine != null && requestLine.startsWith("GET")) {
                        if (requestLine.contains("/weather/")) {
                            stationID = extractStationIDFromRequest(requestLine);
                        }
                        handleGetRequest(out, stationID);  // Pass PrintWriter and stationID to GET handler
                    } 
                    // Handle PUT request
                    else if (requestLine != null && requestLine.startsWith("PUT")) {
                        handlePutRequest(in, out);  // Pass input and output streams to PUT handler
                    } 
                    // Handle bad request
                    else {
                        out.println("HTTP/1.1 400 Bad Request");
                    }

                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Extracts the station ID from a GET request path.
     * @param requestLine The full HTTP GET request line
     * @return The station ID as a string if found, otherwise null
     */
    protected static String extractStationIDFromRequest(String requestLine) {
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length > 1) {
            String path = requestParts[1];  // Example: /weather/IDS60901
            if (path.startsWith("/weather/")) {
                return path.substring("/weather/".length());  // Extract station ID
            }
        }
        return null;
    }

    /**
     * Handles a GET request by returning weather data.
     * If stationID is provided, returns data for that specific station.
     * If no stationID is provided, returns all available weather data.
     * 
     * @param out       PrintWriter to send the HTTP response to the client
     * @param stationID Station ID to retrieve data for (optional)
     */
    protected static void handleGetRequest(PrintWriter out, String stationID) {
        try {
            System.out.println("Received GET request for stationID: " + stationID);
            String responseBody;

            // Check if stationID exists in the weather data
            if (stationID != null && weatherData.containsKey(stationID)) {
                WeatherEntry entry = weatherData.get(stationID);
                JSONParser parser = new JSONParser();
                responseBody = parser.stringify(entry.data);
                System.out.println("Weather data for stationID " + stationID + " found. Sending data to client.");
            } 
            // Return data for all stations if no specific stationID is requested
            else if (stationID == null || stationID.isEmpty()) {
                responseBody = getAllWeatherDataAsJson();
                System.out.println("All weather data found. Sending all stations' data to client.");
            } 
            // Handle stationID not found
            else {
                responseBody = "Station data not found.";
                System.out.println("StationID " + stationID + " not found.");
            }

            // Send the HTTP response
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/json");
            out.println("Content-Length: " + responseBody.length());
            out.println();  // End of headers
            out.println(responseBody);  // Send the body

            System.out.println("Response sent to client for stationID: " + (stationID != null ? stationID : "all stations"));
        } catch (Exception e) {
            out.println("HTTP/1.1 500 Internal Server Error");
            System.err.println("Error handling GET request: " + e.getMessage());
        }
    }

    /**
     * Retrieves all weather data and converts it to a JSON string.
     * @return JSON string containing all weather data
     */
    protected static String getAllWeatherDataAsJson() {
        Map<String, Object> allWeatherData = new LinkedHashMap<>();
        for (Map.Entry<String, WeatherEntry> entry : weatherData.entrySet()) {
            allWeatherData.put(entry.getKey(), entry.getValue().data);
        }
        JSONParser parser = new JSONParser();
        return parser.stringify(allWeatherData);
    }

    /**
     * Handles a PUT request to add or update weather data.
     * 
     * @param in  BufferedReader to read the incoming data
     * @param out PrintWriter to send the HTTP response
     * @throws IOException If an I/O error occurs
     */
    protected static void handlePutRequest(BufferedReader in, PrintWriter out) throws IOException {
        String line;
        int contentLength = 0;
        StringBuilder jsonString = new StringBuilder();

        // Read the HTTP headers to find the Content-Length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        // Read the body based on Content-Length
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        jsonString.append(body);

        if (jsonString.toString().isEmpty()) {
            out.println("HTTP/1.1 204 No Content");
            return;
        }

        processPutRequest(jsonString.toString(), out);
    }

    /**
     * Processes the weather data from a PUT request, updates the weatherData map,
     * and maintains a maximum of 20 entries.
     * 
     * @param jsonString JSON string representing the new weather data
     * @param out        PrintWriter to send the HTTP response
     */
    protected static void processPutRequest(String jsonString, PrintWriter out) {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> newWeatherData = (Map<String, Object>) parser.parse(jsonString);

            if (!newWeatherData.containsKey("id")) {
                out.println("HTTP/1.1 400 Bad Request");
                return;
            }

            String id = (String) newWeatherData.get("id");

            // Add timestamps and Lamport clock to the new data entry
            newWeatherData.put("lastUpdated", System.currentTimeMillis());

            boolean isNewEntry = !weatherData.containsKey(id);
            weatherData.remove(id);  // Remove the old entry if it exists

            weatherData.put(id, new WeatherEntry(newWeatherData, System.currentTimeMillis(), lamportClock.getTime()));
            lamportClock.tick();  // Increment Lamport clock after data change

            maintainMaxEntries();  // Ensure no more than MAX_ENTRIES exist
            saveDataToFile();

            // Respond with appropriate status code
            out.println(isNewEntry ? "HTTP/1.1 201 Created" : "HTTP/1.1 200 OK");

        } catch (Exception e) {
            out.println("HTTP/1.1 500 Internal Server Error");
            System.err.println("Error processing PUT request: " + e.getMessage());
        }
    }

    /**
     * Ensures that the number of weather entries does not exceed the maximum limit.
     * If the limit is exceeded, the oldest entry is removed.
     */
    protected static void maintainMaxEntries() {
        if (weatherData.size() > MAX_ENTRIES) {
            String oldestEntryId = weatherData.keySet().iterator().next();
            System.out.println("Removing oldest entry: " + oldestEntryId);
            weatherData.remove(oldestEntryId);
        }
    }

    /**
     * Loads weather data from a JSON file into the weatherData map.
     * If the file does not exist or cannot be read, the method logs an error.
     */
    protected static void loadDataFromFile() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder jsonData = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonData.append(line);
                }

                JSONParser parser = new JSONParser();
                Map<String, Map<String, Object>> rawData = (Map<String, Map<String, Object>>) parser.parse(jsonData.toString());

                weatherData.clear();  // Clear current data before loading from file

                for (Map.Entry<String, Map<String, Object>> entry : rawData.entrySet()) {
                    String stationID = entry.getKey();
                    Map<String, Object> weatherInfo = entry.getValue();
                    long lastUpdated = (long) weatherInfo.get("lastUpdated");
                    int lamportTime = ((Number) weatherInfo.get("lamportTime")).intValue();
                    weatherData.put(stationID, new WeatherEntry(weatherInfo, lastUpdated, lamportTime));
                }

            } catch (Exception e) {
                System.err.println("Error loading data from file: " + e.getMessage());
            }
        }
    }

    /**
     * Saves weather data to a JSON file, ensuring atomic write with a temporary file.
     * Data is sorted by Lamport clock values in descending order before saving.
     */
    protected static void saveDataToFile() throws IOException {
        File tempFile = new File(FILE_PATH + ".tmp");
        File originalFile = new File(FILE_PATH);

        // Sort weather data by Lamport timestamp in descending order
        List<Map.Entry<String, WeatherEntry>> sortedEntries = new ArrayList<>(weatherData.entrySet());
        sortedEntries.sort((entry1, entry2) -> Integer.compare(entry2.getValue().lamportTime, entry1.getValue().lamportTime));

        // Prepare the data to be saved
        Map<String, Map<String, Object>> dataToSave = new LinkedHashMap<>();
        for (Map.Entry<String, WeatherEntry> entry : sortedEntries) {
            dataToSave.put(entry.getKey(), entry.getValue().data);
        }

        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            JSONParser parser = new JSONParser();
            String jsonString = parser.stringify(dataToSave, 2);  // Pretty print JSON with indentation
            fileWriter.write(jsonString);
            fileWriter.flush();
        } catch (Exception e) {
            System.err.println("Error saving data to temporary file: " + e.getMessage());
            return;
        }

        // Replace the original file with the new one
        if (originalFile.exists() && !originalFile.delete()) {
            System.err.println("Failed to delete the original file.");
            return;
        }

        if (!tempFile.renameTo(originalFile)) {
            System.err.println("Failed to rename the temporary file to the original file.");
        }
    }

    /**
     * Removes weather data entries that have not been updated within the expiration time (30 seconds).
     * The method checks the timestamps of each entry and deletes expired entries.
     */
    protected static void removeExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, WeatherEntry>> iterator = weatherData.entrySet().iterator();
        boolean entriesRemoved = false;

        // Iterate over the weather data entries and remove expired ones
        while (iterator.hasNext()) {
            Map.Entry<String, WeatherEntry> entry = iterator.next();
            if ((currentTime - entry.getValue().lastUpdated) > EXPIRATION_TIME * 1000) {
                System.out.println("Removing stale entry for station id: " + entry.getKey());
                iterator.remove();
                entriesRemoved = true;
            }
        }

        // Save updated data to file if any entries were removed
        if (entriesRemoved) {
            try {
                saveDataToFile();
            } catch (IOException e) {
                System.err.println("Error saving updated data after removing expired entries: " + e.getMessage());
            }
        }
    }

    // WeatherEntry class representing individual weather data with a timestamp and Lamport clock value
    static class WeatherEntry {
        Map<String, Object> data;
        long lastUpdated;
        int lamportTime;

        WeatherEntry(Map<String, Object> data, long lastUpdated, int lamportTime) {
            this.data = data;
            this.lastUpdated = lastUpdated;
            this.lamportTime = lamportTime;
        }
    }
}
