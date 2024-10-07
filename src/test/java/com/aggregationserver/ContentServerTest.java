package com.aggregationserver;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ContentServerTest {

    private ContentServer contentServer;

    @BeforeEach
    public void setup() {
        contentServer = new ContentServer();
    }

    @Test
    public void testReadFileToJSON() throws Exception {
        // Use the file path from your system for the test
        String filePath = "src/test/resources/sample_weather_data.txt";  // Path from your system

        // Call the method to read the data
        Map<String, Object> weatherData = contentServer.readFileToJSON(filePath);

        // Validate the contents of the weather data
        assertEquals("IDS60901", weatherData.get("id"), "Station ID should match");
        assertEquals("25.0", weatherData.get("temperature"), "Temperature should match");
        assertEquals("60", weatherData.get("humidity"), "Humidity should match");
    }

    @Test
    public void testSendPutRequest() throws Exception {
        // Mock a PrintWriter to simulate data transmission
        PrintWriter mockOut = mock(PrintWriter.class);

        // Sample weather data
        Map<String, Object> weatherData = Map.of(
            "id", "IDS60901",
            "temperature", "25.0",
            "humidity", "60"
        );

        // Send the PUT request
        contentServer.sendPutRequest(mockOut, weatherData);

        // Verify that the request is sent
        verify(mockOut, times(1)).println(anyString());
        verify(mockOut, times(1)).flush();
    }

    @Test
    public void testConnectionAndDataSending() throws Exception {
        // Mock the connection
        Socket mockSocket = mock(Socket.class);
        PrintWriter mockOut = mock(PrintWriter.class);
        
        when(mockSocket.getOutputStream()).thenReturn(System.out);

        // Start the content server and simulate sending data
        contentServer.maintainConnectionAndSendData("localhost:4567", "src/test/resources/sample_weather_data.txt");
        
        // Verify that data was sent
        verify(mockOut, times(1)).println(anyString());
    }
}
