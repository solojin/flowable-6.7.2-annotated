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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * 抽象BPMN元素活动行为类
 *
 * 表示BPMN 2.0意义上的“活动”：所有任务、子流程和调用活动的父类。
 * 
 * @author Joram Barrez
 */
public class AbstractBpmnActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    // 多实例活动行为
    protected MultiInstanceActivityBehavior multiInstanceActivityBehavior;

    /**
     * 调用leave（）的子类将首先通过此方法，然后调用常规的{@link FlowNodeActivityBehavior#leave（DelegateExecution）}。
     * 通过这种方式，我们可以检查活动是否具有循环特征，如果是这种情况，则委托给行为。
     */
    @Override
    public void leave(DelegateExecution execution) {
        FlowElement currentFlowElement = execution.getCurrentFlowElement();
        Collection<BoundaryEvent> boundaryEvents = findBoundaryEventsForFlowNode(execution.getProcessDefinitionId(), currentFlowElement);
        if (CollectionUtil.isNotEmpty(boundaryEvents)) {
            executeCompensateBoundaryEvents(boundaryEvents, execution);
        }
        if (!hasLoopCharacteristics()) {
            super.leave(execution);
        } else if (hasMultiInstanceCharacteristics()) {
            multiInstanceActivityBehavior.leave(execution);
        }
    }

    protected void executeCompensateBoundaryEvents(Collection<BoundaryEvent> boundaryEvents, DelegateExecution execution) {

        // 父执行成为作用域，并为每个边界事件创建子执行
        for (BoundaryEvent boundaryEvent : boundaryEvents) {

            if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())) {
                continue;
            }

            if (!(boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                continue;
            }

            ExecutionEntity childExecutionEntity = CommandContextUtil.getExecutionEntityManager().createChildExecution((ExecutionEntity) execution);
            childExecutionEntity.setParentId(execution.getId());
            childExecutionEntity.setCurrentFlowElement(boundaryEvent);
            childExecutionEntity.setScope(false);

            ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
            boundaryEventBehavior.execute(childExecutionEntity);
        }

    }

    protected Collection<BoundaryEvent> findBoundaryEventsForFlowNode(final String processDefinitionId, final FlowElement flowElement) {
        Process process = getProcessDefinition(processDefinitionId);

        // 这可以缓存，也可以在解析时完成
        List<BoundaryEvent> results = new ArrayList<>(1);
        Collection<BoundaryEvent> boundaryEvents = process.findFlowElementsOfType(BoundaryEvent.class, true);
        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            if (boundaryEvent.getAttachedToRefId() != null && boundaryEvent.getAttachedToRefId().equals(flowElement.getId())) {
                results.add(boundaryEvent);
            }
        }
        return results;
    }

    protected Process getProcessDefinition(String processDefinitionId) {
        // TODO:必须提取/应以其他方式访问缓存
        return ProcessDefinitionUtil.getProcess(processDefinitionId);
    }

    protected boolean hasLoopCharacteristics() {
        return hasMultiInstanceCharacteristics();
    }

    protected boolean hasMultiInstanceCharacteristics() {
        return multiInstanceActivityBehavior != null;
    }

    public MultiInstanceActivityBehavior getMultiInstanceActivityBehavior() {
        return multiInstanceActivityBehavior;
    }

    public void setMultiInstanceActivityBehavior(MultiInstanceActivityBehavior multiInstanceActivityBehavior) {
        this.multiInstanceActivityBehavior = multiInstanceActivityBehavior;
    }

}
