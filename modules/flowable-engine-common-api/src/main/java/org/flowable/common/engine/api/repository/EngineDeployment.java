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
package org.flowable.common.engine.api.repository;

import java.util.Date;
import java.util.Map;

/**
 * 表示引擎存储库中已存在的部署。
 * 
 * 部署是资源的容器，如流程定义、案例定义、图像、表单等。
 * 
 * @author Tijs Rademakers
 */
public interface EngineDeployment {

    String getId();

    String getName();

    Date getDeploymentTime();

    String getCategory();

    String getKey();
    
    String getDerivedFrom();

    String getDerivedFromRoot();

    String getTenantId();
    
    String getEngineVersion();
    
    boolean isNew();

    Map<String, EngineResource> getResources();
}
