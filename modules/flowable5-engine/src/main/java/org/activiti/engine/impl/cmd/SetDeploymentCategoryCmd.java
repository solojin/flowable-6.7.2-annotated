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
package org.activiti.engine.impl.cmd;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.repository.Deployment;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;

/**
 * @author Tijs Rademakers
 */
public class SetDeploymentCategoryCmd implements Command<Void> {

    protected String deploymentId;
    protected String category;

    public SetDeploymentCategoryCmd(String deploymentId, String category) {
        this.deploymentId = deploymentId;
        this.category = category;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        if (deploymentId == null) {
            throw new ActivitiIllegalArgumentException("Deployment id is null");
        }

        DeploymentEntity deployment = commandContext
                .getDeploymentEntityManager()
                .findDeploymentById(deploymentId);

        if (deployment == null) {
            throw new ActivitiObjectNotFoundException("No deployment found for id = '" + deploymentId + "'", Deployment.class);
        }

        // Update category
        deployment.setCategory(category);

        if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    ActivitiEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, deployment),
                    EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        }

        return null;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
