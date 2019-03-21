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

import static jtxt.font.otf.CharacterMapper.PLATFORM_WINDOWS;
import static jtxt.font.otf.CharacterMapper.PLATFORM_WINDOWS_UNICODE_BMP;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import jtxt.font.otf.CharacterMapper;

/**
 * 
 */
public class OTFFileReader {
    /**
     * Contains information about a table in the font file. Namely, this class
     * aids in mapping table tags to their location in memory.
     */
    static final class TableRecord {
        final int offset;
        final int length;
        
        public TableRecord(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }
    
    /* package-private */ static class DataConverter {
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
    
    static class Table implements Iterable<Number> {
        enum Type {
            UINT8(8),
            INT8(8),
            UINT16(16),
            INT16(16),
            UINT24(24),
            UINT32(32),
            INT32(32),
            FIXED(32),
            FWORD(16),
            UFWORD(16),
            F2DOT14(16),
            LONGDATETIME(64),
            TAG(32),
            OFFSET16(16),
            OFFSET32(32);
            
            final int size;
            
            Type(int size) {
                this.size = size;
            }
        }
        
        private ByteBuffer buffer;
        private int offset;
        private Type[] types;
        
        Table(ByteBuffer buffer,
              int offset,
              Type... types) {
            this.buffer = buffer;
            this.offset = offset;
            this.types = types;
        }
        
        @Override
        public Iterator<Number> iterator() {
            return new Iterator<Number>() {
                private int i,
                            lo;
                
                @Override
                public boolean hasNext() {
                    return i < types.length;
                }
                
                @Override
                public Number next() {
                    Type type = types[i++];
                    int len = type.size / 8;
                    byte[] bytes = new byte[len];
                    buffer.position(offset + lo);
                    buffer.get(bytes);
                    lo += len;
                    
                    switch (type) {
                    case UINT8:
                    case INT8:
                        return new Byte(bytes[0]);
                    case UINT16:
                    case INT16:
                        return new Short((short)(bytes[0]
                                                 << 8
                                                 ^ bytes[1]));
                    case FIXED:
                    case UINT32:
                    case INT32:
                        return new Integer((int)(bytes[0]
                                                 << 24
                                                 ^ bytes[1]
                                                 << 16
                                                 ^ bytes[2]
                                                 << 8
                                                 ^ bytes[3]));
                    case LONGDATETIME: // NOT proper repr.
                        return new Long((long)(bytes[0]
                                               << 56
                                               ^ bytes[1]
                                               << 48
                                               ^ bytes[2]
                                               << 40
                                               ^ bytes[3]
                                               << 32
                                               ^ bytes[4]
                                               << 24
                                               ^ bytes[5]
                                               << 16
                                               ^ bytes[6]
                                               << 8
                                               ^ bytes[7]));
                    default:
                        return null;
                    }
                }
            };
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
        
        CharacterMapper mapper = createCharacterMapper();
        int offset = tables.get(glyf).offset
                     + mapper.getGlyphOffset('B', CharacterMapper.NO_FEATURES);
        Glyph.SimpleGlyph glyph = new Glyph.SimpleGlyph(buffer,
                                                        offset,
                                                        12,
                                                        -1);
    }
    
    public CharacterMapper createCharacterMapper() {
        /* 
         * TODO: Return a character mapper which supports the characteristics
         *       of this font. (For example, a different character mapper will
         *       need to be constructed if this font supports features and the
         *       client requests that they be enabled.)
         */
        int offset = tables.get(cmap).offset,
            locaOffset = tables.get(loca).offset;
        
        Table table = new Table(buffer,
                                tables.get(head).offset,
                                Table.Type.UINT16,
                                Table.Type.UINT16,
                                Table.Type.FIXED, // BAD
                                Table.Type.UINT32,
                                Table.Type.UINT32,
                                Table.Type.UINT16,
                                Table.Type.UINT16, // Units/EM
                                Table.Type.LONGDATETIME,
                                Table.Type.LONGDATETIME,
                                Table.Type.INT16, // xMin
                                Table.Type.INT16, // yMin
                                Table.Type.INT16, // xMax
                                Table.Type.INT16, // yMax
                                Table.Type.UINT16,
                                Table.Type.UINT16,
                                Table.Type.INT16,
                                Table.Type.INT16,
                                Table.Type.INT16);
        System.out.println("--- TABLE START ---");
        table.forEach(System.out::println);
        System.out.println("--- TABLE END ---");
        
        boolean useLongAddresses = buffer.getShort(tables.get(head).offset
                                                   + 50) == 0 ? false : true;
        int numGlyphs = buffer.getShort(tables.get(maxp).offset + 4);
        
        GlyphLocator locator = GlyphLocator.getInstance(buffer,
                                                        locaOffset,
                                                        numGlyphs,
                                                        useLongAddresses);
        
        return new DefaultOTCMap(buffer.duplicate(),
                                 offset,
                                 PLATFORM_WINDOWS,
                                 PLATFORM_WINDOWS_UNICODE_BMP,
                                 locator);
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
        
        String entries = otf.tables
                            .keySet()
                            .stream()
                            .map(DataConverter::getTagAsString)
                            .collect(Collectors.joining(", ",
                                                        "Tables: <",
                                                        ">"));
        System.out.println(entries);
    }
}
