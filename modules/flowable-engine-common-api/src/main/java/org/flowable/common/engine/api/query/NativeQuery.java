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

import org.flowable.common.engine.api.FlowableException;

/**
 * 描述执行Native查询的基本方法
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface NativeQuery<T extends NativeQuery<?, ?>, U extends Object> {

    /**
     * 处理要执行的SQL语句。注意：如果你需要一个count，你必须自己使用一个count（）语句，否则结果将被视为Flowable实体的丢失。
     * 如果需要分页，则必须自己插入分页代码。我们跳过了这一步，因为这在某些数据库（尤其是MS-SQL/DB2）上做得非常不同
     * */
    T sql(String selectClause);

    /**
     * 添加要在索引查询中替换的参数，例如：param1，：myParam。。。
     */
    T parameter(String name, Object value);

    /** 执行查询并返回结果数 */
    long count();

    /**
     * 执行查询并返回结果实体，如果没有实体与查询条件匹配，则返回null。
     * 
     * @throws FlowableException
     *             当查询产生多个实体时。
     */
    U singleResult();

    /** 执行查询并获得实体列表作为结果。 */
    List<U> list();

    /** 执行查询并获得实体列表作为结果。 */
    List<U> listPage(int firstResult, int maxResults);
}
