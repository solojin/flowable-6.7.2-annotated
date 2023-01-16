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
package org.flowable.common.engine.impl.persistence;

import org.flowable.common.engine.impl.cfg.IdGenerator;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * ｛@link IdGenerator｝实现基于当前时间和它运行的机器的以太网地址。
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class StrongUuidGenerator implements IdGenerator {

    // 同一类加载器上的不同ProcessEngine共享一个生成器。
    protected static volatile TimeBasedGenerator timeBasedGenerator;

    public StrongUuidGenerator() {
        ensureGeneratorInitialized();
    }

    protected void ensureGeneratorInitialized() {
        if (timeBasedGenerator == null) {
            synchronized (StrongUuidGenerator.class) {
                if (timeBasedGenerator == null) {
                    timeBasedGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
                }
            }
        }
    }

    @Override
    public String getNextId() {
        return timeBasedGenerator.generate().toString();
    }

}
