This Aggregation System is designed to aggregate and manage weather data sent by multiple content servers. It uses a Lamport clock to ensure synchronization across distributed systems. The system includes three main components: the AggregationServer, the ContentServer, and the GETClient.

How the System Works
AggregationServer: This is the central server that listens for weather data from multiple ContentServers. It aggregates the data and makes it available for retrieval via GET requests. The server maintains a record of the latest weather data and periodically removes expired entries. The Lamport clock is used to synchronize data changes between the servers.

ContentServer: Each ContentServer reads weather data from a file and periodically sends it to the AggregationServer using HTTP PUT requests. It also synchronizes its actions using the Lamport clock, ensuring consistent updates across the system. Multiple ContentServers can be run simultaneously, each sending its own set of data to the AggregationServer.

GETClient: This client retrieves the aggregated weather data from the AggregationServer using HTTP GET requests. It can either retrieve data for a specific weather station or fetch all available data.

Example Flow:
Start the AggregationServer: The server starts listening for incoming data from content servers.
Run multiple ContentServers: Each ContentServer sends weather data to the AggregationServer every 20 seconds.
Use GETClient to retrieve data: The GETClient can query the AggregationServer to get either all weather data or data for a specific station.
Prerequisites
Ensure you have Maven installed on your system.
Java Development Kit (JDK) 11 or above is required.
Installation and Setup
Before running the system, you need to install all necessary dependencies and compile the code.

Install dependencies:
mvn install

Clean previous runs and compile the code:
mvn clean compile

Running the System
1. Start the AggregationServer
The AggregationServer listens on a specified port (default: 4567). Run the server using the following command:
mvn exec:java "-Dexec.mainClass=com.aggregationserver.AggregationServer"

2. Start the ContentServer
The ContentServer sends weather data from a file to the AggregationServer every 20 seconds. To start a ContentServer:
mvn exec:java "-Dexec.mainClass=com.aggregationserver.ContentServer" "-Dexec.args=localhost:4567 src/test/resources/<filename>"

For example, to send data from sample_weather_data.txt:
mvn exec:java "-Dexec.mainClass=com.aggregationserver.ContentServer" "-Dexec.args=localhost:4567 src/test/resources/sample_weather_data.txt"

To run multiple ContentServers, open several terminal windows and run the above command in each terminal, pointing to different data files if needed.

3. Run the GETClient
The GETClient retrieves weather data from the AggregationServer. You can retrieve data for all stations or for a specific station.

To get data for all existing stations:
mvn exec:java "-Dexec.mainClass=com.aggregationserver.GETClient" "-Dexec.args=localhost:4567"

To get data for a specific station (e.g., station with ID IDS60901):
mvn exec:java "-Dexec.mainClass=com.aggregationserver.GETClient" "-Dexec.args=localhost:4567 IDS60901"

4. Running Tests
To run the tests for the system, execute the following command:
mvn test

This will run all unit tests to ensure that the system components are working as expected.

System Components Overview

AggregationServer:
Aggregates weather data sent from multiple ContentServers.
Removes stale data after a configurable expiration time (default: 30 seconds).
Synchronizes updates using Lamport clocks to maintain consistency in a distributed setup.

ContentServer:
Reads weather data from a file and sends it to the AggregationServer.
Periodically updates the server with fresh data.
Multiple ContentServers can run concurrently, each sending its own data to the central AggregationServer.

GETClient:
Retrieves aggregated weather data from the AggregationServer.
Can request data for all stations or specific weather stations.


Example Usage Flow
Start the AggregationServer to listen for incoming data.
Start several ContentServers, each sending weather data from different files to the AggregationServer.
Use GETClient to query the AggregationServer and retrieve the aggregated weather data.
