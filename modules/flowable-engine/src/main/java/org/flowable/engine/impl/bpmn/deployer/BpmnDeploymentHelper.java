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
package org.flowable.engine.impl.bpmn.deployer;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

/**
 * Methods for working with deployments. Much of the actual work of {@link BpmnDeployer} is done by orchestrating the different pieces of work this class does; by having them here, we allow other
 * deployers to make use of them.
 */
public class BpmnDeploymentHelper {

    protected TimerManager timerManager;
    protected EventSubscriptionManager eventSubscriptionManager;

    /**
     * 验证没有两个进程定义共享同一个键，以防止数据库唯一索引冲突。
     * 
     * @throws FlowableException
     *             if any two processes have the same key
     */
    public void verifyProcessDefinitionsDoNotShareKeys(Collection<ProcessDefinitionEntity> processDefinitions) {
        Set<String> keySet = new LinkedHashSet<>();
        for (ProcessDefinitionEntity processDefinition : processDefinitions) {
            if (keySet.contains(processDefinition.getKey())) {
                throw new FlowableException(
                        "The deployment contains process definitions with the same key '" + processDefinition.getKey() + "' (process id attribute), this is not allowed");
            }
            keySet.add(processDefinition.getKey());
        }
    }

    /**
     * 更新所有流程定义实体，以匹配租户、引擎版本和部署id的部署值。
     */
    public void copyDeploymentValuesToProcessDefinitions(DeploymentEntity deployment,
            List<ProcessDefinitionEntity> processDefinitions) {
        String engineVersion = deployment.getEngineVersion();
        String tenantId = deployment.getTenantId();
        String deploymentId = deployment.getId();

        for (ProcessDefinitionEntity processDefinition : processDefinitions) {

            // Backwards compatibility
            if (engineVersion != null) {
                processDefinition.setEngineVersion(engineVersion);
            }

            // process definition inherits the tenant id
            if (tenantId != null) {
                processDefinition.setTenantId(tenantId);
            }

            processDefinition.setDeploymentId(deploymentId);
        }
    }

    /**
     * 更新所有流程定义实体以具有正确的资源名称。
     */
    public void setResourceNamesOnProcessDefinitions(ParsedDeployment parsedDeployment) {
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            String resourceName = parsedDeployment.getResourceForProcessDefinition(processDefinition).getName();
            processDefinition.setResourceName(resourceName);
        }
    }

    /**
     * 获取与租户和键的最新持久化进程定义匹配的最新进程定义。如果未找到，则返回null。此方法假定租户和密钥在
     * 流程定义实体。
     */
    public ProcessDefinitionEntity getMostRecentVersionOfProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String key = processDefinition.getKey();
        String tenantId = processDefinition.getTenantId();
        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();

        ProcessDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = processDefinitionManager.findLatestProcessDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = processDefinitionManager.findLatestProcessDefinitionByKey(key);
        }

        return existingDefinition;
    }
    
    /**
     * Gets the most recent persisted derived process definition that matches this one for tenant and key. If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * process definition entity.
     */
    public ProcessDefinitionEntity getMostRecentDerivedVersionOfProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String key = processDefinition.getKey();
        String tenantId = processDefinition.getTenantId();
        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();

        ProcessDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = processDefinitionManager.findLatestDerivedProcessDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = processDefinitionManager.findLatestDerivedProcessDefinitionByKey(key);
        }

        return existingDefinition;
    }

    /**
     * Gets the persisted version of the already-deployed process definition. Note that this is different from {@link #getMostRecentVersionOfProcessDefinition} as it looks specifically for a process
     * definition that is already persisted and attached to a particular deployment, rather than the latest version across all deployments.
     */
    public ProcessDefinitionEntity getPersistedInstanceOfProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String deploymentId = processDefinition.getDeploymentId();
        if (StringUtils.isEmpty(processDefinition.getDeploymentId())) {
            throw new IllegalStateException("Provided process definition must have a deployment id.");
        }

        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();
        ProcessDefinitionEntity persistedProcessDefinition = null;
        if (processDefinition.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
            persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKey(deploymentId, processDefinition.getKey());
        } else {
            persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, processDefinition.getKey(), processDefinition.getTenantId());
        }

        return persistedProcessDefinition;
    }

    /**
     * Updates all timers and events for the process definition. This removes obsolete message and signal subscriptions and timers, and adds new ones.
     */
    public void updateTimersAndEvents(ProcessDefinitionEntity processDefinition,
            ProcessDefinitionEntity previousProcessDefinition, ParsedDeployment parsedDeployment) {

        Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
        BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);

        eventSubscriptionManager.removeObsoleteMessageEventSubscriptions(previousProcessDefinition);
        eventSubscriptionManager.removeObsoleteSignalEventSubScription(previousProcessDefinition);
        eventSubscriptionManager.removeObsoleteEventRegistryEventSubScription(previousProcessDefinition);
        eventSubscriptionManager.addEventSubscriptions(processDefinition, process, bpmnModel);

        timerManager.removeObsoleteTimers(processDefinition);
        timerManager.scheduleTimers(processDefinition, process);
    }

    enum ExpressionType {
        USER, GROUP
    }

    /**
     * @param processDefinition
     */
    public void addAuthorizationsForNewProcessDefinition(Process process, ProcessDefinitionEntity processDefinition) {
        CommandContext commandContext = Context.getCommandContext();

        addAuthorizationsFromIterator(commandContext, process.getCandidateStarterUsers(), processDefinition, ExpressionType.USER);
        addAuthorizationsFromIterator(commandContext, process.getCandidateStarterGroups(), processDefinition, ExpressionType.GROUP);
    }

    protected void addAuthorizationsFromIterator(CommandContext commandContext, List<String> expressions,
            ProcessDefinitionEntity processDefinition, ExpressionType expressionType) {

        if (expressions != null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            IdentityLinkService identityLinkService = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService();
            Iterator<String> iterator = expressions.iterator();
            while (iterator.hasNext()) {
                @SuppressWarnings("cast")
                String expression = iterator.next();
                IdentityLinkEntity identityLink = identityLinkService.createIdentityLink();
                identityLink.setProcessDefId(processDefinition.getId());
                if (expressionType == ExpressionType.USER) {
                    identityLink.setUserId(expression);
                } else if (expressionType == ExpressionType.GROUP) {
                    identityLink.setGroupId(expression);
                }
                identityLink.setType(IdentityLinkType.CANDIDATE);
                identityLinkService.insertIdentityLink(identityLink);
            }
        }

    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    public EventSubscriptionManager getEventSubscriptionManager() {
        return eventSubscriptionManager;
    }

    public void setEventSubscriptionManager(EventSubscriptionManager eventSubscriptionManager) {
        this.eventSubscriptionManager = eventSubscriptionManager;
    }
}
