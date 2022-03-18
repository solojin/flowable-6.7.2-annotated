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

package org.activiti.engine.impl.bpmn.behavior;

import java.util.List;

import org.activiti.engine.impl.bpmn.helper.ScopeUtil;
import org.activiti.engine.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * 取消边界事件活动行为类
 *
 * @author Daniel Meyer
 */
public class CancelBoundaryEventActivityBehavior extends FlowNodeActivityBehavior {

    @Override
    public void execute(DelegateExecution execution) {
        ActivityExecution activityExecution = (ActivityExecution) execution;
        List<CompensateEventSubscriptionEntity> eventSubscriptions = ((ExecutionEntity) execution).getCompensateEventSubscriptions();

        if (eventSubscriptions.isEmpty()) {
            leave(activityExecution);
        } else {
            // 取消边界总是同步的
            ScopeUtil.throwCompensationEvent(eventSubscriptions, activityExecution, false);
        }
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
        // 加入补偿执行
        if (execution.getExecutions().isEmpty()) {
            leave(execution);
        } else {
            ((ExecutionEntity) execution).forceUpdate();
        }
    }

}
