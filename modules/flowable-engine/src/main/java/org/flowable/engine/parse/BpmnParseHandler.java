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
package org.flowable.engine.parse;

import java.util.Collection;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * BPMN解析处理器
 * 允许在解析BPMN 2.0进程期间连接到一个或多个元素的解析中。有关更多详细信息，请参阅关于bpmn解析处理程序的userguide部分。
 * 
 * 此类的实例可以注入{@link ProcessEngineConfigurationImpl}。然后，每当解析出与BPMN返回的类型匹配的BPMN 2.0元素时，就会调用该处理程序
 * {@link#getHandledTypes（）}方法。
 * 
 * @see AbstractBpmnParseHandler
 * 
 * @author Joram Barrez
 */
public interface BpmnParseHandler {

    /**
     * 在流程解析期间必须调用此处理程序的类型。
     */
    Collection<Class<? extends BaseElement>> getHandledTypes();

    /**
     * 实际的委派方法。解析器将在匹配{@link#getHandledTypes（）}返回值时调用此方法。
     * 
     * @param bpmnParse
     *            {@link BpmnParse}实例，充当解析过程中生成的所有内容的容器。
     */
    void parse(BpmnParse bpmnParse, BaseElement element);

}
