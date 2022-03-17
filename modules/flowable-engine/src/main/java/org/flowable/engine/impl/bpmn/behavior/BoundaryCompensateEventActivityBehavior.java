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

import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * 边界补偿事件活动行为类
 *
 * 补偿边界事件（compensation boundary event），可以为活动附加补偿处理器。
 *
 * 补偿边界事件必须使用直接关联的方式引用单个的补偿处理器。
 *
 * 补偿边界事件与其它边界事件的活动策略不同。其它边界事件，例如信号边界事件，在其依附的活动启动时激活；当该活动结束时会被解除，并取消相应的事件订阅。而补偿边界事件不是这样。补偿边界事件在其依附的活动成功完成时激活，同时创建补偿事件的相应订阅。当补偿事件被触发，或者相应的流程实例结束时，才会移除订阅。请考虑下列因素：
 *
 * 当补偿被触发时，会调用补偿边界事件关联的补偿处理器。调用次数与其依附的活动成功完成的次数相同。
 *
 * 如果补偿边界事件依附在具有多实例特性的活动上，则会为每一个实例创建补偿事件订阅。
 *
 * 如果补偿边界事件依附在位于循环内部的活动上，则每次该活动执行时，都会创建一个补偿事件订阅。
 *
 * 如果流程实例结束，则取消补偿事件的订阅。
 *
 * 请注意：嵌入式子流程不支持补偿边界事件。
 *
 * @author Tijs Rademakers
 */
public class BoundaryCompensateEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected CompensateEventDefinition compensateEventDefinition;

    public BoundaryCompensateEventActivityBehavior(CompensateEventDefinition compensateEventDefinition, boolean interrupting) {
        super(interrupting);
        this.compensateEventDefinition = compensateEventDefinition;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());
        if (process == null) {
            throw new FlowableException("Process model (id = " + execution.getId() + ") could not be found");
        }

        Activity sourceActivity = null;
        Activity compensationActivity = null;
        List<Association> associations = process.findAssociationsWithSourceRefRecursive(boundaryEvent.getId());
        for (Association association : associations) {
            sourceActivity = boundaryEvent.getAttachedToRef();
            FlowElement targetElement = process.getFlowElement(association.getTargetRef(), true);
            if (targetElement instanceof Activity) {
                Activity activity = (Activity) targetElement;
                if (activity.isForCompensation()) {
                    compensationActivity = activity;
                    break;
                }
            }
        }
        
        if (sourceActivity == null) {
            throw new FlowableException("Parent activity for boundary compensation event could not be found");
        }

        if (compensationActivity == null) {
            throw new FlowableException("Compensation activity could not be found (or it is missing 'isForCompensation=\"true\"'");
        }

        // 查找子流程或流程实例执行
        ExecutionEntity scopeExecution = null;
        ExecutionEntity parentExecution = executionEntity.getParent();
        while (scopeExecution == null && parentExecution != null) {
            if (parentExecution.getCurrentFlowElement() instanceof SubProcess) {
                scopeExecution = parentExecution;

            } else if (parentExecution.isProcessInstanceType()) {
                scopeExecution = parentExecution;
            } else {
                parentExecution = parentExecution.getParent();
            }
        }

        if (scopeExecution == null) {
            throw new FlowableException("Could not find a scope execution for compensation boundary event " + boundaryEvent.getId());
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) processEngineConfiguration.getEventSubscriptionServiceConfiguration()
                .getEventSubscriptionService().createEventSubscriptionBuilder()
                        .eventType(CompensateEventSubscriptionEntity.EVENT_TYPE)
                        .executionId(scopeExecution.getId())
                        .processInstanceId(scopeExecution.getProcessInstanceId())
                        .activityId(sourceActivity.getId())
                        .tenantId(scopeExecution.getTenantId())
                        .create();
        
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        if (boundaryEvent.isCancelActivity()) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (eventSubscription instanceof CompensateEventSubscriptionEntity && eventSubscription.getActivityId().equals(compensateEventDefinition.getActivityRef())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                    CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscription);
                }
            }
        }

        super.trigger(executionEntity, triggerName, triggerData);
    }
}