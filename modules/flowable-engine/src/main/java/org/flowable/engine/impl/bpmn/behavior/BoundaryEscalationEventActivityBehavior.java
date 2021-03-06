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
package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.bpmn.model.Escalation;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * 边界扩大事件活动行为类
 *
 * 扩大事件大多数用在连接子流程和父流程，用于触发一个子流程，不能发起流程实例
 * 与错误事件不同，扩大事件不是临界事件，在抛出扩大事件的地方会继续执行
 *
 * @author Tijs Rademakers
 */
public class BoundaryEscalationEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected EscalationEventDefinition escalationEventDefinition;
    protected Escalation escalation;

    public BoundaryEscalationEventActivityBehavior(EscalationEventDefinition escalationEventDefinition, Escalation escalation, boolean interrupting) {
        super(interrupting);
        this.escalationEventDefinition = escalationEventDefinition;
        this.escalation = escalation;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        String escalationCode = null;
        String escalationName = null;
        if (escalation != null) {
            escalationCode = escalation.getEscalationCode();
            escalationName = escalation.getName();
        } else {
            escalationCode = escalationEventDefinition.getEscalationCode();
        }

        // 从命令上下文中获取流程引擎配置，进而获取事件调遣器
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEscalationEvent(FlowableEngineEventType.ACTIVITY_ESCALATION_WAITING, executionEntity.getActivityId(), escalationCode,
                    escalationName, executionEntity.getId(), executionEntity.getProcessInstanceId(), executionEntity.getProcessDefinitionId()),
                    processEngineConfiguration.getEngineCfgKey());
        }
    }
}