package com.aggregationserver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GETClientTest {

    private GETClient client;

    @BeforeEach
    public void setup() {
        client = new GETClient();
    }

    @Test
    public void testGetWeatherDataForSpecificStation() throws Exception {
        // Mock a socket to simulate the server response
        Socket mockSocket = mock(Socket.class);
        InputStream mockInputStream = new ByteArrayInputStream(
                ("HTTP/1.1 200 OK\nContent-Type: application/json\n\n" +
                        "{\"id\":\"IDS60901\", \"temperature\":\"25.0\", \"humidity\":\"60\"}").getBytes());

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(System.out);

        // Simulate the client sending a GET request
        String result = client.getWeatherData("localhost:4567", "IDS60901");

        // Verify that the response contains expected data
        assertTrue(result.contains("\"id\":\"IDS60901\""));
        assertTrue(result.contains("\"temperature\":\"25.0\""));
        assertTrue(result.contains("\"humidity\":\"60\""));
    }

    @Test
    public void testGetAllStationsWeatherData() throws Exception {
        // Mock a socket to simulate the server response for all stations
        Socket mockSocket = mock(Socket.class);
        InputStream mockInputStream = new ByteArrayInputStream(
                ("HTTP/1.1 200 OK\nContent-Type: application/json\n\n" +
                        "{\"IDS60901\": {\"temperature\":\"25.0\", \"humidity\":\"60\"}, " +
                        "\"IDS60902\": {\"temperature\":\"20.0\", \"humidity\":\"50\"}}").getBytes());

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(System.out);

        // Simulate the client sending a GET request
        String result = client.getWeatherData("localhost:4567", null);

        // Verify that the response contains data from both stations
        assertTrue(result.contains("\"IDS60901\""));
        assertTrue(result.contains("\"IDS60902\""));
    }
}
