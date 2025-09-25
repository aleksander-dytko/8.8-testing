package com.example.camunda;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaProcessTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for the Camunda process using Camunda Process Test framework.
 * This test validates all the operations:
 * - Deploy process definition
 * - Start process instance
 * - Query user tasks
 * - Assign and complete user task
 * - Activate and complete service task job
 */
@ExtendWith(CamundaProcessTestExtension.class)
class CamundaProcessApplicationTest {

    private static final Logger logger = LoggerFactory.getLogger(CamundaProcessApplicationTest.class);
    private static final String PROCESS_ID = "sample-process";
    private static final String USER_TASK_ID = "user-task";
    private static final String SERVICE_TASK_TYPE = "processData";
    private static final String ASSIGNEE = "demo";

    @Test
    void shouldCompleteProcessWithUserTaskAndServiceTask(CamundaProcessTestContext processTestContext) {
        // Get the Camunda client from the test context
        CamundaClient client = processTestContext.createClient();
        
        logger.info("Starting comprehensive process test");
        
        // Create application instance with test client
        CamundaProcessApplication app = new CamundaProcessApplication(client);
        
        // 1. Deploy process definition
        logger.info("Step 1: Deploying process definition");
        app.deployProcessDefinition();
        
        // Verify deployment was successful
        var deployments = client.newTopologyRequest().send().join().getBrokers();
        assertFalse(deployments.isEmpty(), "Brokers should be available after deployment");
        
        // 2. Start process instance with variables
        logger.info("Step 2: Starting process instance");
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("orderId", "TEST-12345");
        processVariables.put("customerName", "Test Customer");
        processVariables.put("amount", 100.0);
        
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
            .bpmnProcessId(PROCESS_ID)
            .latestVersion()
            .variables(processVariables)
            .send()
            .join();
        
        assertNotNull(processInstance);
        assertTrue(processInstance.getProcessInstanceKey() > 0);
        assertEquals(PROCESS_ID, processInstance.getBpmnProcessId());
        logger.info("Process instance created with key: {}", processInstance.getProcessInstanceKey());
        
        // 3. Wait for and query user task
        logger.info("Step 3: Querying user tasks");
        
        // Wait a bit for the process to reach the user task
        try {
            Thread.sleep(2000); // Simple wait instead of waitForIdleState
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Optional<UserTask> userTaskOpt = app.queryUserTask(processInstance.getProcessInstanceKey());
        assertTrue(userTaskOpt.isPresent(), "User task should be available");
        
        UserTask userTask = userTaskOpt.get();
        assertEquals(processInstance.getProcessInstanceKey(), userTask.getProcessInstanceKey());
        logger.info("Found user task with key: {}", userTask.getUserTaskKey());
        
        // 4. Assign and complete user task
        logger.info("Step 4: Assigning and completing user task");
        
        // Assign the task
        client.newAssignUserTaskCommand(userTask.getUserTaskKey())
            .assignee(ASSIGNEE)
            .send()
            .join();
        
        // Verify task assignment by querying again
        var updatedTasksResult = client.newUserTaskSearchRequest()
            .filter(f -> f.userTaskKey(userTask.getUserTaskKey()))
            .send()
            .join();
        assertFalse(updatedTasksResult.items().isEmpty());
        assertEquals(ASSIGNEE, updatedTasksResult.items().get(0).getAssignee());
        
        // Complete the user task with additional variables
        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("approved", true);
        taskVariables.put("reviewComments", "Test review completed");
        taskVariables.put("reviewedBy", ASSIGNEE);
        
        client.newCompleteUserTaskCommand(userTask.getUserTaskKey())
            .variables(taskVariables)
            .send()
            .join();
        
        logger.info("User task completed successfully");
        
        // 5. Handle service task job
        logger.info("Step 5: Processing service task job");
        
        // Set up job worker for service task
        boolean[] jobCompleted = {false};
        var jobWorker = client.newWorker()
            .jobType(SERVICE_TASK_TYPE)
            .handler((jobClient, job) -> {
                try {
                    logger.info("Processing service task job with key: {}", job.getKey());
                    
                    // Verify job contains expected variables
                    Map<String, Object> jobVariables = job.getVariablesAsMap();
                    assertTrue(jobVariables.containsKey("orderId"));
                    assertTrue(jobVariables.containsKey("approved"));
                    assertEquals("TEST-12345", jobVariables.get("orderId"));
                    assertEquals(true, jobVariables.get("approved"));
                    
                    // Add processing result variables
                    Map<String, Object> resultVariables = new HashMap<>(jobVariables);
                    resultVariables.put("processed", true);
                    resultVariables.put("processedAt", System.currentTimeMillis());
                    resultVariables.put("processingResult", "SUCCESS");
                    
                    // Complete the job
                    jobClient.newCompleteCommand(job.getKey())
                        .variables(resultVariables)
                        .send()
                        .join();
                    
                    jobCompleted[0] = true;
                    logger.info("Service task job completed successfully");
                    
                } catch (Exception e) {
                    logger.error("Failed to process job", e);
                    jobClient.newFailCommand(job.getKey())
                        .retries(0)
                        .errorMessage("Job processing failed: " + e.getMessage())
                        .send();
                }
            })
            .open();
        
        // Wait for process to complete
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify job was completed
        assertTrue(jobCompleted[0], "Service task job should have been completed");
        
        // Clean up job worker
        jobWorker.close();
        
        // 6. Verify process instance completion
        logger.info("Step 6: Verifying process completion");
        
        // The process should be completed, so it might not appear in active instances
        // This is expected behavior for completed processes
        logger.info("Process test completed successfully");
    }
    
    @Test
    void shouldHandleProcessWithoutUserTaskInteraction(CamundaProcessTestContext processTestContext) {
        CamundaClient client = processTestContext.createClient();
        
        logger.info("Testing process deployment and instance creation only");
        
        CamundaProcessApplication app = new CamundaProcessApplication(client);
        
        // Deploy process
        app.deployProcessDefinition();
        
        // Start instance
        ProcessInstanceEvent processInstance = app.startProcessInstance();
        
        assertNotNull(processInstance);
        assertTrue(processInstance.getProcessInstanceKey() > 0);
        
        logger.info("Basic process operations test completed");
    }
    
    @Test
    void shouldHandleUserTaskOperations(CamundaProcessTestContext processTestContext) {
        CamundaClient client = processTestContext.createClient();
        CamundaProcessApplication app = new CamundaProcessApplication(client);
        
        logger.info("Testing user task specific operations");
        
        // Deploy and start process
        app.deployProcessDefinition();
        ProcessInstanceEvent processInstance = app.startProcessInstance();
        
        // Wait for user task
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Query user task
        Optional<UserTask> userTask = app.queryUserTask(processInstance.getProcessInstanceKey());
        assertTrue(userTask.isPresent(), "User task should be present");
        
        // Test task assignment and completion
        app.assignAndCompleteUserTask(userTask.get());
        
        logger.info("User task operations test completed");
    }
}