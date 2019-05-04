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

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * 
 */
public class CharacterMapper {
    public static final int PLATFORM_UNICODE = 0,
                            PLATFORM_OS_X = 1,
                            PLATFORM_WINDOWS = 3,
                            PLATFORM_CUSTOM = 4;
    
    // The subset of encodings which are supported by this character mapper.
    public static final int PLATFORM_UNICODE_ID = 5,
                            PLATFORM_OS_X_ID = 0,
                            PLATFORM_WINDOWS_SYMBOL_ID = 0,
                            PLATFORM_WINDOWS_UNICODE_BMP = 1,
                            PLATFORM_WINDOWS_UNICODE_FULL = 10;
    
    protected static final class EncodingRecord {
        public short platformId,
                     encodingId;
        public int offset;
        
        private EncodingRecord(short platformId,
                               short encodingId,
                               int offset) {
            this.platformId = platformId;
            this.encodingId = encodingId;
            this.offset = offset;
        }
        
        /**
         * Determines and returns an appropriate {@code GlyphIndexer} for the
         * given format, according to the formats which have been defined in
         * the OpenType specification. See the <code>cmap</code> page for
         * details about the layout of the various glyph-to-index tables.
         * 
         * @param buffer The buffer for this font.
         * @param offset The offset into the buffer for the beginning of the
         *               cmap subtables (which start directly after the record
         *               entries).
         * 
         * @return A compatible {@code GlyphIndexer}.
         */
        public static GlyphIndexer createIndexer(ByteBuffer buffer,
                                                 int offset) {
            short format = buffer.getShort();
            int length = buffer.getShort();
            /* language */ buffer.getShort();
            
            switch (format) {
            case 0:
                return new ByteIndexer(buffer,
                                       offset,
                                       length);
            case 4:
                return new SegmentDeltaIndexer(buffer,
                                               offset,
                                               length);
            case 6:
            case 12:
            case 14:
            default:
                /*
                 * If the format which is defined by this record does not have
                 * an implementation, return the `.notdef` character in all
                 * cases.
                 */
                return id -> 0;
            }
        }
        
        @Override
        public String toString() {
            String fmt = "EncodingRecord: [platformId=%d, encodingId=%d, "
                         + "offset=%d]%n";
            
            return String.format(fmt,
                                 platformId,
                                 encodingId,
                                 offset);
        }
    }
    
    private static class ByteIndexer implements GlyphIndexer {
        private byte[] indices;
        
        private ByteIndexer(ByteBuffer buffer,
                            int offset,
                            int length) {
            indices = new byte[256];
            buffer.get(indices);
        }
        
        @Override
        public int getGlyphId(int character) {
            if (character < 0 || character >= 256) return 0;
            
            return indices[character];
        }
    }
    
    private static class SegmentDeltaIndexer implements GlyphIndexer {
        private ShortBuffer buffer;
        private int glyphIdArrayOffset;
        
        private short segments;
        private short[] endCodes,
                        startCodes,
                        idDeltas,
                        idRangeOffsets;
        
        private SegmentDeltaIndexer(ByteBuffer buff,
                                    int offset,
                                    int length) {
            buffer = buff.asShortBuffer();
            segments = (short)(buffer.get() / 2);
            /* searchRange */ buffer.get();
            /* entrySelector */ buffer.get();
            /* rangeShift */ buffer.get();
            
            endCodes = new short[segments];
            startCodes = new short[segments];
            idDeltas = new short[segments];
            idRangeOffsets = new short[segments];
            
            buffer.get(endCodes);
            /* reserved */ buffer.get();
            buffer.get(startCodes);
            buffer.get(idDeltas);
            buffer.get(idRangeOffsets);
            glyphIdArrayOffset = buffer.position();
        }
        
        private int findAddressForIdRange(int segment) {
            return glyphIdArrayOffset
                   - (idRangeOffsets.length - segment)
                   * 2;
        }
        
        @Override
        public int getGlyphId(int character) {
            char code = (char)character;
            
            int s = 0;
            for (; s < segments; s++) {
                if (code <= endCodes[s]) {
                    if (code >= startCodes[s]) break;
                    
                    /*
                     * The given character is not in any of the segments
                     * defined by this mapping.
                     */
                    return 0;
                }
            }
            
            short idRangeOffset = idRangeOffsets[s];
            if (idRangeOffset == 0) return code + idDeltas[s];
            
            int id = buffer.get(idRangeOffset
                                + (code - startCodes[s])
                                + findAddressForIdRange(s));
            
            if (id == 0) return 0;
            
            return (id + idDeltas[s]) % 65536;
        }
    }
    
    private EncodingRecord[] records;
    private GlyphIndexer indexer;
    
    /**
     * TODO: Update this documentation once the API is stable.
     */
    /* package-private */ CharacterMapper(ByteBuffer buffer,
                                          int offset,
                                          int platformId,
                                          int encodingId) {
        if (platformId < 0 || platformId > 4)
            throw new IllegalArgumentException("Unsupported platform ID "
                                               + platformId);
        
        buffer.position(offset);
        /* version */ buffer.getShort();
        short numTables = buffer.getShort();
        records = new EncodingRecord[numTables];
        
        for (int i = 0; i < numTables; i++) {
            /* 
             * Platform and encoding IDs for this record, named in such a way
             * as to not conflict with the already existing local variables in
             * this constructor.
             */
            short pId = buffer.getShort(),
                  eId = buffer.getShort();
            records[i] = new EncodingRecord(pId,
                                            eId,
                                            buffer.getInt());
            
            if (pId == platformId && eId == encodingId)
                indexer = EncodingRecord.createIndexer(buffer,
                                                       offset + 4 + i * 8);
        }
    }
    
    public GlyphIndexer getGlyphIndexer() {
        return indexer;
    }
}