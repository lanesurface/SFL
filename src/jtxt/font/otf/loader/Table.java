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
    
    private OTFData[] data;
    
    /* package-private */ Table(TableDescriptor descriptor,
                                int offset,
                                int length) {
        this.descriptor = descriptor;
        this.offset = offset;
        this.length = length;
    }
    
    public Table load(ByteBuffer buffer) {
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
        
        this.data = data;
        
        return this;
    }
    
    public int getOffsetAt(int index) {
        // Return the offset in memory for the table entry at the given index.
        int length = descriptor.getLength();
        if (index >= length || index < 0)
            throw new IndexOutOfBoundsException("The index must be a value in "
                                                + "the range: [0, "
                                                + length
                                                + ").");
        
        int offset = this.offset;
        for (int i = 0; i < index; i++)
            offset += descriptor.getDataTypeAt(i)
                                .getNumBytes();
        
        return offset;
    }
    
    /**
     * <b>
     * Note: The data for this table must be loaded before it can be read
     * from memory.
     * </b>
     * 
     * Read the specified number of bytes into the given byte array.
     * 
     * @param start The offset into this table which should be used to
     *              determine the first byte that will be read into the array.
     * @param end The last byte which should be read into the array.
     * @param dest The array that these bytes will be stored within. The size
     *             of the array doesn't matter; a new array with the same size
     *             will be allocated anyway. This value may be null if this
     *             method appears on the right-hand side of an assignment. The
     *             length of the returned array will be
     *             <code>start - end</code>.
     *
     * @return A byte array which is identical to <code>dest</code>, unless
     *         dest is null.
     * 
     * @see Table#load(ByteBuffer)
     */
    public byte[] readIntoArray(int start,
                                int end,
                                byte[] dest) {
        if (data == null) throw new IllegalStateException("The data for this "
                                                          + "table must be "
                                                          + "loaded before it "
                                                          + "can be read.");
        
        dest = new byte[length];
        
        for (int i = start; i < dest.length; i++)
            for (int li = 0;
                li < data[i].type.getNumBytes();
                li++) dest[i + li] = data[i].data[li];
        
        return dest;
    }
    
    @Override
    public String toString() {
        return String.format("offset=%d,%nlength=%d%n---",
                             offset,
                             length);
    }
}
