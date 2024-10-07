package com.aggregationserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple JSON parser capable of parsing JSON strings into Java objects (Map,
 * List, String, Number, etc.).
 */
public class JSONParser {

    // Current index in the JSON string being parsed
    private int index;

    // The JSON string being parsed
    private String json;

    /**
     * Parses a JSON string and returns the corresponding Java object (Map,
     * List, String, Number, etc.).
     *
     * @param jsonString The JSON string to parse.
     * @return The parsed Java object representation of the JSON.
     * @throws Exception If an error occurs during parsing.
     */
    public Object parse(String jsonString) throws Exception {
        this.json = jsonString.trim();  // Remove leading/trailing whitespace
        this.index = 0;
        return parseValue();  // Start parsing from the root value
    }

    /**
     * Parses the next value in the JSON string, which can be an object, array,
     * string, number, boolean, or null.
     *
     * @return The parsed value.
     * @throws Exception If an error occurs during parsing.
     */
    private Object parseValue() throws Exception {
        skipWhitespace();  // Skip any whitespace before parsing the value
        switch (json.charAt(index)) {
            case '{':
                return parseObject();  // JSON object
            case '[':
                return parseArray();   // JSON array
            case '"':
                return parseString();  // JSON string
            case 't':
            case 'f':
                return parseBoolean(); // JSON boolean
            case 'n':
                return parseNull();    // JSON null
            default:
                return parseNumber();  // JSON number
        }
    }

    /**
     * Parses a JSON object and returns it as a Map<String, Object>.
     *
     * @return The parsed object as a Map.
     * @throws Exception If an error occurs during parsing.
     */
    private Map<String, Object> parseObject() throws Exception {
        Map<String, Object> obj = new HashMap<>();
        index++;  // Skip '{'
        skipWhitespace();

        while (json.charAt(index) != '}') {  // Continue until '}' is found
            String key = parseString();  // Parse key
            skipWhitespace();
            index++;  // Skip ':'
            skipWhitespace();
            Object value = parseValue();  // Parse value
            obj.put(key, value);  // Add key-value pair to the object
            skipWhitespace();
            if (json.charAt(index) == ',') {
                index++;  // Skip ',' between key-value pairs
            }
            skipWhitespace();
        }
        index++;  // Skip '}'
        return obj;
    }

    /**
     * Parses a JSON array and returns it as a List<Object>.
     *
     * @return The parsed array as a List.
     * @throws Exception If an error occurs during parsing.
     */
    private List<Object> parseArray() throws Exception {
        List<Object> array = new ArrayList<>();
        index++;  // Skip '['
        skipWhitespace();

        while (json.charAt(index) != ']') {  // Continue until ']' is found
            array.add(parseValue());  // Parse each value in the array
            skipWhitespace();
            if (json.charAt(index) == ',') {
                index++;  // Skip ',' between array values
            }
            skipWhitespace();
        }
        index++;  // Skip ']'
        return array;
    }

    /**
     * Parses a JSON string and returns it.
     *
     * @return The parsed string.
     * @throws Exception If an error occurs during parsing.
     */
    private String parseString() throws Exception {
        StringBuilder sb = new StringBuilder();
        index++;  // Skip '"'

        while (json.charAt(index) != '"') {  // Continue until the closing '"' is found
            sb.append(json.charAt(index));
            index++;
        }
        index++;  // Skip closing '"'
        return sb.toString();
    }

    /**
     * Parses a JSON number and returns it as a Number (int, long, or double).
     *
     * @return The parsed number.
     * @throws Exception If an error occurs during parsing.
     */
    private Number parseNumber() throws Exception {
        StringBuilder sb = new StringBuilder();

        // Collect all digits, sign, and decimal point
        while (Character.isDigit(json.charAt(index)) || json.charAt(index) == '.' || json.charAt(index) == '-') {
            sb.append(json.charAt(index));
            index++;
        }

        String numberString = sb.toString();

        try {
            if (numberString.contains(".")) {
                return Double.parseDouble(numberString);  // Return as double if it has a decimal point
            } else {
                // Return as long if it's a large number, otherwise int
                long longValue = Long.parseLong(numberString);
                if (longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
                    return (int) longValue;  // Use int for smaller numbers
                } else {
                    return longValue;  // Use long for larger numbers
                }
            }
        } catch (NumberFormatException e) {
            throw new Exception("Invalid number format: " + numberString);
        }
    }

    /**
     * Parses a JSON boolean and returns it.
     *
     * @return The parsed boolean.
     * @throws Exception If an error occurs during parsing.
     */
    private Boolean parseBoolean() throws Exception {
        if (json.startsWith("true", index)) {
            index += 4;  // Skip "true"
            return true;
        } else if (json.startsWith("false", index)) {
            index += 5;  // Skip "false"
            return false;
        }
        throw new Exception("Invalid JSON boolean");
    }

    /**
     * Parses the JSON 'null' value.
     *
     * @return The parsed null value (returns null).
     * @throws Exception If an error occurs during parsing.
     */
    private Object parseNull() throws Exception {
        if (json.startsWith("null", index)) {
            index += 4;  // Skip "null"
            return null;
        }
        throw new Exception("Invalid JSON null");
    }

    /**
     * Skips any whitespace in the JSON string.
     */
    private void skipWhitespace() {
        while (Character.isWhitespace(json.charAt(index))) {
            index++;
        }
    }

    /**
     * Main method for testing the JSONParser functionality.
     */
    public static void main(String[] args) throws Exception {
        JSONParser parser = new JSONParser();

        String jsonString = "{\"name\":\"John\",\"age\":30,\"isStudent\":false,\"address\":{\"city\":\"New York\"},\"subjects\":[\"Math\",\"Science\"],\"score\":null}";

        Object result = parser.parse(jsonString);
        System.out.println(result);  // This will print the parsed JSON as a Map or List in Java
    }

    /**
     * Converts a Java object (Map, List, String, Number, etc.) back into a JSON
     * string. This version of stringify returns a compact (no extra whitespace)
     * JSON string.
     *
     * @param obj The Java object to convert to JSON.
     * @return The JSON string representation of the object.
     */
    public String stringify(Object obj) {
        return stringify(obj, 0);  // Default indentation level is 0 (compact JSON)
    }

    /**
     * Converts a Java object (Map, List, String, Number, etc.) into a
     * pretty-printed JSON string with the specified level of indentation.
     *
     * @param obj The Java object to convert to JSON.
     * @param indentLevel The current indentation level for pretty-printing.
     * @return The JSON string representation of the object.
     */
    public String stringify(Object obj, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(indentLevel);  // Use 2 spaces for each level of indentation

        if (obj instanceof Map) {
            sb.append("{\n");
            Map<String, Object> map = (Map<String, Object>) obj;
            for (String key : map.keySet()) {
                sb.append(indent).append("  \"").append(key).append("\": ")
                        .append(stringify(map.get(key), indentLevel + 1)).append(",\n");
            }
            if (!map.isEmpty()) {
                sb.setLength(sb.length() - 2);  // Remove trailing comma and newline

            }
            sb.append("\n").append(indent).append("}");
        } else if (obj instanceof List) {
            sb.append("[\n");
            List<Object> list = (List<Object>) obj;
            for (Object value : list) {
                sb.append(indent).append("  ").append(stringify(value, indentLevel + 1)).append(",\n");
            }
            if (!list.isEmpty()) {
                sb.setLength(sb.length() - 2);  // Remove trailing comma and newline

            }
            sb.append("\n").append(indent).append("]");
        } else if (obj instanceof String) {
            sb.append("\"").append(obj.toString()).append("\"");
        } else {
            sb.append(obj.toString());  // For numbers, booleans, and null
        }

        return sb.toString();
    }

}
