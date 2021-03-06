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

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * 网关活动行为抽象类
 * 用于所有网关活动实现的超类。
 * 
 * @author Joram Barrez
 */
public abstract class GatewayActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected void lockFirstParentScope(DelegateExecution execution) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

        boolean found = false;
        ExecutionEntity parentScopeExecution = null;
        ExecutionEntity currentExecution = (ExecutionEntity) execution;
        while (!found && currentExecution != null && currentExecution.getParentId() != null) {
            parentScopeExecution = executionEntityManager.findById(currentExecution.getParentId());
            if (parentScopeExecution != null && parentScopeExecution.isScope()) {
                found = true;
            }
            currentExecution = parentScopeExecution;
        }

        parentScopeExecution.forceUpdate();
    }

}
