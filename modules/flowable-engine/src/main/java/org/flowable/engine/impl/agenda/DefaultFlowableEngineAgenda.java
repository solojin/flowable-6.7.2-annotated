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
package org.flowable.engine.impl.agenda;

import java.util.Collection;

import org.flowable.common.engine.impl.agenda.AbstractAgenda;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.interceptor.MigrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对于正在执行的每个API调用（以及｛@link Command｝），都会创建一个新的代理实例。
 * 在这个代理上，操作被放置，｛@link CommandExecutor｝将继续执行，直到所有执行。
 *
 * 在编写｛@link ActivityBehavior｝实现时，该代理还允许轻松访问计划新操作的方法。
 *
 * 在执行｛@link Command｝期间，始终可以使用｛@linkCommandContextUtil#getAgenda（）｝获取代理。
 *
 * @author Joram Barrez
 */
public class DefaultFlowableEngineAgenda extends AbstractAgenda implements FlowableEngineAgenda {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFlowableEngineAgenda.class);

    public DefaultFlowableEngineAgenda(CommandContext commandContext) {
        super(commandContext);
    }

    /**
     * Generic method to plan a {@link Runnable}.
     */
    @Override
    public void planOperation(Runnable operation, ExecutionEntity executionEntity) {
        operations.add(operation);
        LOGGER.debug("Operation {} added to agenda", operation.getClass());

        if (executionEntity != null) {
            CommandContextUtil.addInvolvedExecution(commandContext, executionEntity);
        }
    }

    /* SPECIFIC operations */

    @Override
    public void planContinueProcessOperation(ExecutionEntity execution) {
        planOperation(new ContinueProcessOperation(commandContext, execution), execution);
    }

    @Override
    public void planContinueProcessSynchronousOperation(ExecutionEntity execution) {
        planOperation(new ContinueProcessOperation(commandContext, execution, true, false, null), execution);
    }

    @Override
    public void planContinueProcessWithMigrationContextOperation(ExecutionEntity execution, MigrationContext migrationContext) {
        planOperation(new ContinueProcessOperation(commandContext, execution, false, false, migrationContext), execution);
    }

    @Override
    public void planContinueProcessInCompensation(ExecutionEntity execution) {
        planOperation(new ContinueProcessOperation(commandContext, execution, false, true, null), execution);
    }

    @Override
    public void planContinueMultiInstanceOperation(ExecutionEntity execution, ExecutionEntity multiInstanceRootExecution, int loopCounter) {
        planOperation(new ContinueMultiInstanceOperation(commandContext, execution, multiInstanceRootExecution, loopCounter), execution);
    }

    @Override
    public void planTakeOutgoingSequenceFlowsOperation(ExecutionEntity execution, boolean evaluateConditions) {
        planOperation(new TakeOutgoingSequenceFlowsOperation(commandContext, execution, evaluateConditions), execution);
    }

    @Override
    public void planEndExecutionOperation(ExecutionEntity execution) {
        planOperation(new EndExecutionOperation(commandContext, execution), execution);
    }
    
    @Override
    public void planEndExecutionOperationSynchronous(ExecutionEntity execution) {
        planOperation(new EndExecutionOperation(commandContext, execution, true), execution);
    }

    @Override
    public void planTriggerExecutionOperation(ExecutionEntity execution) {
        planOperation(new TriggerExecutionOperation(commandContext, execution), execution);
    }

    @Override
    public void planAsyncTriggerExecutionOperation(ExecutionEntity execution) {
        planOperation(new TriggerExecutionOperation(commandContext, execution, true), execution);
    }
    
    @Override
    public void planEvaluateConditionalEventsOperation(ExecutionEntity execution) {
        planOperation(new EvaluateConditionalEventsOperation(commandContext, execution), execution);
    }
    
    @Override
    public void planEvaluateVariableListenerEventsOperation(String processDefinitionId, String processInstanceId) {
        planOperation(new EvaluateVariableListenerEventDefinitionsOperation(commandContext, processDefinitionId, processInstanceId));
    }

    @Override
    public void planDestroyScopeOperation(ExecutionEntity execution) {
        planOperation(new DestroyScopeOperation(commandContext, execution), execution);
    }

    @Override
    public void planExecuteInactiveBehaviorsOperation(Collection<ExecutionEntity> executions) {
        planOperation(new ExecuteInactiveBehaviorsOperation(commandContext, executions));
    }

}
