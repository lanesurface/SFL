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
 * A {@code Table} contains information about a particular aspect of the font,
 * according to the tables which are outlined in the
 * <a href="https://docs.microsoft.com/en-us/typography/opentype/spec/">
 * OpenType specification</a>. An instance of this class does not need to
 * be contain a layout which is in that specification; however, all tables
 * which are defined by it can be loaded from an {@link OTFFontFileReader}.
 */
public class Table {
    public static class TableDescriptor {
        private OTFDataType[] layout;
        
        public TableDescriptor(OTFDataType... types) {
            this.layout = types;
        }
        
        public TableDescriptor add(OTFDataType... types) {
            OTFDataType[] layout = new OTFDataType[this.layout.length
                                                   + types.length];
            System.arraycopy(this.layout, 0, layout, 0, this.layout.length);
            System.arraycopy(types,
                             0,
                             layout,
                             this.layout.length,
                             types.length);
            
            return new TableDescriptor(layout);
        }
        
        public int getNumTypes() {
            return layout.length;
        }
        
        public int getLength() {
            int len = 0;
            for (OTFDataType type : layout)
                len += type.getNumBytes();
            
            return len;
        }
        
        public OTFDataType getDataTypeAt(int index) {
            return layout[index];
        }
        
        @Override
        public String toString() {
            return "TableDescriptor: " + Arrays.toString(layout);
        }
    }
    
    /**
     * The descriptor describes how the data within this table has been laid
     * out. When the table is loaded into memory, the data is constructed
     * from this information.
     */
    public final TableDescriptor descriptor;
    
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
    
    public int getOffsetAt(int index) {
        // Return the offset in memory for the table entry at the given index.
        int length = descriptor.getLength();
        if (index >= length || index < 0)
            throw new IndexOutOfBoundsException("The index must be a value in "
                                                + "the range: [0, "
                                                + length
                                                + "].");
        
        int offset = this.offset;
        for (int i = 0; i < index; i++)
            offset += descriptor.getDataTypeAt(i)
                                .getNumBytes();
        
        return offset;
    }
    
    @Override
    public String toString() {
        return String.format("offset=%d,%nlength=%d%n---",
                             offset,
                             length);
    }
}
