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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 任务活动行为类
 *
 * 所有BPMN 2.0任务类型（如ServiceTask、ScriptTask、UserTask等）的父类。
 * 
 * 单独使用时，它的行为就像一个传递活动。
 * 
 * @author Joram Barrez
 */
public class TaskActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected List<String> getActiveValueList(List<String> originalValues, String propertyName, ObjectNode taskElementProperties) {
        List<String> activeValues = originalValues;
        if (taskElementProperties != null) {
            JsonNode overrideValuesNode = taskElementProperties.get(propertyName);
            if (overrideValuesNode != null) {
                if (overrideValuesNode.isNull() || !overrideValuesNode.isArray() || overrideValuesNode.size() == 0) {
                    activeValues = null;
                } else {
                    activeValues = new ArrayList<>();
                    for (JsonNode valueNode : overrideValuesNode) {
                        activeValues.add(valueNode.asText());
                    }
                }
            }
        }
        return activeValues;
    }
}
