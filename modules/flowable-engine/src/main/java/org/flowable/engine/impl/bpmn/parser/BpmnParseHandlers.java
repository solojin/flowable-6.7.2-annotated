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
package org.flowable.engine.impl.bpmn.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.DataObject;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.parse.BpmnParseHandler;
import org.slf4j.Logger;

/**
 * BPMN解析处理器
 *
 * @author Joram Barrez
 */
public class BpmnParseHandlers {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BpmnParseHandlers.class);

    protected Map<Class<? extends BaseElement>, List<BpmnParseHandler>> parseHandlers;

    public BpmnParseHandlers() {
        this.parseHandlers = new HashMap<>();
    }

    public List<BpmnParseHandler> getHandlersFor(Class<? extends BaseElement> clazz) {
        return parseHandlers.get(clazz);
    }

    // 批量添加BPMN处理器
    public void addHandlers(List<BpmnParseHandler> bpmnParseHandlers) {
        for (BpmnParseHandler bpmnParseHandler : bpmnParseHandlers) {
            addHandler(bpmnParseHandler);
        }
    }

    // 添加BPMN处理器
    public void addHandler(BpmnParseHandler bpmnParseHandler) {
        for (Class<? extends BaseElement> type : bpmnParseHandler.getHandledTypes()) {
            List<BpmnParseHandler> handlers = parseHandlers.get(type);
            if (handlers == null) {
                handlers = new ArrayList<>();
                parseHandlers.put(type, handlers);
            }
            handlers.add(bpmnParseHandler);
        }
    }

    // 解析元素
    public void parseElement(BpmnParse bpmnParse, BaseElement element) {

        if (element instanceof DataObject) {
            // 忽略DataObject元素，因为它们是在进程上处理的
            // 和子流程级别
            return;
        }

        // 如果是流元素，设置为BPMN解析器当前正处理的流元素
        if (element instanceof FlowElement) {
            bpmnParse.setCurrentFlowElement((FlowElement) element);
        }

        // 执行解析处理程序
        List<BpmnParseHandler> handlers = parseHandlers.get(element.getClass());

        if (handlers == null) {
            // 找不到与{elementId}匹配的分析处理程序。这可能是一个BUG。
            LOGGER.warn("Could not find matching parse handler for + {} this is likely a bug.", element.getId());
        } else {
            for (BpmnParseHandler handler : handlers) {
                handler.parse(bpmnParse, element);
            }
        }
    }

}
