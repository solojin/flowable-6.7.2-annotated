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
package org.flowable.engine.impl.jobexecutor;

import java.io.IOException;

import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 定时器事件处理器
 */
public class TimerEventHandler {

    // 限定固定名称，定时器活动ID的属性名称
    public static final String PROPERTYNAME_TIMER_ACTIVITY_ID = "activityId";
    // 限定固定名称，截止日期表达式的属性名称
    public static final String PROPERTYNAME_END_DATE_EXPRESSION = "timerEndDate";
    // 限定固定名称，日程表名称的属性名称
    public static final String PROPERTYNAME_CALENDAR_NAME_EXPRESSION = "calendarName";

    // 创建配置
    public static String createConfiguration(String id, String endDate, String calendarName) {
        // 实际是从命令上下文中获取到流程引擎配置
        ObjectNode cfgJson = createObjectNode();

        // 重设流程引擎配置中的，定时器活动ID、截止日期表达式、日程表名称
        cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, id);
        if (endDate != null) {
            cfgJson.put(PROPERTYNAME_END_DATE_EXPRESSION, endDate);
        }
        if (calendarName != null) {
            cfgJson.put(PROPERTYNAME_CALENDAR_NAME_EXPRESSION, calendarName);
        }
        return cfgJson.toString();
    }

    // 给配置器设置活动ID
    public static String setActivityIdToConfiguration(String jobHandlerConfiguration, String activityId) {
        try {
            ObjectNode cfgJson = readJsonValueAsObjectNode(jobHandlerConfiguration);
            cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, activityId);
            return cfgJson.toString();
        } catch (IOException ex) {
            return jobHandlerConfiguration;
        }
    }

    // 从配置器获取活动ID
    public static String getActivityIdFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode activityIdNode = cfgJson.get(PROPERTYNAME_TIMER_ACTIVITY_ID);
            if (activityIdNode != null) {
                return activityIdNode.asText();
            } else {
                return jobHandlerConfiguration;
            }
            
        } catch (IOException ex) {
            return jobHandlerConfiguration;
        }
    }

    // 从配置器获取日程表名称
    public static String getCalendarNameFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode calendarNameNode = cfgJson.get(PROPERTYNAME_CALENDAR_NAME_EXPRESSION);
            if (calendarNameNode != null) {
                return calendarNameNode.asText();
            } else {
                return "";
            }
            
        } catch (IOException ex) {
            // 未指定日程表名称
            return "";
        }
    }

    // 给配置器设置截止时间
    public static String setEndDateToConfiguration(String jobHandlerConfiguration, String endDate) {
        ObjectNode cfgJson = null;
        try {
            cfgJson = readJsonValueAsObjectNode(jobHandlerConfiguration);
        } catch (IOException ex) {
            // 创建json配置
            cfgJson = createObjectNode();
            cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, jobHandlerConfiguration);
        }
        
        if (endDate != null) {
            cfgJson.put(PROPERTYNAME_END_DATE_EXPRESSION, endDate);
        }

        return cfgJson.toString();
    }

    // 从配置器获取截止日期
    public static String getEndDateFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode endDateNode = cfgJson.get(PROPERTYNAME_END_DATE_EXPRESSION);
            if (endDateNode != null) {
                return endDateNode.asText();
            } else {
                return null;
            }
            
        } catch (IOException ex) {
            return null;
        }
    }

    // 创建对象节点
    protected static ObjectNode createObjectNode() {
        // 从命令上下文工具类中获取流程引擎配置类
        return CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().createObjectNode();
    }

    // 读取JSON值，转换为对象节点
    protected static ObjectNode readJsonValueAsObjectNode(String config) throws IOException {
        return (ObjectNode) readJsonValue(config);
    }

    // 读取JSON值
    protected static JsonNode readJsonValue(String config) throws IOException {
        // 避免空指针错误
        if (CommandContextUtil.getCommandContext() != null) {
            return CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().readTree(config);
        } else {
            return new ObjectMapper().readTree(config);
        }
    }

}
