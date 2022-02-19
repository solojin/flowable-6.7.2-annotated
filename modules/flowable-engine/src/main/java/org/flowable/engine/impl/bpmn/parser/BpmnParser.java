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

import org.flowable.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.flowable.engine.impl.cfg.BpmnParseFactory;

/**
 * BPMN 2.0流程模型的解析器。
 * 
 * 流程引擎中只有一个该解析器的实例。这个{@link BpmnParser}创建了{@link BpmnParse}实例，可用于实际解析BPMN 2.0 XML流程定义。
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class BpmnParser {

    /**
     * BPMN 2.0图交换元素的名称空间。
     */
    public static final String BPMN_DI_NS = "http://www.omg.org/spec/BPMN/20100524/DI";

    /**
     * BPMN 2.0图表公共元素的名称空间。
     */
    public static final String BPMN_DC_NS = "http://www.omg.org/spec/DD/20100524/DC";

    /**
     * 通用OMG DI元素的名称空间（不要问我为什么不使用BPMN_DI_NS…）
     */
    public static final String OMG_DI_NS = "http://www.omg.org/spec/DD/20100524/DI";

    protected ActivityBehaviorFactory activityBehaviorFactory;
    protected ListenerFactory listenerFactory;
    protected BpmnParseFactory bpmnParseFactory;
    protected BpmnParseHandlers bpmnParserHandlers;

    /**
     * 创建一个新的{@link BpmnParse}实例，该实例只能用于解析一个BPMN 2.0进程定义。
     */
    public BpmnParse createParse() {
        return bpmnParseFactory.createBpmnParse(this);
    }

    public ActivityBehaviorFactory getActivityBehaviorFactory() {
        return activityBehaviorFactory;
    }

    public void setActivityBehaviorFactory(ActivityBehaviorFactory activityBehaviorFactory) {
        this.activityBehaviorFactory = activityBehaviorFactory;
    }

    public ListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    public void setListenerFactory(ListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
    }

    public BpmnParseFactory getBpmnParseFactory() {
        return bpmnParseFactory;
    }

    public void setBpmnParseFactory(BpmnParseFactory bpmnParseFactory) {
        this.bpmnParseFactory = bpmnParseFactory;
    }

    public BpmnParseHandlers getBpmnParserHandlers() {
        return bpmnParserHandlers;
    }

    public void setBpmnParserHandlers(BpmnParseHandlers bpmnParserHandlers) {
        this.bpmnParserHandlers = bpmnParserHandlers;
    }
}
