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
package org.flowable.common.engine.impl;

/**
 * 此类的实现可以插入{@link AbstractEngineConfiguration}。这样的实现可以以任何编程方式配置引擎。
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface EngineConfigurator {

    /**
     * 在完成任何初始化之前调用<b>。例如，这对于在创建使用这些属性的任何内容之前更改配置设置很有用。
     * 
     * 允许通过传递{@link AbstractEngineConfiguration}来调整引擎，该参数允许以编程方式调整引擎。
     * 
     * 例如，jdbc url。当{@link EngineConfigurator}实例想要更改它时，它需要使用此方法，否则数据源就已经创建了
     * 带有jdbc url的“old”值。
     */
    void beforeInit(AbstractEngineConfiguration engineConfiguration);

    /**
     * 当引擎启动时，在可用之前，但在内部对象初始化完成之后调用。
     * 
     * 允许通过传递{@link AbstractEngineConfiguration}来调整引擎，该参数允许以编程方式调整引擎。
     * 
     * ldap用户/组管理器就是一个例子，它是对引擎的一个补充。无需为此重写默认属性（否则应该使用
     * {@link#beforeInit（AbstractEngineConfiguration）}方法），因此此方法中包含的逻辑在初始化默认对象后执行。
     *
     * 
     * 也许更好的名称应该是'afterInit'（cfr{@link#beforeInit（AbstractEngineConfiguration）}），但由于向后兼容，这是不可能的。
     */
    void configure(AbstractEngineConfiguration engineConfiguration);

    /**
     * 使用{@link EngineConfigurator}实例时，它们首先按此优先级编号排序（从低到高）。如果您在
     * {@link EngineConfigurator}实例间有依赖，根据需要使用相应的优先级对其进行排序。
     */
    int getPriority();

}
