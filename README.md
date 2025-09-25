# 8.8-testing
A demo Java project to test Java Client and Camunda Process Test

## Overview

This project demonstrates the use of Camunda 8.8-alpha8 Java Client and Camunda Process Test framework to:

1. Deploy a process definition
2. Start a process instance
3. Query user tasks for the started instance
4. Assign and complete the user task
5. Activate and complete a job for a service task

## Project Structure

- `src/main/java/com/example/camunda/CamundaProcessApplication.java` - Main application demonstrating all operations
- `src/main/resources/sample-process.bpmn` - Sample BPMN process with user task and service task
- `src/test/java/com/example/camunda/CamundaProcessApplicationTest.java` - Comprehensive tests using Camunda Process Test

## BPMN Process

The sample process includes:
- Start event
- User task (assigned to "demo" user)
- Service task (type: "processData")
- End event

## Technologies Used

- Java 17
- Maven 3.9.x
- Camunda Java Client 8.8.0-alpha8
- Camunda Process Test 8.8.0-alpha8
- JUnit 5
- Logback for logging

## Key Features Demonstrated

### Using Camunda Java Client:
- Deploying BPMN process definitions
- Starting process instances with variables
- Querying user tasks using search API
- Assigning and completing user tasks
- Creating job workers for service tasks
- Handling job activation and completion

### Using Camunda Process Test:
- Integration testing of complete process flows
- Mocking and testing individual process components
- Verifying process behavior in isolated test environment

## Running the Application

```bash
# Compile the project
mvn clean compile

# Run tests (requires Camunda Process Test environment)
mvn test

# Run the main application (requires running Camunda broker with REST API at http://localhost:8080)
mvn exec:java -Dexec.mainClass="com.example.camunda.CamundaProcessApplication"
```

## Configuration

- **REST API**: The application is configured to connect to Camunda REST API at `http://localhost:8080`
- **Wait Time**: The application waits 10 seconds before querying user tasks to ensure process progression
- **Logging**: Configured with INFO level for application flow, WARN level for networking libraries

## Technical Implementation Details

### CountDownLatch Usage
The application uses `CountDownLatch` for synchronization in service task job handling:
- Initialized with count=1 to wait for a single job completion
- Job worker handler runs asynchronously when a job is received  
- `countDown()` is called when job completes (successfully or with failure)
- Main thread waits using `await()` until count reaches 0
- Ensures main thread coordination with async job processing

### Logging Configuration
The logging levels are configured as follows:
- **INFO**: Used for application flow (`com.example.camunda`) and Camunda client operations (`io.camunda`)
- **WARN**: Used for networking libraries (`io.grpc`, `io.netty`) to reduce noise
- **Pattern**: Shows timestamp, thread name, log level, logger name, and message

## Notes

- The application connects to Camunda 8 REST API at http://localhost:8080
- Tests use the Camunda Process Test framework which provides an embedded test environment
- All operations use the Orchestration cluster API through the Camunda Java Client
- The application includes a 10-second wait before querying user tasks to ensure proper process flow
- Camunda test assertions are used in tests for better process verification
