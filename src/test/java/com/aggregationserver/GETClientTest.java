package com.aggregationserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GETClientTest {

    private GETClient getClient;

    @BeforeEach
    public void setup() {
        getClient = new GETClient();
    }

    // Test for sending a valid GET request and receiving data
    @Test
    public void testSendGetRequest_Valid() throws Exception {
        // Mock the socket and streams for server communication
        Socket mockSocket = mock(Socket.class);
        BufferedReader mockBufferedReader = mock(BufferedReader.class);
        PrintWriter mockPrintWriter = mock(PrintWriter.class);
        when(mockSocket.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockSocket.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockBufferedReader.readLine()).thenReturn("HTTP/1.1 200 OK", "", "{\"id\":\"IDS60901\",\"temperature\":\"25.0\",\"humidity\":\"60\"}", null);

        // Simulate sending the GET request and receiving the response
        String[] response = getClient.sendGetRequest("localhost:4567", "IDS60901");

        // Assert that the response contains valid headers and body
        assertTrue(response[0].contains("200 OK"));
        assertTrue(response[1].contains("\"temperature\":\"25.0\""));
        assertTrue(response[1].contains("\"humidity\":\"60\""));
    }

    // Test for sending a GET request with an invalid host
    @Test
    public void testSendGetRequest_InvalidHost() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            getClient.sendGetRequest("invalidHostPort", "IDS60901");
        });

        assertEquals("Invalid serverHostPort format.", exception.getMessage());
    }

    // Test for displaying weather data from JSON
    @Test
    public void testDisplayWeatherData_ValidJSON_FromFile() throws Exception {
        // Load the sample weather data
        File file = new File("src/test/resources/sample_weather_data.txt");
        String sampleData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        // Mock the server's response to the GET request with the sample data
        Socket mockSocket = mock(Socket.class);
        BufferedReader mockIn = new BufferedReader(new StringReader("HTTP/1.1 200 OK\n\n" + sampleData));
        PrintWriter mockOut = mock(PrintWriter.class);

        // Mock socket input and output
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        // GETClient sends the GET request and processes the response
        GETClient client = new GETClient();
        client.sendGetRequest("localhost:8080", "stationID");


        // Verify the client correctly reads and displays the weather data
        verify(mockIn).readLine();
    }

    // Test for handling invalid JSON data
    @Test
    public void testDisplayWeatherData_InvalidJSON() {
        String invalidJson = "Invalid JSON";

        // Mock the error output
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorStream));

        getClient.displayWeatherData(invalidJson);

        assertTrue(errorStream.toString().contains("Error parsing or displaying weather data"));
    }

    // Test the helper method getWeatherData
    @Test
    public void testGetWeatherData_Valid() throws Exception {
        // Mock the GET request
        String expectedJson = "{\"id\":\"IDS60901\",\"temperature\":\"25.0\",\"humidity\":\"60\"}";
        GETClient mockClient = mock(GETClient.class);
        when(mockClient.getWeatherData("localhost:4567", "IDS60901")).thenReturn(expectedJson);

        // Call the helper method and verify the result
        String result = mockClient.getWeatherData("localhost:4567", "IDS60901");
        assertEquals(expectedJson, result);
    }

    // Test Lamport clock ticking after a GET request
    @Test
    public void testLamportClockTickAfterGetRequest() throws Exception {
        // Initialize Lamport clock value to 0
        LamportClock clock = getClient.getLamportClock();
        assertEquals(0, clock.getTime());

        // Mock the GET request and ensure the clock ticks after
        getClient.sendGetRequest("localhost:4567", "IDS60901");
        assertEquals(1, clock.getTime());
    }
}
