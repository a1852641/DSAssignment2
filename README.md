
# Weather Data Aggregation System

This system is designed to aggregate and manage weather data from multiple content servers. It uses a **Lamport clock** to ensure synchronization across distributed systems. The system consists of three main components: `AggregationServer`, `ContentServer`, and `GETClient`.

## System Components

### 1. AggregationServer
- Central server that listens for weather data from multiple content servers.
- Aggregates weather data and makes it available for retrieval via `GET` requests.
- Maintains the latest weather data and periodically removes expired entries.
- Uses **Lamport clocks** to synchronize data changes between the servers.

### 2. ContentServer
- Reads weather data from a file and periodically sends it to the `AggregationServer` via `HTTP PUT` requests.
- Synchronizes updates using **Lamport clocks** for consistency.
- Multiple `ContentServers` can be run simultaneously, each sending its own set of data to the `AggregationServer`.

### 3. GETClient
- Retrieves aggregated weather data from the `AggregationServer` via `HTTP GET` requests.
- Can fetch data for a specific weather station or for all stations.

---

## How the System Works

### Example Flow:
1. **Start the AggregationServer**  
   The server starts listening for incoming data from content servers.
   
2. **Run multiple ContentServers**  
   Each `ContentServer` sends weather data to the `AggregationServer` every 20 seconds.
   
3. **Retrieve data using GETClient**  
   The `GETClient` can query the `AggregationServer` to get either all weather data or data for a specific station.

---

## Prerequisites

- **Maven** must be installed on your system.
- **Java Development Kit (JDK) 11** or above is required.

---

## Installation and Setup

### Install dependencies:
```bash
mvn install
```

### Clean previous runs and compile the code:
```bash
mvn clean compile
```

---

## Running the System

### 1. Start the AggregationServer
The `AggregationServer` listens on a specified port (default: 4567).
```bash
mvn exec:java "-Dexec.mainClass=com.aggregationserver.AggregationServer"
```

### 2. Start the ContentServer
The `ContentServer` sends weather data to the `AggregationServer` every 20 seconds.  
To start a `ContentServer`:
```bash
mvn exec:java "-Dexec.mainClass=com.aggregationserver.ContentServer" "-Dexec.args=localhost:4567 src/test/resources/"
```
For example, to send data from `sample_weather_data.txt`:
```bash
mvn exec:java "-Dexec.mainClass=com.aggregationserver.ContentServer" "-Dexec.args=localhost:4567 src/test/resources/sample_weather_data.txt"
```

To run multiple `ContentServers`, open several terminal windows and run the above command in each terminal, pointing to different data files if needed.

### 3. Run the GETClient
To retrieve data from the `AggregationServer`, use the following commands:

- Get data for all existing stations:
```bash
mvn exec:java "-Dexec.mainClass=com.aggregationserver.GETClient" "-Dexec.args=localhost:4567"
```

- Get data for a specific station (e.g., station with ID `IDS60901`):
```bash
mvn exec:java "-Dexec.mainClass=com.aggregationserver.GETClient" "-Dexec.args=localhost:4567 IDS60901"
```

---

## Running Tests

To run the tests for the system, execute the following command:
```bash
mvn test
```
This will run all unit tests to ensure that the system components are working as expected.

---

## System Components Overview

- **AggregationServer**: Aggregates weather data from multiple `ContentServers`, removes stale data after 30 seconds, and synchronizes updates using **Lamport clocks**.
- **ContentServer**: Reads weather data from a file and sends it to the `AggregationServer` every 20 seconds.
- **GETClient**: Retrieves aggregated weather data, either for all stations or for a specific weather station.

---

### Example Usage Flow
1. Start the `AggregationServer` to listen for incoming data.
2. Start several `ContentServers`, each sending weather data from different files to the `AggregationServer`.
3. Use `GETClient` to query the `AggregationServer` and retrieve the aggregated weather data.
