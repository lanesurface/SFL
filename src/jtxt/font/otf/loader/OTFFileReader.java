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

import jtxt.font.otf.CharacterMapper;
import jtxt.font.otf.OpenTypeFont;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 */
public class OTFFileReader {
    /**
     * Contains information about a table in the font file. Namely, this class
     * aids in mapping table tags to their location in memory.
     */
    static final class TableRecord {
        public final int offset;
        public final int length;
        
        public TableRecord(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }
    
    private static class DataConverter {
        public static String getTagAsString(int tag) {
            byte[] bytes = { (byte)(tag >> 24 & 0xFF),
                             (byte)(tag >> 16 & 0xFF),
                             (byte)(tag >> 8 & 0xFF),
                             (byte)(tag & 0xFF) };
            
            return new String(bytes, Charset.forName("US-ASCII"));
        }

        public static float[] getF2Dot14(ByteBuffer source,
                                         int offset,
                                         int count) {
            int bytesToRead = 2 * count;
            byte[] data = new byte[bytesToRead];
            source.get(data,
                       offset,
                       bytesToRead);

            float[] nums = new float[count];
            for (int n = 0; n < count; n++) {
                int d1i = 2 * n,
                        d2i = d1i + 1;
                float sum = data[d1i] >> 6;
                short decimal = (short)((data[d1i] & 0x3F)
                        << 8
                        ^ data[d2i]
                        & 0xFF);
                for (int b = 1; b <= 14; b++)
                    sum += ((decimal & 1 << 14 - b)
                            >> 14
                            - b) / Math.pow(2, b);

                nums[n] = sum;
            }

            return nums;
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
                            STAT = 0x53_54_41_54,
                            SVG  = 0x53_56_47_20,
                            VDMX = 0x56_44_4D_58,
                            VORG = 0x56_4F_52_47,
                            VVAR = 0x56_56_41_52,
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
                            name = 0x6E_61_6D_65,
                            pclt = 0x70_63_6C_74,
                            post = 0x70_6F_73_74,
                            prep = 0x70_72_65_70,
                            sbix = 0x73_62_69_78,
                            vhea = 0x76_68_65_61,
                            vmtx = 0x76_6D_74_78;
    
    private static final int TABLE_RECORD_OFFSET = 4 + 2 * 4;
    
    /**
     * The buffer which contains all of the data that this font holds. This
     * data is transformed into a more suitable format and packaged into
     * {@code Table}s. These tables can be looked up with a {@code Tag},
     * which serves as their unique ID.
     */
    private ByteBuffer buffer;
    
    /**
     * All of the tags which are in this font. Each tag corresponds to a
     * respective {@code Table} in memory; a tag associates a specific Table's
     * offset with a four-byte ASCII identifier.
     */
    private Map<Integer, TableRecord> tables;
    
    public OTFFileReader(File file) {
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
        mapTableRecords(TABLE_RECORD_OFFSET,
                        numTables);
        
        byte[] f = new byte[] { (byte)0x70,
                                (byte)0x00,
                                (byte)0x7f,
                                (byte)0xff,
                                (byte)0xff,
                                (byte)0xff };
        float[] nums = DataConverter.getF2Dot14(ByteBuffer.wrap(f),
                                                0,
                                                3);
        System.out.println(Arrays.toString(nums));
        
        createCharacterMapper();
    }
    
    public CharacterMapper createCharacterMapper() {
        /* 
         * TODO: Return a character mapper which supports the characteristics
         *       of this font. (For example, a different character mapper will
         *       need to be constructed if this font supports features and the
         *       client requests that they be enabled.)
         */
        
        return new DefaultOTCMap(buffer,
                                 tables.get(cmap).offset);
    }
    
    private TableRecord[] mapTableRecords(int offset, int numTables) {
        TableRecord[] recordEntries = new TableRecord[numTables];
        
        buffer.position(offset);
        for (int i = 0; i < numTables; i++) {
            /*
             * TAG      tag
             * UINT32   checksum
             * OFFSET32 offset
             * UINT32   length 
             */
            int tag = (int)(buffer.getLong()
                            >> 32
                            & 0xFFFFFFFF);

            tables.put(tag, new TableRecord(buffer.getInt(),
                                            buffer.getInt()));
        }
        
        return recordEntries;
    }
    
    public static void main(String[] args)
        throws URISyntaxException
    {   
        File file = new File(
            ClassLoader.getSystemResource("CALIBRI.TTF")
                       .toURI()
        );
        OTFFileReader otf = new OTFFileReader(file);
        
        String entries= otf.tables
                           .keySet()
                           .stream()
                           .map(DataConverter::getTagAsString)
                           .collect(Collectors.joining(", ",
                                                       "Tables: <",
                                                       ">"));
        System.out.println(entries);
    }
}
