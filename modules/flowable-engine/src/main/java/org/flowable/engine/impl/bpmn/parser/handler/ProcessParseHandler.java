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
package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.List;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.event.FlowableEventSupport;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流程解析处理器
 *
 * @author Joram Barrez
 */
public class ProcessParseHandler extends AbstractBpmnParseHandler<Process> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessParseHandler.class);

    public static final String PROPERTYNAME_DOCUMENTATION = "documentation";

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return Process.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, Process process) {
        if (!process.isExecutable()) {
            // 忽略id为“{}”的不可执行进程。设置属性isExecutable=“true”以部署此流程。
            LOGGER.info("Ignoring non-executable process with id='{}'. Set the attribute isExecutable=\"true\" to deploy this process.", process.getId());
        } else {
            bpmnParse.getProcessDefinitions().add(transformProcess(bpmnParse, process));
        }
    }

    protected ProcessDefinitionEntity transformProcess(BpmnParse bpmnParse, Process process) {
        ProcessDefinitionEntity currentProcessDefinition = CommandContextUtil.getProcessDefinitionEntityManager().create();
        bpmnParse.setCurrentProcessDefinition(currentProcessDefinition);

        /*
         * 映射对象模型-bpmn xml:
         *  流程定义id -> 由activiti引擎生成
         *  流程定义主键 -> bpmn id (必要的)
         *  流程定义名称 -> bpmn 名称 (可选)
         */
        currentProcessDefinition.setKey(process.getId());
        currentProcessDefinition.setName(process.getName());
        currentProcessDefinition.setCategory(bpmnParse.getBpmnModel().getTargetNamespace());
        currentProcessDefinition.setDescription(process.getDocumentation());
        currentProcessDefinition.setDeploymentId(bpmnParse.getDeployment().getId());

        if (bpmnParse.getDeployment().getEngineVersion() != null) {
            currentProcessDefinition.setEngineVersion(bpmnParse.getDeployment().getEngineVersion());
        }

        createEventListeners(bpmnParse, process.getEventListeners());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing process {}", currentProcessDefinition.getKey());
        }

        bpmnParse.processFlowElements(process.getFlowElements());
        processArtifacts(bpmnParse, process.getArtifacts());

        return currentProcessDefinition;
    }

    protected void createEventListeners(BpmnParse bpmnParse, List<EventListener> eventListeners) {

        if (eventListeners != null && !eventListeners.isEmpty()) {
            for (EventListener eventListener : eventListeners) {
                // 提取特定事件类型（如果有）
                FlowableEngineEventType[] types = FlowableEngineEventType.getTypesFromString(eventListener.getEvents());

                if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(eventListener.getImplementationType())) {
                    getEventSupport(bpmnParse.getBpmnModel()).addEventListener(bpmnParse.getListenerFactory().createClassDelegateEventListener(eventListener), types);

                } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(eventListener.getImplementationType())) {
                    getEventSupport(bpmnParse.getBpmnModel()).addEventListener(bpmnParse.getListenerFactory().createDelegateExpressionEventListener(eventListener), types);

                } else if (ImplementationType.IMPLEMENTATION_TYPE_THROW_SIGNAL_EVENT.equals(eventListener.getImplementationType())
                        || ImplementationType.IMPLEMENTATION_TYPE_THROW_GLOBAL_SIGNAL_EVENT.equals(eventListener.getImplementationType())
                        || ImplementationType.IMPLEMENTATION_TYPE_THROW_MESSAGE_EVENT.equals(eventListener.getImplementationType())
                        || ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT.equals(eventListener.getImplementationType())) {

                    getEventSupport(bpmnParse.getBpmnModel()).addEventListener(bpmnParse.getListenerFactory().createEventThrowingEventListener(eventListener), types);

                } else {
                    // 事件监听器不支持的实现类型：{}元素{}
                    LOGGER.warn("Unsupported implementation type for EventListener: {} for element {}", eventListener.getImplementationType(), bpmnParse.getCurrentFlowElement().getId());
                }
            }
        }

    }

    protected FlowableEventSupport getEventSupport(BpmnModel bpmnModel) {
        return (FlowableEventSupport) bpmnModel.getEventSupport();
    }

}
