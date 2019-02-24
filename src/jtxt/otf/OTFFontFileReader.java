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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import jtxt.otf.Table.OTFDataType;

import static jtxt.otf.Table.OTFDataType.*;

/**
 * 
 */
public class OTFFontFileReader {
    public static class OTFData {
        private final OTFDataType type;
        
        private final byte[] data;
        
        public OTFData(OTFDataType type, byte[] data) {
            this.type = type;
            this.data = data;
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
        
        Table offsetTable = readTable(0, Table.OFFSET_TABLE_DESC);
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
    
    public static void main(String[] args) throws FileNotFoundException,
                                                  IOException,
                                                  URISyntaxException { 
//        BufferedInputStream in = new BufferedInputStream(
//            new FileInputStream(ClassLoader.getSystemResource("calibri.ttf")
//                                           .getFile())
//        );
//        
//        String offset = "32,sfntVersion;"
//                        + "16,numTables;"
//                        + "16,searchRange;"
//                        + "16,entrySelector;"
//                        + "16,rangeShift;";
//        readTable(in, offset);
//        
//        String tableRecord = "32,tag;"
//                             + "32,checksum;"
//                             + "32,offset;"
//                             + "32,length;";
//        
//        int numTableRecords = 22;
//        for (int i = 0;
//             i < numTableRecords;
//             i++)
//        {
//            System.out.println("----- TABLE " + i + " -----");
//            readTable(in, tableRecord);
//        }
        
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
