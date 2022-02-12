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
package org.activiti.engine.impl.jobexecutor;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.job.api.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时器执行嵌套活动作业处理器
 * 即任务超时作业。在实际项目中，可以为任务节点绑定一个定时边界事件，如果任务节点在指定时间之内没有结束，则流程引擎实例会按照边界事件的方向执行
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TimerExecuteNestedActivityJobHandler extends TimerEventHandler implements JobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerExecuteNestedActivityJobHandler.class);

    // 类型：定时器-转换
    public static final String TYPE = "timer-transition";
    public static final String PROPERTYNAME_TIMER_ACTIVITY_ID = "activityId";
    public static final String PROPERTYNAME_END_DATE_EXPRESSION = "timerEndDate";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(Job job, String configuration, ExecutionEntity execution, CommandContext commandContext) {

        String nestedActivityId = TimerEventHandler.getActivityIdFromConfiguration(configuration);

        ActivityImpl borderEventActivity = execution.getProcessDefinition().findActivity(nestedActivityId);

        if (borderEventActivity == null) {
            // 触发定时器时出错：未找到边界事件活动“+nestedActivityId+”
            throw new ActivitiException("Error while firing timer: border event activity " + nestedActivityId + " not found");
        }

        try {
            if (commandContext.getEventDispatcher().isEnabled()) {
                commandContext.getEventDispatcher().dispatchEvent(
                        ActivitiEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_FIRED, job),
                        EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
                dispatchActivityTimeoutIfNeeded(job, execution, commandContext);
            }

            borderEventActivity
                    .getActivityBehavior()
                    .execute(execution);
        } catch (RuntimeException e) {
            // 定时器执行期间出现异常
            LOGGER.error("exception during timer execution", e);
            throw e;

        } catch (Exception e) {
            // 定时器执行期间出现异常
            LOGGER.error("exception during timer execution", e);
            // 定时器执行期间出现异常
            throw new ActivitiException("exception during timer execution: " + e.getMessage(), e);
        }
    }

    // 条件调度超时活动
    protected void dispatchActivityTimeoutIfNeeded(Job timerEntity, ExecutionEntity execution, CommandContext commandContext) {

        String nestedActivityId = TimerEventHandler.getActivityIdFromConfiguration(timerEntity.getJobHandlerConfiguration());

        ActivityImpl boundaryEventActivity = execution.getProcessDefinition().findActivity(nestedActivityId);
        ActivityBehavior boundaryActivityBehavior = boundaryEventActivity.getActivityBehavior();
        if (boundaryActivityBehavior instanceof BoundaryEventActivityBehavior) {
            BoundaryEventActivityBehavior boundaryEventActivityBehavior = (BoundaryEventActivityBehavior) boundaryActivityBehavior;
            if (boundaryEventActivityBehavior.isInterrupting()) {
                dispatchExecutionTimeOut(timerEntity, execution, commandContext);
            }
        }
    }

    // 调度超时执行
    protected void dispatchExecutionTimeOut(Job job, ExecutionEntity execution, CommandContext commandContext) {
        // 子流程
        for (ExecutionEntity subExecution : execution.getExecutions()) {
            dispatchExecutionTimeOut(job, subExecution, commandContext);
        }

        // 调用活动
        ExecutionEntity subProcessInstance = commandContext.getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(execution.getId());
        if (subProcessInstance != null) {
            dispatchExecutionTimeOut(job, subProcessInstance, commandContext);
        }

        // 带有定时器边界事件的活动
        ActivityImpl activity = execution.getActivity();
        if (activity != null && activity.getActivityBehavior() != null) {
            dispatchActivityTimeOut(job, activity, execution, commandContext);
        }
    }

    // 调度超时活动
    protected void dispatchActivityTimeOut(Job job, ActivityImpl activity, ExecutionEntity execution, CommandContext commandContext) {
        commandContext.getEventDispatcher().dispatchEvent(
                ActivitiEventBuilder.createActivityCancelledEvent(activity.getId(),
                        (String) activity.getProperties().get("name"),
                        execution.getId(),
                        execution.getProcessInstanceId(), execution.getProcessDefinitionId(),
                        (String) activity.getProperties().get("type"),
                        activity.getActivityBehavior().getClass().getCanonicalName(),
                        job), EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
    }

}
