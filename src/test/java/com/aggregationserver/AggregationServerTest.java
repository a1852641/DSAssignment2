package com.aggregationserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class AggregationServerTest {

    private AggregationServer aggregationServer;

    @BeforeEach
    public void setup() {
        aggregationServer = new AggregationServer();
    }

    @Test
    public void testPutRequestStoresData() throws Exception {
        // Simulate weather data in JSON format
        String weatherDataJson = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";

        // Create BufferedReader to simulate client input
        BufferedReader in = new BufferedReader(new StringReader(weatherDataJson));

        // Create PrintWriter to simulate server's response output
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter, true);

        // Call the method with simulated input/output
        aggregationServer.handlePutRequest(in, out);

        // Verify that the data is stored correctly
        Map<String, Object> weatherData = aggregationServer.getWeatherData("IDS60901");
        assertNotNull(weatherData, "Weather data should be stored.");
        assertEquals("25.0", weatherData.get("temperature"), "Temperature should match.");
        assertEquals("60", weatherData.get("humidity"), "Humidity should match.");
    }

    @Test
    public void testGetSpecificStationData() throws Exception {
        // Add sample data first
        String weatherDataJson = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";
        aggregationServer.handlePutRequest(weatherDataJson);

        // Simulate GET request for a specific station
        String response = aggregationServer.handleGetRequest("IDS60901");

        // Verify the response contains expected data
        assertTrue(response.contains("\"id\":\"IDS60901\""), "Response should contain the station ID.");
        assertTrue(response.contains("\"temperature\":\"25.0\""), "Response should contain the correct temperature.");
        assertTrue(response.contains("\"humidity\":\"60\""), "Response should contain the correct humidity.");
    }

    @Test
    public void testGetAllStationsData() throws Exception {
        // Add sample data for two stations
        String station1Data = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";
        String station2Data = "{\"id\":\"IDS60902\", \"temperature\":\"20.0\", \"humidity\":\"50\"}";
        
        aggregationServer.handlePutRequest(station1Data);
        ggregationServer.handlePutRequest(station2Data);

        // Simulate GET request for all stations
        String response = aggregationServer.handleGetRequest(null);

        // Verify the response contains data from both stations
        assertTrue(response.contains("\"IDS60901\""), "Response should contain station 1 data.");
        assertTrue(response.contains("\"IDS60902\""), "Response should contain station 2 data.");
    }
}
