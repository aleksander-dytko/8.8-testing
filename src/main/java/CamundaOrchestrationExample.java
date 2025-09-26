import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class CamundaOrchestrationExample {

    private static final Logger log = LoggerFactory.getLogger(CamundaOrchestrationExample.class);

    public static void main (String[] args){
    URI REST = URI.create("http://localhost:8080");

    try (CamundaClient client = CamundaClient.newClientBuilder()
            .restAddress(REST)
            .build()) {

        // Deploy a BPMN process
        DeploymentEvent deploymentEvent = client.newDeployResourceCommand()
                .addResourceFromClasspath("demoProcess.bpmn")
                .send()
                .join();
        log.info("Deployment successful: {}", deploymentEvent.getKey());

        // Start a process instance
        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId("demoProcess")
                .latestVersion()
                .send()
                .join();
        log.info("Process instance started: {}", instance.getProcessInstanceKey());
        long processInstanceKey = instance.getProcessInstanceKey();

        Thread.sleep(5000); // Wait for the task to be created

        // Query user tasks for this process instance
        SearchResponse<UserTask> taskSearchQueryResult = client.newUserTaskSearchRequest()
                .filter(userTaskFilter -> userTaskFilter.processInstanceKey(processInstanceKey))
                .send()
                .join();


        if (!taskSearchQueryResult.items().isEmpty()){
            UserTask task = taskSearchQueryResult.items().getFirst();
            long userTaskKey = task.getUserTaskKey();
            log.info("User task found: {}", userTaskKey);

            // Assign the user task to a user
            client.newAssignUserTaskCommand(userTaskKey)
                    .assignee("demo")
                    .send()
                    .join();
            log.info("User task assigned to 'demo': {}", userTaskKey);

            // Complete the user task
            client.newCompleteUserTaskCommand(userTaskKey)
                    .send()
                    .join();
            log.info("User task completed: {}", userTaskKey);
        } else {
            log.warn("No user tasks found for process instance: {}", processInstanceKey);
        }

        Thread.sleep(5000);
        SearchResponse<ProcessInstance> result = client.newProcessInstanceSearchRequest()
                .filter(processInstanceFilter -> processInstanceFilter.processInstanceKey(processInstanceKey))
                .send()
                .join();
        if (!result.items().isEmpty()) {
            ProcessInstance processInstance = result.items().getFirst();
            log.info("Process instance state: {}", processInstance.getState());
        } else {
            log.warn("Process instance not found: {}", processInstanceKey);
        }

    } catch (Exception e) {
        log.error("Error occurred during Camunda orchestration", e);
    }

    }
}
