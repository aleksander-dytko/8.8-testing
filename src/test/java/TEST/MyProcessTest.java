package TEST;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.AssignUserTaskResponse;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTest;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.assertions.UserTaskSelectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@CamundaProcessTest
public class MyProcessTest {

    private CamundaClient client;
    private CamundaProcessTestContext processTestContext;

    @BeforeEach
            void deployProcessDefinition() {
        client
                .newDeployResourceCommand()
                .addResourceFromClasspath("demoProcess.bpmn")
                .send()
                .join();
    }

    @Test
    void shouldStartProcessInstance() {
        //when
        final ProcessInstanceEvent processInstance =
                client
                        .newCreateInstanceCommand()
                        .bpmnProcessId("demoProcess")
                        .latestVersion()
                        .send()
                        .join();

        //then
        CamundaAssert.assertThat(processInstance).isActive();
        CamundaAssert.assertThat(processInstance).hasActiveElement("userTask_1",1);
    }

    @Disabled
    @Test
    void shouldFinishProcessInstance() throws InterruptedException {

        //when
        final ProcessInstanceEvent processInstance =
                client
                        .newCreateInstanceCommand()
                        .bpmnProcessId("demoProcess")
                        .latestVersion()
                        .send()
                        .join();

        long processInstanceKey = processInstance.getProcessInstanceKey();

        //then
        CamundaAssert.assertThatUserTask(UserTaskSelectors.byElementId("userTask_1",processInstanceKey)).isCreated();


        //when
        processTestContext.completeUserTask(UserTaskSelectors.byElementId("userTask_1"));

        //then
        CamundaAssert.assertThat(processInstance).isCompleted();


    }
}
