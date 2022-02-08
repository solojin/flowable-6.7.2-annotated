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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeploymentSettings;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class BpmnDeployer implements EngineDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnDeployer.class);

    // ID生成器
    protected IdGenerator idGenerator;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected BpmnDeploymentHelper bpmnDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected ProcessDefinitionDiagramHelper processDefinitionDiagramHelper;
    protected boolean usePrefixId;

    @Override
    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        LOGGER.debug("Processing deployment {}", deployment.getName());

        // 解析部署包含部署、流程定义和BPMN与每个流程定义关联的资源、解析和模型.
        ParsedDeployment parsedDeployment = parsedDeploymentBuilderFactory
                .getBuilderForDeploymentAndSettings(deployment, deploymentSettings)
                .build();

        bpmnDeploymentHelper.verifyProcessDefinitionsDoNotShareKeys(parsedDeployment.getAllProcessDefinitions());

        bpmnDeploymentHelper.copyDeploymentValuesToProcessDefinitions(
                parsedDeployment.getDeployment(), parsedDeployment.getAllProcessDefinitions());
        bpmnDeploymentHelper.setResourceNamesOnProcessDefinitions(parsedDeployment);

        createAndPersistNewDiagramsIfNeeded(parsedDeployment);
        setProcessDefinitionDiagramNames(parsedDeployment);

        if (deployment.isNew()) {
            if (!deploymentSettings.containsKey(DeploymentSettings.IS_DERIVED_DEPLOYMENT)) {
                Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapOfNewProcessDefinitionToPreviousVersion = getPreviousVersionsOfProcessDefinitions(parsedDeployment);
                setProcessDefinitionVersionsAndIds(parsedDeployment, mapOfNewProcessDefinitionToPreviousVersion);
                persistProcessDefinitionsAndAuthorizations(parsedDeployment);
                updateTimersAndEvents(parsedDeployment, mapOfNewProcessDefinitionToPreviousVersion);

            } else {
                Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapOfNewProcessDefinitionToPreviousDerivedVersion = 
                                getPreviousDerivedFromVersionsOfProcessDefinitions(parsedDeployment);
                setDerivedProcessDefinitionVersionsAndIds(parsedDeployment, mapOfNewProcessDefinitionToPreviousDerivedVersion, deploymentSettings);
                persistProcessDefinitionsAndAuthorizations(parsedDeployment);
            }
          
        } else {
            makeProcessDefinitionsConsistentWithPersistedVersions(parsedDeployment);
        }

        cachingAndArtifactsManager.updateCachingAndArtifacts(parsedDeployment);

        if (deployment.isNew()) {
            dispatchProcessDefinitionEntityInitializedEvent(parsedDeployment);
        }

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);
            createLocalizationValues(processDefinition.getId(), bpmnModel.getProcessById(processDefinition.getKey()));
        }
    }

    /**
     * 如果部署是新的，为流程定义创建新图表、相关流程定义支持它，并且配置引擎以创建新图表。
     * 当此方法创建新图表时，它还通过资源实体管理器将其持久化，并将其添加到部署的资源中。
     */
    protected void createAndPersistNewDiagramsIfNeeded(ParsedDeployment parsedDeployment) {
        final ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        final DeploymentEntity deploymentEntity = parsedDeployment.getDeployment();

        final ResourceEntityManager resourceEntityManager = processEngineConfiguration.getResourceEntityManager();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            if (processDefinitionDiagramHelper.shouldCreateDiagram(processDefinition, deploymentEntity)) {
                ResourceEntity resource = processDefinitionDiagramHelper.createDiagramForProcessDefinition(
                        processDefinition, parsedDeployment.getBpmnParseForProcessDefinition(processDefinition));
                if (resource != null) {
                    resourceEntityManager.insert(resource, false);
                    deploymentEntity.addResource(resource); // now we'll find it if we look for the diagram name later.
                }
            }
        }
    }

    /**
     * 更新所有流程定义实体，使其具有正确的图表资源名称。
     * 必须在createAndPersistNewDiagramsAsNeeded方法之后调用，以确保所有新创建的图都已将他们的资源附加到部署中。
     */
    protected void setProcessDefinitionDiagramNames(ParsedDeployment parsedDeployment) {
        Map<String, EngineResource> resources = parsedDeployment.getDeployment().getResources();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            String diagramResourceName = ResourceNameUtil.getProcessDiagramResourceNameFromDeployment(processDefinition, resources);
            processDefinition.setDiagramResourceName(diagramResourceName);
        }
    }

    /**
     * 由主键和租户构造从新P流程定义实体到上一版本的映射。
     * 如果不存在以前的版本，则不会创建映射条目。
     */
    protected Map<ProcessDefinitionEntity, ProcessDefinitionEntity> getPreviousVersionsOfProcessDefinitions(
            ParsedDeployment parsedDeployment) {

        Map<ProcessDefinitionEntity, ProcessDefinitionEntity> result = new LinkedHashMap<>();

        for (ProcessDefinitionEntity newDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity existingDefinition = bpmnDeploymentHelper.getMostRecentVersionOfProcessDefinition(newDefinition);

            if (existingDefinition != null) {
                result.put(newDefinition, existingDefinition);
            }
        }

        return result;
    }
    
    /**
     * 构造一个映射从新的流程定义实体到根据主键和租户从版本派生的上一个流程定义实体。
     * 如果不存在以前的版本，则不会创建映射条目。
     */
    protected Map<ProcessDefinitionEntity, ProcessDefinitionEntity> getPreviousDerivedFromVersionsOfProcessDefinitions(
            ParsedDeployment parsedDeployment) {

        Map<ProcessDefinitionEntity, ProcessDefinitionEntity> result = new LinkedHashMap<>();

        for (ProcessDefinitionEntity newDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity existingDefinition = bpmnDeploymentHelper.getMostRecentDerivedVersionOfProcessDefinition(newDefinition);

            if (existingDefinition != null) {
                result.put(newDefinition, existingDefinition);
            }
        }

        return result;
    }

    /**
     * 设置每个流程定义实体的版本和标识符。
     * 如果映射包含流程定义的旧版本，则该版本将设置为该旧实体的版本加一；否则设置为1.
     * 还调度实体创建 ENTITY_CREATED 事件。
     */
    protected void setProcessDefinitionVersionsAndIds(ParsedDeployment parsedDeployment,
            Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions) {
        
        CommandContext commandContext = Context.getCommandContext();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            int version = 1;

            ProcessDefinitionEntity latest = mapNewToOldProcessDefinitions.get(processDefinition);
            if (latest != null) {
                version = latest.getVersion() + 1;
            }

            processDefinition.setVersion(version);
            processDefinition.setId(getIdForNewProcessDefinition(processDefinition));
            Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
            FlowElement initialElement = process.getInitialFlowElement();
            if (initialElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) initialElement;
                if (startEvent.getFormKey() != null) {
                    processDefinition.setHasStartFormKey(true);
                }
            }

            cachingAndArtifactsManager.updateProcessDefinitionCache(parsedDeployment);

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processDefinition),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }
    }
    
    protected void setDerivedProcessDefinitionVersionsAndIds(ParsedDeployment parsedDeployment, 
            Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions, Map<String, Object> deploymentSettings) {
        
        CommandContext commandContext = Context.getCommandContext();
        
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            int derivedVersion = 1;
            
            ProcessDefinitionEntity latest = mapNewToOldProcessDefinitions.get(processDefinition);
            if (latest != null) {
                derivedVersion = latest.getDerivedVersion() + 1;
            }
            
            processDefinition.setVersion(0);
            processDefinition.setDerivedVersion(derivedVersion);
            if (usePrefixId) {
                processDefinition.setId(processDefinition.getIdPrefix() + idGenerator.getNextId());
            } else {
                processDefinition.setId(idGenerator.getNextId());
            }
            
            processDefinition.setDerivedFrom((String) deploymentSettings.get(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ID));
            processDefinition.setDerivedFromRoot((String) deploymentSettings.get(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ROOT_ID));

            Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
            FlowElement initialElement = process.getInitialFlowElement();
            if (initialElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) initialElement;
                if (startEvent.getFormKey() != null) {
                    processDefinition.setHasStartFormKey(true);
                }
            }
            
            cachingAndArtifactsManager.updateProcessDefinitionCache(parsedDeployment);

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processDefinition),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }
    }

    /**
     * 保存每个进程定义。假设部署是新的，之前从未保存过定义，并且它们的所有值都已正确设置.
     */
    protected void persistProcessDefinitionsAndAuthorizations(ParsedDeployment parsedDeployment) {
        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ProcessDefinitionEntityManager processDefinitionManager = processEngineConfiguration.getProcessDefinitionEntityManager();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            processDefinitionManager.insert(processDefinition, false);
            bpmnDeploymentHelper.addAuthorizationsForNewProcessDefinition(parsedDeployment.getProcessModelForProcessDefinition(processDefinition), processDefinition);
        }
    }

    protected void updateTimersAndEvents(ParsedDeployment parsedDeployment,
            Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions) {

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            bpmnDeploymentHelper.updateTimersAndEvents(processDefinition,
                    mapNewToOldProcessDefinitions.get(processDefinition),
                    parsedDeployment);
        }
    }

    protected void dispatchProcessDefinitionEntityInitializedEvent(ParsedDeployment parsedDeployment) {
        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        for (ProcessDefinitionEntity processDefinitionEntity : parsedDeployment.getAllProcessDefinitions()) {
            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, processDefinitionEntity),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }
    }

    /**
     * 返回用于新进程定义的ID;
     * 子类可以覆盖此项以提供自己的标识方案。
     * 整个引擎中的流程定义ID必须是唯一的！
     */
    protected String getIdForNewProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String prefixId = "";
        if (usePrefixId) {
            prefixId = processDefinition.getIdPrefix();
        } 
        
        String nextId = idGenerator.getNextId();

        String result = prefixId + processDefinition.getKey() + ":" + processDefinition.getVersion() + ":" + nextId; // ACT-505
        // ACT-115: id的最大长度为64个字符
        if (result.length() > 64) {
            // 由于长度超出了长进程定义键
            result = prefixId + nextId;
        }

        return result;
    }

    /**
     * 加载每个进程定义的持久化版本，并将内存版本上的值设置为一致。
     */
    protected void makeProcessDefinitionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment) {
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity persistedProcessDefinition = bpmnDeploymentHelper.getPersistedInstanceOfProcessDefinition(processDefinition);

            if (persistedProcessDefinition != null) {
                processDefinition.setId(persistedProcessDefinition.getId());
                processDefinition.setVersion(persistedProcessDefinition.getVersion());
                processDefinition.setSuspensionState(persistedProcessDefinition.getSuspensionState());
                processDefinition.setHasStartFormKey(persistedProcessDefinition.hasStartFormKey());
                processDefinition.setGraphicalNotationDefined(persistedProcessDefinition.isGraphicalNotationDefined());
            }
        }
    }

    protected void createLocalizationValues(String processDefinitionId, Process process) {
        if (process == null) {
            return;
        }

        CommandContext commandContext = Context.getCommandContext();
        DynamicBpmnService dynamicBpmnService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicBpmnService();
        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processDefinitionId);

        boolean localizationValuesChanged = false;
        List<ExtensionElement> localizationElements = process.getExtensionElements().get("localization");
        if (localizationElements != null) {
            for (ExtensionElement localizationElement : localizationElements) {
                if (BpmnXMLConstants.FLOWABLE_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix()) ||
                        BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {

                    String locale = localizationElement.getAttributeValue(null, "locale");
                    String name = localizationElement.getAttributeValue(null, "name");
                    String documentation = null;
                    List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
                    if (documentationElements != null) {
                        for (ExtensionElement documentationElement : documentationElements) {
                            documentation = StringUtils.trimToNull(documentationElement.getElementText());
                            break;
                        }
                    }

                    String processId = process.getId();
                    if (!isEqualToCurrentLocalizationValue(locale, processId, "name", name, infoNode)) {
                        dynamicBpmnService.changeLocalizationName(locale, processId, name, infoNode);
                        localizationValuesChanged = true;
                    }

                    if (documentation != null && !isEqualToCurrentLocalizationValue(locale, processId, "description", documentation, infoNode)) {
                        dynamicBpmnService.changeLocalizationDescription(locale, processId, documentation, infoNode);
                        localizationValuesChanged = true;
                    }
                }
            }
        }

        boolean isFlowElementLocalizationChanged = localizeFlowElements(process.getFlowElements(), infoNode);
        boolean isDataObjectLocalizationChanged = localizeDataObjectElements(process.getDataObjects(), infoNode);
        if (isFlowElementLocalizationChanged || isDataObjectLocalizationChanged) {
            localizationValuesChanged = true;
        }

        if (localizationValuesChanged) {
            dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);
        }
    }

    protected boolean localizeFlowElements(Collection<FlowElement> flowElements, ObjectNode infoNode) {
        boolean localizationValuesChanged = false;

        if (flowElements == null) {
            return localizationValuesChanged;
        }

        CommandContext commandContext = Context.getCommandContext();
        DynamicBpmnService dynamicBpmnService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicBpmnService();

        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask || flowElement instanceof SubProcess) {
                List<ExtensionElement> localizationElements = flowElement.getExtensionElements().get("localization");
                if (localizationElements != null) {
                    for (ExtensionElement localizationElement : localizationElements) {
                        if (BpmnXMLConstants.FLOWABLE_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix()) ||
                                BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {

                            String locale = localizationElement.getAttributeValue(null, "locale");
                            String name = localizationElement.getAttributeValue(null, "name");
                            String documentation = null;
                            List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
                            if (documentationElements != null) {
                                for (ExtensionElement documentationElement : documentationElements) {
                                    documentation = StringUtils.trimToNull(documentationElement.getElementText());
                                    break;
                                }
                            }

                            String flowElementId = flowElement.getId();
                            if (!isEqualToCurrentLocalizationValue(locale, flowElementId, "name", name, infoNode)) {
                                dynamicBpmnService.changeLocalizationName(locale, flowElementId, name, infoNode);
                                localizationValuesChanged = true;
                            }

                            if (documentation != null && !isEqualToCurrentLocalizationValue(locale, flowElementId, "description", documentation, infoNode)) {
                                dynamicBpmnService.changeLocalizationDescription(locale, flowElementId, documentation, infoNode);
                                localizationValuesChanged = true;
                            }
                        }
                    }
                }

                if (flowElement instanceof SubProcess) {
                    SubProcess subprocess = (SubProcess) flowElement;
                    boolean isFlowElementLocalizationChanged = localizeFlowElements(subprocess.getFlowElements(), infoNode);
                    boolean isDataObjectLocalizationChanged = localizeDataObjectElements(subprocess.getDataObjects(), infoNode);
                    if (isFlowElementLocalizationChanged || isDataObjectLocalizationChanged) {
                        localizationValuesChanged = true;
                    }
                }
            }
        }

        return localizationValuesChanged;
    }

    protected boolean isEqualToCurrentLocalizationValue(String language, String id, String propertyName, String propertyValue, ObjectNode infoNode) {
        boolean isEqual = false;
        JsonNode localizationNode = infoNode.path("localization").path(language).path(id).path(propertyName);
        if (!localizationNode.isMissingNode() && !localizationNode.isNull() && localizationNode.asText().equals(propertyValue)) {
            isEqual = true;
        }
        return isEqual;
    }

    protected boolean localizeDataObjectElements(List<ValuedDataObject> dataObjects, ObjectNode infoNode) {
        boolean localizationValuesChanged = false;
        CommandContext commandContext = Context.getCommandContext();
        DynamicBpmnService dynamicBpmnService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicBpmnService();

        for (ValuedDataObject dataObject : dataObjects) {
            List<ExtensionElement> localizationElements = dataObject.getExtensionElements().get("localization");
            if (localizationElements != null) {
                for (ExtensionElement localizationElement : localizationElements) {
                    if (BpmnXMLConstants.FLOWABLE_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix()) ||
                            BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {

                        String locale = localizationElement.getAttributeValue(null, "locale");
                        String name = localizationElement.getAttributeValue(null, "name");
                        String documentation = null;

                        List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
                        if (documentationElements != null) {
                            for (ExtensionElement documentationElement : documentationElements) {
                                documentation = StringUtils.trimToNull(documentationElement.getElementText());
                                break;
                            }
                        }

                        if (name != null && !isEqualToCurrentLocalizationValue(locale, dataObject.getId(), DynamicBpmnConstants.LOCALIZATION_NAME, name, infoNode)) {
                            dynamicBpmnService.changeLocalizationName(locale, dataObject.getId(), name, infoNode);
                            localizationValuesChanged = true;
                        }

                        if (documentation != null && !isEqualToCurrentLocalizationValue(locale, dataObject.getId(),
                                DynamicBpmnConstants.LOCALIZATION_DESCRIPTION, documentation, infoNode)) {

                            dynamicBpmnService.changeLocalizationDescription(locale, dataObject.getId(), documentation, infoNode);
                            localizationValuesChanged = true;
                        }
                    }
                }
            }
        }

        return localizationValuesChanged;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public ParsedDeploymentBuilderFactory getExParsedDeploymentBuilderFactory() {
        return parsedDeploymentBuilderFactory;
    }

    public void setParsedDeploymentBuilderFactory(ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory) {
        this.parsedDeploymentBuilderFactory = parsedDeploymentBuilderFactory;
    }

    public BpmnDeploymentHelper getBpmnDeploymentHelper() {
        return bpmnDeploymentHelper;
    }

    public void setBpmnDeploymentHelper(BpmnDeploymentHelper bpmnDeploymentHelper) {
        this.bpmnDeploymentHelper = bpmnDeploymentHelper;
    }

    public CachingAndArtifactsManager getCachingAndArtifcatsManager() {
        return cachingAndArtifactsManager;
    }

    public void setCachingAndArtifactsManager(CachingAndArtifactsManager manager) {
        this.cachingAndArtifactsManager = manager;
    }

    public ProcessDefinitionDiagramHelper getProcessDefinitionDiagramHelper() {
        return processDefinitionDiagramHelper;
    }

    public void setProcessDefinitionDiagramHelper(ProcessDefinitionDiagramHelper processDefinitionDiagramHelper) {
        this.processDefinitionDiagramHelper = processDefinitionDiagramHelper;
    }

    public boolean isUsePrefixId() {
        return usePrefixId;
    }

    public void setUsePrefixId(boolean usePrefixId) {
        this.usePrefixId = usePrefixId;
    }
}
