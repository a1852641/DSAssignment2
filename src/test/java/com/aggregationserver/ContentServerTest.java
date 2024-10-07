package com.aggregationserver;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ContentServerTest {

    public ContentServer contentServer;
    public JSONParser jsonParser;

    @BeforeEach
    public void setup() {
        contentServer = new ContentServer();
        jsonParser = new JSONParser();
    }

    // Test for reading the weather data file using the example file
    @Test
    public void testReadFileToJSON_UsingExampleFile() throws Exception {
        // Path to the sample file
        String filePath = "src/test/resources/sample_weather_text.txt";

        // Parse the weather data from the file
        Map<String, Object> weatherData = ContentServer.readFileToJSON(filePath);

        // Assertions to verify the correct data has been parsed
        assertEquals("IDS60901", weatherData.get("id"));
        assertEquals("Adelaide (West Terrace / ngayirdapira)", weatherData.get("name"));
        assertEquals("SA", weatherData.get("state"));
        assertEquals("13.3", weatherData.get("air_temp")); // Temperature as string
        assertEquals("60", weatherData.get("rel_hum")); // Humidity as string
        assertTrue(weatherData.containsKey("lamportTime")); // Ensure Lamport time is present
    }

    // Mock test for sending weather data using sendPutRequest method
    @Test
    public void testSendWeatherData_UsingSendPutRequest() throws Exception {
        // Mocking PrintWriter to capture the output
        PrintWriter mockOut = mock(PrintWriter.class);

        // Sample weather data (manually constructed to match sample_weather_text.txt)
        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("id", "IDS60901");
        weatherData.put("name", "Adelaide (West Terrace / ngayirdapira)");
        weatherData.put("state", "SA");
        weatherData.put("air_temp", "13.3");
        weatherData.put("rel_hum", "60");
        weatherData.put("lamportTime", 1);  // Simulate Lamport clock value

        // Invoke the method to send the PUT request
        ContentServer.sendPutRequest(mockOut, weatherData);

        // Verify the correct HTTP request format was written
        verify(mockOut).println("PUT /weather.json HTTP/1.1");
        verify(mockOut).println("User-Agent: ContentServer/1.0");
        verify(mockOut).println("Content-Type: application/json");

        // Verify the weather data content was sent
        verify(mockOut).println(anyString());  // The actual JSON string

        // Verify the PUT request ended with a blank line after the headers
        verify(mockOut, times(1)).println(); // Ensures there was an empty line between headers and body
    }

}
