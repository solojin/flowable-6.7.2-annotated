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
package org.flowable.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.repository.Deployment;

/**
 * @author Joram Barrez
 */
public interface DeploymentEntityManager extends EntityManager<DeploymentEntity> {

    // 根据生命周期查询部署
    List<Deployment> findDeploymentsByQueryCriteria(DeploymentQueryImpl deploymentQuery);

    // 获取部署资源名称
    List<String> getDeploymentResourceNames(String deploymentId);

    // 根据本地方法查询部署
    List<Deployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap);

    // 根据本地方法查询部署数量
    long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap);

    // 根据生命周期查询部署数量
    long findDeploymentCountByQueryCriteria(DeploymentQueryImpl deploymentQuery);

    // 移除部署
    void deleteDeployment(String deploymentId, boolean cascade);

}