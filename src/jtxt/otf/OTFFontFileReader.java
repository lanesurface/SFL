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
package jtxt.otf;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;

import static jtxt.otf.OTFDataType.*;

/**
 * 
 */
public class OTFFontFileReader {
    /**
     * 
     */
    public static class OTFData {
        private final OTFDataType type;
        
        private final byte[] data;
        
        public OTFData(OTFDataType type, byte[] data) {
            this.type = type;
            this.data = data;
        }
        
        public OTFDataType getDataType() {
            return type;
        }
        
        public byte[] getRawData() {
            return data;
        }
    }
    
    /**
     * The name given to this font. This is used internally and has no bearing
     * on how the font is loaded.
     */
    private String name;
    
    private RandomAccessFile raf;
    
    /**
     * The buffer which contains all of the data that this font holds. This
     * data is transformed into a more suitable format and packaged into
     * {@code Table}s. These tables can be looked up with a {@code Tag},
     * which serves as a unique ID.
     */
    private ByteBuffer buffer;
    
    /**
     * All of the tags which are in this font. Each tag corresponds to a
     * respective {@code Table} in memory; a tag associates a specific Table's
     * offset with a four-byte ASCII identifier.
     */
    private List<Tag> tags;
    
    public OTFFontFileReader(File file,
                             String name) {
        this.name = name;
        try {
            raf = new RandomAccessFile(file, "r");
            buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY,
                                          0,
                                          raf.length());
        }
        catch (IOException ioe) { /* ... */ }
        
        Table offsetTable = readTable(0, UINT32,   // sfntVersion
                                         UINT16,   // numTables
                                         UINT16,   // searchRange
                                         UINT16,   // entrySelector
                                         UINT16 ); // rangeShift
        
        int version = (int)offsetTable.getData(0);
        System.out.format("sfntVersion=%d%n", version);
        
        int numTables = (int)offsetTable.getData(1);
        System.out.format("numTables=%d%n", numTables);
        
        printTables(numTables);
    }
    
    public void printTables(int numTables) {
        // Print the table record entries.
        for (int i = 0; i < numTables; i++) {
            Table record = readTable(i * 4 * 4 + (4 + 2 * 4), TAG,
                                                              UINT32,
                                                              OFFSET32,
                                                              UINT32);
            System.out.println("--- TABLE " + i + " ---");
            String tag = new String(record.getOTFData(0).data,
                                    Charset.forName("US-ASCII"));
            System.out.println(tag);
            System.out.format("checksum=%d,%noffset=%d,%nlength=%d%n",
                              (int)record.getData(1),
                              (int)record.getData(2),
                              (int)record.getData(3));
        }
    }
    
    protected Table readTable(int offset, OTFDataType... types) {
        buffer.position(offset);
        
        OTFData[] data = new OTFData[types.length];
        for (int i = 0; i < types.length; i++) {
            OTFDataType type = types[i];
            byte[] bytes = new byte[type.getNumBytes()];
            buffer.get(bytes);
            
            data[i] = new OTFData(type,
                                  bytes);
        }
        
        return new Table(data);
    }
    
    public static class Tag { }
    
    public Table getTable(Tag tag) { return null; }
    
    public static void main(String[] args)
        throws URISyntaxException
    {   
        File file = new File(
            ClassLoader.getSystemResource("CALIBRI.TTF")
                       .toURI()
        );
        OTFFontFileReader otf = new OTFFontFileReader(file,
                                                      "Calibri");
        /*
         * So we want to be able to easily convert between OTF types and Java
         * data types with the OTFDataType#getDataAsJavaType(byte[]) method.
         * 
         * Right now, this method returns incorrect values for the byte array
         * and doesn't support all data types which have been defined in this
         * enumeration.
         * 
         *  1. I need to use constant class bodies to override this method,
         *     and have the default behavior be something reasonable (probably
         *     related to byte[] -> int conversion).
         *  2. Fix the problem with bytes in Java? being a different size than
         *     their size in the OTF file.
         */
        int i = (int)INT32.getDataAsJavaType(new byte[] { 0,
                                                          0,
                                                          (byte)56,
                                                          (byte)128 });
        System.out.println("i=" + i);
    }
}
