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

# Run the main application (requires running Camunda broker)
mvn exec:java -Dexec.mainClass="com.example.camunda.CamundaProcessApplication"
```

## Notes

- The application expects a Camunda 8 broker to be running on localhost:26500 (default)
- Tests use the Camunda Process Test framework which provides an embedded test environment
- All operations use the Orchestration cluster API through the Camunda Java Client
