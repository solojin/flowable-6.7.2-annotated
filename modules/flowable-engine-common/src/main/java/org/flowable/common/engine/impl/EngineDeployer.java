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

import java.util.Map;

import org.flowable.common.engine.api.repository.EngineDeployment;

/**
 * 引擎部署器，实现类有BPMN部署器BpmnDeployer，DMN部署器DmnDeployer，CMMN部署器CmmnDeployer，
 * 规则部署器RulesDeployer，事件部署器EventDeployer，表单部署器FormDeployer，应用部署器AppDeployer
 * @author Tijs Rademakers
 */
public interface EngineDeployer {


    void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings);
}
