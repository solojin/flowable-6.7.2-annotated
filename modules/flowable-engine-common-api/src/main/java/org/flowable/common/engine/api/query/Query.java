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

package org.flowable.common.engine.api.query;

import java.util.List;

/**
 * 描述查询的基本方法
 *
 * @author Frederik Heremans
 */
public interface Query<T extends Query<?, ?>, U extends Object> {

    enum NullHandlingOnOrder {
        NULLS_FIRST, NULLS_LAST
    }

    /**
     * 根据该类中定义的给定属性对结果进行升序排序（需要在调用orderByXxxx方法之一之后）。
     */
    T asc();

    /**
     * 将结果按该类中定义的给定属性降序排列（需要在调用orderByXxxx方法之一之后）。
     */
    T desc();

    T orderBy(QueryProperty property);

    T orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder);

    /**
     * 执行查询并返回结果数
     */
    long count();

    /**
     * 执行查询并返回结果实体，如果没有实体与查询条件匹配，则返回null。
     *
     * @throws org.flowable.common.engine.api.FlowableException 当查询产生多个实体时。
     */
    U singleResult();

    /**
     * 执行查询并获得实体列表作为结果。
     */
    List<U> list();

    /**
     * 执行查询并获得实体列表作为结果。
     */
    List<U> listPage(int firstResult, int maxResults);
}
