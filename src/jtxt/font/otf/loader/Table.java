/* 
 * Copyright 2019 Lane W. Surface
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jtxt.font.otf.loader;

import java.nio.ByteBuffer;
import java.util.Arrays;

import jtxt.font.otf.loader.OTFFontFileReader.OTFData;

/**
 * 
 */
public class Table {
    public static class TableDescriptor {
        private OTFDataType[] types;
        
        public TableDescriptor(OTFDataType... types) {
            this.types = types;
        }
        
        public int getNumTypes() {
            return types.length;
        }
        
        public int getLength() {
            int len = 0;
            for (OTFDataType type : types)
                len += type.getNumBytes();
            
            return len;
        }
        
        public OTFDataType getDataTypeAt(int index) {
            return types[index];
        }
        
        @Override
        public String toString() {
            return "TableDescriptor: " + Arrays.toString(types);
        }
    }
    
    public TableDescriptor descriptor;
    
    private final int offset,
                      length;
    
    /* package-private */ Table(TableDescriptor descriptor,
                                int offset,
                                int length) {
        this.descriptor = descriptor;
        this.offset = offset;
        this.length = length;
    }
    
    public OTFData[] load(ByteBuffer buffer) {
        int prev = buffer.position();
        buffer.position(offset);
        
        int length = descriptor.getNumTypes(); /* Is this the same as what the
                                                  Table will be initialized
                                                  to? */
        OTFData[] data = new OTFData[length];
        for (int i = 0; i < length; i++) {
            OTFDataType type = descriptor.getDataTypeAt(i);
            byte[] bytes = new byte[type.getNumBytes()];
            buffer.get(bytes).position(prev);
            
            data[i] = new OTFData(type,
                                  bytes);
        }
        
        return data;
    }
    
    @Override
    public String toString() {
        return String.format("offset=%d,%nlength=%d%n---",
                             offset,
                             length);
    }
    
    /* 
     * TableDescriptor glyfDescriptor = new TableDescriptor(INT16,
     *                                                      INT16,
     *                                                      INT16,
     *                                                      INT16,
     *                                                      INT16);
     * Table glyfTable = new Table(glyfDescriptor, offset, length);
     * ...
     * Table table = tables.get(tag);
     * OTFData[] data = table.load(buffer);
     * 
     * for (int i = 0; i < data.length; i++)
     *     table.descriptor.getDataTypeAt(i)
     *                     .createCompatibleJavaType(data.getRawData());
     */
}
