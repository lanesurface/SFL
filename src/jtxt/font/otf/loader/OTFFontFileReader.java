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

import static jtxt.font.otf.loader.OTFDataType.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import jtxt.font.otf.loader.Table.TableDescriptor;

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
        
        public Object createCompatibleData() {
            return type.getDataAsJavaType(data);
        }
        
        public byte[] getRawData() {
            return data;
        }
    }
    
    /*
     * All of the tags which a table defined in the OTF specification may
     * take on. The name of the constant is the same as the hexadecimal
     * string which it defines.
     */
    public static final int BASE = 0x42_41_53_45,
                            CBDT = 0x43_42_44_54,
                            CBLT = 0x43_42_4C_54,
                            CFF  = 0x43_46_46_20,
                            CFF2 = 0x43_46_46_32,
                            COLR = 0x43_4F_4C_52,
                            CPAL = 0x43_50_50_4C,
                            DSIG = 0x44_53_49_47,
                            EBDT = 0x45_42_44_54,
                            EBLC = 0x45_42_4C_43,
                            EBSC = 0x45_42_53_43,
                            GDEF = 0x47_44_45_46,
                            GPOS = 0x47_50_4F_53,
                            GSUB = 0x47_53_55_42,
                            HVAR = 0x48_56_41_52,
                            JSTF = 0x4A_53_54_46,
                            LTSH = 0x4C_54_53_48,
                            MATH = 0x4D_41_54_48,
                            MERG = 0x4D_45_52_47,
                            MVAR = 0x4D_56_41_52,
                            OS_2 = 0x4F_53_5F_32,
                            avar = 0x61_76_61_72,
                            cmap = 0x63_6D_61_70,
                            cvar = 0x63_76_61_72,
                            cvt  = 0x63_76_74_20,
                            fpgm = 0x66_70_67_6D,
                            fvar = 0x66_76_61_72,
                            gasp = 0x67_61_73_70,
                            glyf = 0x67_6C_79_66,
                            gvar = 0x67_76_61_72,
                            hdmx = 0x68_64_6D_78,
                            head = 0x68_65_61_64,
                            hhea = 0x68_68_65_61,
                            hmtx = 0x68_6D_74_78,
                            kern = 0x6B_65_72_6E,
                            loca = 0x6C_6F_63_61,
                            maxp = 0x6D_61_78_70,
                            meta = 0x6D_65_74_61,
                            name = 0x6E_61_6D_65;
    
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
    private Map<Integer, Table> tables;
    
    public OTFFontFileReader(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY,
                                          0,
                                          raf.length());
            raf.close();
        }
        catch (IOException ioe) { /* ... */ }
        
        tables = new HashMap<>();
        /* sfntVersion */ buffer.getInt();
        int numTables = buffer.getShort();
        System.out.format("numTables=%d%n", numTables);
        
        TableDescriptor recordDesc = new TableDescriptor(TAG,
                                                         UINT32,
                                                         OFFSET32,
                                                         UINT32);
        for (int i = 0; i < numTables; i++) {
            buffer.position(recordDesc.getLength()
                            * i
                            + (4 + 2 * 4));
            
            int tag = (int)(buffer.getLong() >> 32 & 0xFFFFFFFF),
                offset = buffer.getInt(),
                length = buffer.getInt();
            
            System.out.println(getTagAsString(tag)
                               + "\noffset=" + offset
                               + "\nlength=" + length
                               + "\n---");
            
            addTableEntry(tag,
                          offset,
                          length);
        }
    }
    
    protected void addTableEntry(int tag,
                                 int tableOffset,
                                 int tableLength) {
        TableDescriptor descriptor;
        switch (tag) {
        case glyf:
            descriptor = new TableDescriptor(INT16,  // numberOfContours
                                             INT16,  // xMin
                                             INT16,  // yMin
                                             INT16,  // xMax
                                             INT16); // yMax
            break;
        case head:
            descriptor = new TableDescriptor(UINT16, // majorVersion
                                             UINT16, // minorVersion
                                             FIXED,  // fontRevision
                                             UINT32, // checkSumAdjustment
                                             UINT32, // magicNumber
                                             UINT16, // flags
                                             UINT16, // unitsPerEm
                                             LONGDATETIME, // created
                                             LONGDATETIME, // modified
                                             INT16,  // xMin
                                             INT16,  // yMin
                                             INT16,  // xMax
                                             INT16,  // yMax
                                             UINT16, // macStyle
                                             UINT16, // lowestRecPPEM
                                             INT16,  // fontDirectionHint
                                             INT16,  // indexToLocFormat
                                             INT16); // glyphDataFormat
            break;
        default:
            /* 
             * The tag isn't correct or the table type is not supported by this
             * font loader; either way, return silently.
             */
            return;
        }
        
        tables.put(tag, new Table(descriptor,
                                  tableOffset,
                                  tableLength));
    }
    
    public String getTagAsString(int tag) {
        byte[] bytes = { (byte)(tag >> 24 & 0xFF),
                         (byte)(tag >> 16 & 0xFF),
                         (byte)(tag >> 8 & 0xFF),
                         (byte)(tag & 0xFF) };
        
        return new String(bytes, Charset.forName("US-ASCII"));
    }
    
    public static class TableDNEx extends IllegalArgumentException { }
    
    public Table getTable(int tag) throws TableDNEx { return null; }
    
    public static void main(String[] args)
        throws URISyntaxException
    {   
        File file = new File(
            ClassLoader.getSystemResource("CALIBRI.TTF")
                       .toURI()
        );
        OTFFontFileReader otf = new OTFFontFileReader(file);
        /*
         * So we want to be able to easily convert between OTF types and Java
         * data types with the OTFDataType#getDataAsJavaType(byte[]) method.
         * 
         * Right now, this method returns -incorrect- values for the byte array
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
