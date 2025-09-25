package com.example.camunda;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Main application demonstrating Camunda 8.8 Java Client operations:
 * - Deploy process definition
 * - Start process instance
 * - Query user tasks
 * - Assign and complete user task
 * - Activate and complete service task job
 */
public class CamundaProcessApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(CamundaProcessApplication.class);
    private static final String PROCESS_ID = "sample-process";
    private static final String USER_TASK_ID = "user-task";
    private static final String SERVICE_TASK_TYPE = "processData";
    private static final String ASSIGNEE = "demo";
    
    private final CamundaClient client;
    
    public CamundaProcessApplication(CamundaClient client) {
        this.client = client;
    }
    
    public static void main(String[] args) {
        // Create Camunda client configured to use REST API at localhost:8080
        try (CamundaClient client = CamundaClient.newClientBuilder()
                .restAddress(URI.create("http://localhost:8080"))
                .build()) {
            CamundaProcessApplication app = new CamundaProcessApplication(client);
            app.runProcessDemo();
        } catch (Exception e) {
            logger.error("Error running process demo", e);
        }
    }
    
    /**
     * Runs the complete process demonstration
     */
    public void runProcessDemo() {
        try {
            logger.info("Starting Camunda Process Demo");
            
            // 1. Deploy process definition
            deployProcessDefinition();
            
            // 2. Start process instance
            ProcessInstanceEvent processInstance = startProcessInstance();
            long processInstanceKey = processInstance.getProcessInstanceKey();
            
            // Wait 10 seconds before querying user tasks to ensure process has progressed
            logger.info("Waiting 10 seconds for process to reach user task...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Wait interrupted", e);
            }
            
            // 3. Query user tasks for the started instance
            Optional<UserTask> userTask = queryUserTask(processInstanceKey);
            
            if (userTask.isPresent()) {
                // 4. Assign and complete the user task
                assignAndCompleteUserTask(userTask.get());
            } else {
                logger.warn("No user task found for process instance: {}", processInstanceKey);
            }
            
            // 5. Activate and complete service task job
            activateAndCompleteServiceTask();
            
            logger.info("Process demo completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during process demo execution", e);
            throw new RuntimeException("Process demo failed", e);
        }
    }
    
    /**
     * Deploy the process definition from BPMN file
     */
    public void deployProcessDefinition() {
        try {
            logger.info("Deploying process definition...");
            
            InputStream processStream = getClass().getClassLoader()
                .getResourceAsStream("sample-process.bpmn");
                
            if (processStream == null) {
                throw new RuntimeException("Could not find sample-process.bpmn in resources");
            }
            
            var deployment = client.newDeployResourceCommand()
                .addResourceStream(processStream, "sample-process.bpmn")
                .send()
                .join();
                
            logger.info("Process deployed successfully with key: {}", deployment.getKey());
            
        } catch (Exception e) {
            logger.error("Failed to deploy process definition", e);
            throw e;
        }
    }
    
    /**
     * Start a new process instance
     */
    public ProcessInstanceEvent startProcessInstance() {
        try {
            logger.info("Starting process instance...");
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", "12345");
            variables.put("customerName", "John Doe");
            
            ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();
                
            logger.info("Process instance started with key: {}", processInstance.getProcessInstanceKey());
            return processInstance;
            
        } catch (Exception e) {
            logger.error("Failed to start process instance", e);
            throw e;
        }
    }
    
    /**
     * Query user tasks for a specific process instance
     */
    public Optional<UserTask> queryUserTask(long processInstanceKey) {
        try {
            logger.info("Querying user tasks for process instance: {}", processInstanceKey);
            
            var searchRequest = client.newUserTaskSearchRequest()
                .filter(f -> f.processInstanceKey(processInstanceKey))
                .send()
                .join();
                
            if (!searchRequest.items().isEmpty()) {
                UserTask task = searchRequest.items().get(0);
                logger.info("Found user task with key: {}", task.getUserTaskKey());
                return Optional.of(task);
            } else {
                logger.warn("No user tasks found for process instance: {}", processInstanceKey);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Failed to query user tasks", e);
            throw e;
        }
    }
    
    /**
     * Assign and complete a user task
     */
    public void assignAndCompleteUserTask(UserTask task) {
        try {
            long taskKey = task.getUserTaskKey();
            logger.info("Assigning and completing user task with key: {}", taskKey);
            
            // Assign the task
            client.newAssignUserTaskCommand(taskKey)
                .assignee(ASSIGNEE)
                .send()
                .join();
            logger.info("Task assigned to: {}", ASSIGNEE);
            
            // Complete the task with variables
            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("approved", true);
            taskVariables.put("comments", "Task completed successfully");
            
            client.newCompleteUserTaskCommand(taskKey)
                .variables(taskVariables)
                .send()
                .join();
            logger.info("User task completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to assign and complete user task", e);
            throw e;
        }
    }
    
    /**
     * Activate and complete a service task job
     */
    public void activateAndCompleteServiceTask() {
        try {
            logger.info("Activating and completing service task job...");
            
            /*
             * CountDownLatch Explanation:
             * A CountDownLatch is a synchronization primitive that allows one or more threads
             * to wait until a set of operations being performed in other threads completes.
             * 
             * In this case:
             * - We initialize the latch with count=1
             * - The job worker handler runs asynchronously when a job is received
             * - When the job is completed (successfully or with failure), we call latch.countDown()
             * - The main thread waits using latch.await() until the count reaches 0
             * - This ensures the main thread doesn't continue until the service task job is processed
             * 
             * This pattern is necessary because job workers operate asynchronously, and we need
             * to coordinate between the async job processing and the main application flow.
             */
            CountDownLatch latch = new CountDownLatch(1);
            
            // Create job worker to handle service task
            var jobWorker = client.newWorker()
                .jobType(SERVICE_TASK_TYPE)
                .handler((jobClient, job) -> {
                    try {
                        logger.info("Processing job with key: {}", job.getKey());
                        
                        // Process the job
                        Map<String, Object> jobVariables = new HashMap<>(job.getVariablesAsMap());
                        jobVariables.put("processed", true);
                        jobVariables.put("processedAt", System.currentTimeMillis());
                        
                        // Complete the job
                        jobClient.newCompleteCommand(job.getKey())
                            .variables(jobVariables)
                            .send()
                            .join();
                        logger.info("Service task job completed successfully");
                        
                        latch.countDown();
                    } catch (Exception e) {
                        logger.error("Failed to complete job", e);
                        jobClient.newFailCommand(job.getKey())
                            .retries(0)
                            .errorMessage("Job processing failed: " + e.getMessage())
                            .send();
                        latch.countDown();
                    }
                })
                .open();
            
            // Wait for job to be processed (with timeout)
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            
            if (!completed) {
                logger.warn("Service task job was not completed within timeout");
            }
            
            // Close the job worker
            jobWorker.close();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Service task job processing was interrupted", e);
            throw new RuntimeException("Service task processing interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to activate and complete service task", e);
            throw e;
        }
    }
}