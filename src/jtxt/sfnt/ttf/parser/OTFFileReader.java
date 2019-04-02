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
package jtxt.sfnt.ttf.parser;

import static jtxt.sfnt.ttf.CharacterMapper.PLATFORM_WINDOWS;
import static jtxt.sfnt.ttf.CharacterMapper.PLATFORM_WINDOWS_UNICODE_BMP;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import jtxt.sfnt.ttf.CharacterMapper;

/**
 * 
 */
public class OTFFileReader {
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
    
    /**
     * The buffer which contains all of the data that this font holds. This
     * data is transformed into a more suitable format and packaged into
     * {@code Table}s. These tables can be looked up with a {@code Tag},
     * which serves as their unique ID.
     */
    private ByteBuffer buffer;
    private Map<Integer, Integer> tables;
    private CharacterMapper cmapper;
    private final short unitsPerEm,
                        flags,
                        locaFormat,
                        xMin,
                        yMin,
                        xMax,
                        yMax;
    private final int glyphOffset;
    
    /**
     * Creates a new instance of a font file reader. The file which is passed
     * to this constructor must contain valid data, according to the OpenType
     * Font Specification as defined by Microsoft and Adobe.
     */
    public OTFFileReader(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY,
                                          0,
                                          raf.length());
            raf.close();
        }
        catch (IOException ioe) {
            /*
             * A general IOException has been thrown, which indicates that the
             * file is not on disk or could not be read.
             */
            throw new IllegalArgumentException("The provided file could not "
                                               + "be read from disk.");
        }
        
        tables = new HashMap<>();
        /* sfntVersion */ buffer.getInt();
        int numTables = buffer.getShort();
        
        buffer.position(4 + 2 * 4);
        for (int i = 0; i < numTables; i++) {
            /*
             * TAG      tag
             * UINT32   checksum
             * OFFSET32 offset
             * UINT32   length 
             */
            int tag = buffer.getInt();
            /* checksum */ buffer.getInt();
            
            tables.put(tag, (int)(buffer.getLong()
                                  >> 32
                                  & 0xFFFFFFFF));
        }
        
        int hoff = tables.get(head);
        unitsPerEm = buffer.getShort(hoff + 18);
        locaFormat = buffer.getShort(hoff + 50);
        glyphOffset = tables.get(glyf);
        
        // TODO: Initialize these variables.
        flags = xMin
              = yMin
              = xMax
              = yMax
              = 0;
        
        cmapper = createCharacterMapper(PLATFORM_WINDOWS,
                                        PLATFORM_WINDOWS_UNICODE_BMP);
    }
    
    /**
     * Locates, constructs, and scales the {@code Glyph} for the given
     * character. The value of this character is interpreted in the format
     * specified (and this is usually UTF-8), using the most significant bits
     * of this value if not all are required.
     * 
     * @param character The value of the character which this Glyph should be
     *                  created for.
     * @param dpi The dots-per-inch (resolution) of the output device that the
     *            Glyph will be scaled for. Displaying the returned Glyph on
     *            any device different from the one specified will cause its
     *            size to be skewed.
     * @param features Features according to those defined by the Feature
     *                 interface used for {@code Glyph} location and other
     *                 glyph-specific and optional operations. This should be
     *                 zero for now.
     * 
     * @return A {@code Glyph} which represents the value of the given
     *         character.
     */
    public Glyph getGlyph(char character,
                          int dpi,
                          int size,
                          int features) {
        int offset = glyphOffset + cmapper.getGlyphOffset(character,
                                                          features);
        
        /*
         * For now, return a SimpleGlyph in all cases. (This sometimes causes
         * OutOfBoundsExceptions to be thrown when the requested Glyph is
         * composite--in the future, this should be a call to a static factory
         * method.
         */
        return new Glyph.SimpleGlyph(buffer.duplicate(),
                                     offset,
                                     dpi,
                                     unitsPerEm,
                                     size);
    }
    
    public Metrics getMetrics(int ptSize,
                              int dpi) {
        return new Metrics(this,
                           ptSize,
                           unitsPerEm,
                           dpi);
    }
    
    public CharacterMapper createCharacterMapper(int platform,
                                                 int format) {
        int offset = tables.get(cmap),
            locaOffset = tables.get(loca);
        
        int numGlyphs = buffer.getShort(tables.get(maxp) + 4);
        boolean longAddresses = locaFormat == 0
                                ? false
                                : true;
        
        GlyphLocator locator = GlyphLocator.getInstance(buffer,
                                                        locaOffset,
                                                        numGlyphs,
                                                        longAddresses);
        
        return new DefaultOTCMap(buffer.duplicate(),
                                 offset,
                                 platform,
                                 format,
                                 locator);
    }
    
    /**
     * Gets the {@code ByteBuffer} which backs this font file, where the
     * position of the buffer which is returned is set to the position of the
     * table in this font for the given tag. 
     * 
     * @param tag The integer value for the ASCII string which defines the
     *            name of a table in this font. All table tags are defined as
     *            static constants in this file.
     * 
     * @return A ByteBuffer positioned at the offset in this font for the tag
     *         which was specified.
     */
    public ByteBuffer getBufferForTable(int tag) {
        int offset = tables.get(tag);
        
        return (ByteBuffer)buffer.duplicate()
                                 .position(offset);
    }
}
