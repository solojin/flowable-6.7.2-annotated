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
package org.flowable.common.engine.impl.util.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class InputStreamSource implements StreamSource {

    // This class is used for bpmn parsing.
    // The bpmn parsers needs to go over the stream at least twice:
    // Once for the schema validation and once for the parsing itself.
    // So we keep the content of the inputstream in memory so we can
    // re-read it.

    // 此类用于bpmn解析。
    // bpmn解析器需要至少对流进行两次检查：
    // 一次用于模式验证，一次用于解析本身。
    // 因此，我们将输入流的内容保存在内存中，以便重读一遍

    protected BufferedInputStream inputStream;
    protected byte[] bytes;

    public InputStreamSource(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream);
    }

    @Override
    public InputStream getInputStream() {
        if (bytes == null) {
            try {
                bytes = getBytesFromInputStream(inputStream);
            } catch (IOException e) {
                throw new FlowableException("Could not read from inputstream", e);
            }
        }
        return new BufferedInputStream(new ByteArrayInputStream(bytes));
    }

    @Override
    public String toString() {
        return "InputStream";
    }

    public byte[] getBytesFromInputStream(InputStream inStream) throws IOException {
        long length = inStream.available();
        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = inStream.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new FlowableException("Could not completely read inputstream ");
        }

        // Close the input stream and return bytes
        // 关闭输入流并返回字节
        inStream.close();
        return bytes;
    }

}
