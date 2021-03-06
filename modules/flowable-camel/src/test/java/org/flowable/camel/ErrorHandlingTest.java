/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.camel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.camel.util.Routing;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * Demonstrates an issue in Activiti 5.12. Exception on Camel routes will be lost, if default error handling is used.
 *
 * @author stefan.schulze@accelsis.biz
 */
@Tag("camel")
@ContextConfiguration("classpath:error-camel-flowable-context.xml")
public class ErrorHandlingTest extends SpringFlowableTestCase {

    private static final int WAIT = 3000;

    private static final String PREVIOUS_WAIT_STATE = "LogProcessStart";
    private static final String NEXT_WAIT_STATE = "ReceiveResult";

    /**
     * Process instance should be removed after completion. Works as intended, if no exception interrupts the Camel route.
     */
    @Test
    @Deployment(resources = {"process/errorHandling.bpmn20.xml"})
    public void testCamelRouteWorksAsIntended() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("routing", Routing.DEFAULT);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ErrorHandling", variables);

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        managementService.executeJob(job.getId());

        Thread.sleep(WAIT);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).as("Process instance not completed").isZero();
    }

    /**
     * Expected behavior, with default error handling in Camel: Roll-back to previous wait state. Fails with Activiti 5.12.
     */
    @Test
    @Deployment(resources = {"process/errorHandling.bpmn20.xml"})
    public void testRollbackOnException() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("routing", Routing.PROVOKE_ERROR);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ErrorHandling", variables);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId(PREVIOUS_WAIT_STATE).count()).as("No roll-back to previous wait state").isEqualTo(1);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId(NEXT_WAIT_STATE).count()).as("Process instance advanced to next wait state").isZero();
    }

    /**
     * Exception caught and processed by Camel dead letter queue handler. Process instance proceeds to ReceiveTask as expected.
     */
    @Test
    @Deployment(resources = {"process/errorHandling.bpmn20.xml"})
    public void testErrorHandledByCamel() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("routing", Routing.HANDLE_ERROR);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ErrorHandling", variables);

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        managementService.executeJob(job.getId());

        Thread.sleep(WAIT);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId(NEXT_WAIT_STATE).count()).as("Process instance did not reach next wait state").isEqualTo(1);
    }
}
