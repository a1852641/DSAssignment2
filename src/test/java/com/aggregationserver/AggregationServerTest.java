package com.aggregationserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AggregationServerTest {

    private AggregationServer aggregationServer;
    private PrintWriter mockOut;
    private BufferedReader mockIn;

    @BeforeEach
    public void setup() {
        aggregationServer = new AggregationServer();  // Initialize the server
        mockOut = mock(PrintWriter.class);  // Mock the PrintWriter
        mockIn = mock(BufferedReader.class);  // Mock the BufferedReader
    }

    // Test handling a valid PUT request and storing the weather data
    @Test
    public void testHandlePutRequest_ValidData_FromFile() throws Exception {
        // Load the sample data from file
        File file = new File("src/test/resources/sample_weather_data.txt");
        String weatherData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        // Mock socket, input, and output streams for the test
        Socket mockSocket = mock(Socket.class);
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        PrintWriter mockOut = mock(PrintWriter.class);

        // Mock socket input to simulate the incoming data
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(weatherData.getBytes()));
        when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        // Create the AggregationServer instance and process the request
        AggregationServer server = new AggregationServer();
        server.processPutRequest("PUT /weather.json", mockOut);


        // Verify the server's response to a valid PUT request (200 OK)
        verify(mockOut).println("HTTP/1.1 200 OK");
    }

    // Test handling a GET request for a specific station
    @Test
    public void testHandleGetRequest_SpecificStation() throws Exception {
        // Add mock weather data for station IDS60901
        String stationID = "IDS60901";
        String validJson = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";
        aggregationServer.processPutRequest(validJson, mockOut);

        // Call the GET handler for the specific station
        aggregationServer.handleGetRequest(mockOut, stationID);

        // Verify that the response contains the correct weather data
        verify(mockOut, times(1)).println(contains("25.0"));
        verify(mockOut, times(1)).println(contains("60"));
    }

    // Test handling a GET request for all stations
    @Test
    public void testHandleGetRequest_AllStations() throws Exception {
        // Add mock weather data for two stations
        String stationData1 = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";
        String stationData2 = "{\"id\":\"IDS60902\", \"temperature\":\"22.0\", \"humidity\":\"55\"}";
        aggregationServer.processPutRequest(stationData1, mockOut);
        aggregationServer.processPutRequest(stationData2, mockOut);

        // Call the GET handler for all stations (null station ID)
        aggregationServer.handleGetRequest(mockOut, null);

        // Verify that the response contains data for both stations
        verify(mockOut, times(1)).println(contains("25.0"));
        verify(mockOut, times(1)).println(contains("22.0"));
        verify(mockOut, times(1)).println(contains("60"));
        verify(mockOut, times(1)).println(contains("55"));
    }

    // Test removing expired weather data entries
    @Test
    public void testRemoveExpiredEntries() throws Exception {
        // Add mock weather data
        String stationData1 = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";
        aggregationServer.processPutRequest(stationData1, mockOut);

        // Fast-forward time by 31 seconds (simulate expiration)
        Thread.sleep(31000);

        // Run the method that removes expired entries
        aggregationServer.removeExpiredEntries();

        // Verify that the data has been removed
        assertNull(AggregationServer.weatherData.get("IDS60901"));
    }

    // Test maintaining a maximum of 20 entries
    @Test
    public void testMaxEntriesLimit() throws Exception {
        // Add 21 mock entries
        for (int i = 0; i < 21; i++) {
            String stationID = "IDS609" + (i + 1);
            String stationData = "{\"id\":\"" + stationID + "\", \"temperature\":\"" + (20 + i) + "\", \"humidity\":\"" + (50 + i) + "\"}";
            aggregationServer.processPutRequest(stationData, mockOut);
        }

        // Verify that only 20 entries remain, and the oldest entry was removed
        assertEquals(20, AggregationServer.weatherData.size());
        assertNull(AggregationServer.weatherData.get("IDS60901"));  // The oldest entry should have been removed
        assertNotNull(AggregationServer.weatherData.get("IDS60921"));  // The newest entry should still exist
    }

    // Test Lamport clock ticks after PUT requests
    @Test
    public void testLamportClockTicksAfterPutRequest() throws Exception {
        int initialTime = AggregationServer.lamportClock.getTime();

        // Add mock weather data
        String stationData1 = "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}";
        aggregationServer.processPutRequest(stationData1, mockOut);

        // Verify that the Lamport clock has ticked
        assertEquals(initialTime + 1, AggregationServer.lamportClock.getTime());
    }
}
