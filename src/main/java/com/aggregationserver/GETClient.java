package com.aggregationserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GETClient connects to the AggregationServer to retrieve weather data.
 * It sends GET requests for all weather data or specific station data and processes the JSON response.
 */
public class GETClient {

    private static LamportClock lamportClock = new LamportClock();

    public static void main(String[] args) {
        // Check for proper argument usage
        if (args.length < 1) {
            System.out.println("Usage: GETClient <serverHostPort> [stationID]");
            return;
        }

        String serverHostPort = args[0];  // Server host and port in the form of "host:port"
        String stationID = args.length > 1 ? args[1] : null;  // Optional station ID for specific weather data

        try {
            // Send GET request to server and get response
            String[] responseParts = sendGetRequest(serverHostPort, stationID);
            if (responseParts != null) {
                System.out.println("Server Response:");
                System.out.println(responseParts[0]);  // Print response headers (status code, headers)

                // Parse and display the JSON response body
                displayWeatherData(responseParts[1]);
            } else {
                System.err.println("No data received from the server.");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Sends a GET request to the specified server for weather data.
     * @param serverHostPort The server host and port in the form "host:port"
     * @param stationID Optional station ID to retrieve data for a specific station
     * @return An array containing response headers and the body
     * @throws IOException If an I/O error occurs during the network communication
     */
    public static String[] sendGetRequest(String serverHostPort, String stationID) throws IOException {
        String host;
        int port;

        // Extract the host and port using regex
        String regex = "(http://)?([^:/]+)(:(\\d+))?";
        Matcher matcher = Pattern.compile(regex).matcher(serverHostPort);

        if (matcher.find()) {
            host = matcher.group(2);  // Extract the host (e.g., example.com)
            port = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 80;  // Default to port 80 if not provided
        } else {
            throw new IllegalArgumentException("Invalid serverHostPort format.");
        }

        // Open a socket to the server and send the GET request
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Construct the GET request with an optional stationID
            String getRequest = "GET /weather";
            if (stationID != null && !stationID.isEmpty()) {
                getRequest += "/" + stationID;
            }
            getRequest += " HTTP/1.1";
            out.println(getRequest);
            out.println("User-Agent: GETClient/1.0");
            out.println("Host: " + host);
            out.println();

            // Increment Lamport clock for this GET request
            lamportClock.tick();

            // Read and process the server response
            StringBuilder headers = new StringBuilder();
            StringBuilder body = new StringBuilder();
            String responseLine;
            boolean isJson = false;

            // Parse the response, distinguishing between headers and body
            while ((responseLine = in.readLine()) != null) {
                if (responseLine.isEmpty()) {
                    isJson = true;  // Reached the end of headers
                    continue;
                }

                if (isJson) {
                    body.append(responseLine).append("\n");  // Read the JSON body
                } else {
                    headers.append(responseLine).append("\n");  // Read headers
                }
            }

            // Return both headers and body
            return new String[]{headers.toString(), body.toString()};
        }
    }

    /**
     * Parses and displays the weather data from the JSON response.
     * @param jsonResponse The JSON response from the server containing weather data
     */
    public static void displayWeatherData(String jsonResponse) {
        try {            
            // Parse the JSON response
            JSONParser parser = new JSONParser();
            Object parsedData = parser.parse(jsonResponse);
    
            // Check if the parsed data is a Map (i.e., a JSON object)
            if (parsedData instanceof Map) {
                Map<String, Object> weatherData = (Map<String, Object>) parsedData;
                
                // Iterate through the weather data and display key-value pairs
                for (Map.Entry<String, Object> entry : weatherData.entrySet()) {
                    String stationID = entry.getKey();
                    Object stationData = entry.getValue();
    
                    System.out.println("Station ID: " + stationID);
    
                    // Check if stationData is a Map or something else
                    if (stationData instanceof Map) {
                        Map<String, Object> stationDataMap = (Map<String, Object>) stationData;
                        for (Map.Entry<String, Object> dataEntry : stationDataMap.entrySet()) {
                            System.out.println(dataEntry.getKey() + ": " + dataEntry.getValue());
                        }
                    } else {
                        // If it's not a Map, just print the value
                        System.out.println("Data: " + stationData);
                    }
    
                    System.out.println();  // Add spacing between different stations
                }
            } else if (parsedData instanceof String) {
                // Handle the case where the JSON is just a string
                System.out.println("Received a simple string: " + parsedData);
            } else {
                System.out.println("Unexpected data type received: " + parsedData.getClass().getName());
            }
        } catch (Exception e) {
            System.err.println("Error parsing or displaying weather data: " + e.getMessage());
        }
    }
    
    
    /**
     * Sends a GET request and retrieves the weather data as a JSON string.
     * This is a helper method that can be used by other classes.
     * @param serverHostPort The server host and port in the form "host:port"
     * @param stationID Optional station ID for specific weather data
     * @return A string containing the weather data in JSON format
     * @throws IOException If an I/O error occurs during the network communication
     */
    public String getWeatherData(String serverHostPort, String stationID) throws IOException {
        String[] responseParts = sendGetRequest(serverHostPort, stationID);
        return responseParts != null ? responseParts[1] : null;
    }

    /**
     * Retrieves the current Lamport clock instance.
     * @return The LamportClock instance used by this client
     */
    public LamportClock getLamportClock() {
        return this.lamportClock;
    }
}
