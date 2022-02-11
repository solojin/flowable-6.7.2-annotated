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
package org.flowable.engine.impl.jobexecutor;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.StartProcessInstanceCmd;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时启动事件作业处理器
 * 可以在开始事件中设置定时启动，这样当流程部署完成后，超过预定时间，流程实例自动启动
 * */
public class TimerStartEventJobHandler extends TimerEventHandler implements JobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerStartEventJobHandler.class);

    // 类型：定时启动事件
    public static final String TYPE = "timer-start-event";

    // 获取作业处理器类型
    @Override
    public String getType() {
        return TYPE;
    }

    // 执行作业，configuration配置，variableScope变量范围，commandContext命令上下文
    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {

        ProcessDefinitionEntity processDefinitionEntity = ProcessDefinitionUtil
                .getProcessDefinitionFromDatabase(job.getProcessDefinitionId()); // From DB -> need to get latest suspended state
        if (processDefinitionEntity == null) {
            throw new FlowableException("找不到定时器启动事件所需的进程定义");
        }

        try {
            if (!processDefinitionEntity.isSuspended()) {
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_FIRED, job),
                            processEngineConfiguration.getEngineCfgKey());
                }

                // 找到与信号启动事件匹配的初始流程元素
                org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(job.getProcessDefinitionId());
                String activityId = TimerEventHandler.getActivityIdFromConfiguration(configuration);
                if (activityId != null) {
                    FlowElement flowElement = process.getFlowElement(activityId, true);
                    if (flowElement == null) {
                        // 找不到与activityId匹配的流程元素
                        throw new FlowableException("Could not find matching FlowElement for activityId " + activityId);
                    }
                    ProcessInstanceHelper processInstanceHelper = processEngineConfiguration.getProcessInstanceHelper();
                    processInstanceHelper.createAndStartProcessInstanceWithInitialFlowElement(processDefinitionEntity, null, null, null, flowElement, process
                            , null, null, true);
                } else {
                    new StartProcessInstanceCmd(processDefinitionEntity.getKey(), null, null, null, job.getTenantId()).execute(commandContext);
                }

            } else {
                // 忽略挂起的进程定义{}的定时器
                LOGGER.debug("ignoring timer of suspended process definition {}", processDefinitionEntity.getName());
            }
        } catch (RuntimeException e) {
            // 定时器执行期间出现异常
            LOGGER.error("exception during timer execution", e);
            throw e;
        } catch (Exception e) {
            // 定时器执行期间出现异常
            LOGGER.error("exception during timer execution", e);
            // // 定时器执行期间出现异常
            throw new FlowableException("exception during timer execution: " + e.getMessage(), e);
        }
    }
}
