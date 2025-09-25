package TEST;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTest;
import io.camunda.process.test.api.CamundaProcessTestContext;
import org.junit.jupiter.api.Test;


@CamundaProcessTest
public class MyProcessTest {

    private CamundaClient client;
    private CamundaProcessTestContext processTestContext;

    @Test
    void shouldStartProcessInstance() {
        // given
        client
                .newDeployResourceCommand()
                .addResourceFromClasspath("demoProcess.bpmn")
                .send()
                .join();

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
}
