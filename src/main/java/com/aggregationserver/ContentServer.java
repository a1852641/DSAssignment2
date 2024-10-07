package com.aggregationserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The ContentServer reads weather data from a file and sends it to an AggregationServer periodically.
 * It maintains a connection with the server and ensures that data is sent every 20 seconds.
 */
public class ContentServer {

    private static LamportClock lamportClock = new LamportClock();  // Lamport clock for synchronization
    private static Map<String, String> dataStore = new HashMap<>();  // Store weather data
    private static JSONParser jsonParser = new JSONParser();  // Utility to handle JSON conversion
    private static boolean isFirstConnection = true;  // Track first connection for a custom message

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ContentServer <serverHostPort> <dataFilePath>");
            return;
        }

        String serverHostPort = args[0];  // Host and port of the AggregationServer
        String filePath = args[1];  // Path to the file containing weather data

        System.out.println("Starting ContentServer... Host: " + serverHostPort + ", File: " + filePath);

        try {
            // Maintain connection and send data to the AggregationServer every 20 seconds
            maintainConnectionAndSendData(serverHostPort, filePath);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * This method keeps the connection alive and sends weather data to the AggregationServer every 20 seconds.
     * It uses a scheduled executor service to handle periodic data transmissions.
     * 
     * @param serverHostPort The server host and port in the format "host:port"
     * @param filePath The file path to the weather data
     * @throws IOException If an I/O error occurs while establishing the connection
     */
    public static void maintainConnectionAndSendData(String serverHostPort, String filePath) throws IOException {
        String[] hostPort = serverHostPort.split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        // Scheduled task to send data every 20 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Open a new connection for each PUT request
                try (Socket socket = new Socket(host, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Custom message for the first connection
                    if (isFirstConnection) {
                        System.out.println("Connected to server, sending data...");
                        isFirstConnection = false;
                    } else {
                        System.out.println("Sending data to server...");
                    }

                    // Read weather data from the file and send it to the server
                    Map<String, Object> weatherData = readFileToJSON(filePath);
                    sendPutRequest(out, weatherData);

                    // Read and print server responses
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        System.out.println("Response from server: " + responseLine);
                    }

                } catch (IOException e) {
                    System.err.println("Error during PUT request: " + e.getMessage());
                }
            } catch (Exception e) { // Handle file reading or other exceptions
                System.err.println("Error reading weather data file: " + e.getMessage());
            }
        }, 0, 20, TimeUnit.SECONDS);
    }

    /**
     * Reads the weather data from a file and returns it as a Map.
     * The file should contain key-value pairs where each entry is separated by a colon.
     * 
     * @param filePath The file path to the weather data
     * @return A Map containing the weather data
     * @throws FileNotFoundException If the file cannot be found
     */
    public static Map<String, Object> readFileToJSON(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        Map<String, Object> weatherData = new HashMap<>();
        boolean idFound = false;  // Track if 'id' field is found

        // Read the file line by line
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String[] entry = scanner.nextLine().split(":");
                if (entry.length == 2) {
                    String key = entry[0].trim();
                    String value = entry[1].trim();
                    weatherData.put(key, value);

                    if (key.equals("id")) {
                        if (value.isEmpty()) {
                            throw new IllegalArgumentException("Error: Missing or empty 'id' field.");
                        }
                        idFound = true;
                    }
                }
            }

            // Ensure that the 'id' field is present in the data
            if (!idFound) {
                throw new IllegalArgumentException("Error: No 'id' field found.");
            }

            // Add Lamport time to synchronize the data
            weatherData.put("lamportTime", lamportClock.getTime());
        }

        return weatherData;
    }

    /**
     * Sends the PUT request to the AggregationServer containing the weather data.
     * 
     * @param out The PrintWriter to send data to the server
     * @param weatherData The weather data to be sent as a Map
     */
    public static void sendPutRequest(PrintWriter out, Map<String, Object> weatherData) {
        // Convert the weather data Map to a JSON string
        String jsonString = jsonParser.stringify(weatherData);

        // Construct and send the PUT request
        out.println("PUT /weather.json HTTP/1.1");
        out.println("User-Agent: ContentServer/1.0");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonString.length());
        out.println();  // End of headers
        out.println(jsonString);  // Send the JSON string

        lamportClock.tick();  // Increment Lamport clock after the data change
    }
}
