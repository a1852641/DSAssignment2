package com.aggregationserver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.List;

public class JSONParserTest {

    @Test
    public void testParseSimpleObject() throws Exception {
        JSONParser parser = new JSONParser();
        String jsonString = "{\"name\":\"John\",\"age\":30}";
        
        Map<String, Object> result = (Map<String, Object>) parser.parse(jsonString);
        
        assertEquals("John", result.get("name"));
        assertEquals(30, result.get("age"));
    }

    @Test
    public void testParseArray() throws Exception {
        JSONParser parser = new JSONParser();
        String jsonString = "[\"Apple\", \"Banana\", \"Cherry\"]";
        
        List<Object> result = (List<Object>) parser.parse(jsonString);
        
        assertEquals("Apple", result.get(0));
        assertEquals("Banana", result.get(1));
        assertEquals("Cherry", result.get(2));
    }

    @Test
    public void testStringifyObject() {
        JSONParser parser = new JSONParser();
        Map<String, Object> object = Map.of("name", "Alice", "age", 25);
        
        String jsonString = parser.stringify(object);
        assertEquals("{\"name\":\"Alice\",\"age\":25}", jsonString);
    }
    
    @Test
    public void testStringifyArray() {
        JSONParser parser = new JSONParser();
        List<String> array = List.of("Red", "Green", "Blue");
        
        String jsonString = parser.stringify(array);
        assertEquals("[\"Red\",\"Green\",\"Blue\"]", jsonString);
    }
}
