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
import java.nio.ShortBuffer;

import jtxt.font.otf.CharacterMapper;

/**
 * 
 */
public class DefaultOTCMap implements CharacterMapper {
    protected static final class EncodingRecord {
        protected short platformId;
        protected short encodingId;
        protected int offset;
        
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
        private static GlyphIndexer createIndexer(ByteBuffer buffer,
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
                return null; // TODO: Need default impl.
            }
        }
        
        @Override
        public String toString() {
            String fmt = "EncodingRecord: [platformId=%d, encodingId=%d, "
                         + "offset=%d] %n";
            return String.format(fmt,
                                 platformId,
                                 encodingId,
                                 offset);
        }
    }
    
    /**
     * Determines the identifier for the given character. The ID returned by a
     * {@code GlyphIndexer} can be used to lookup the address in the
     * <code>loca</code> table.
     */
    protected static abstract class GlyphIndexer {
        protected final ByteBuffer buffer;
        protected final int offset;
        protected final int length;
        
        protected GlyphIndexer(ByteBuffer buffer,
                               int offset,
                               int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }
        
        /**
         * Finds the ID for the given glyph for the cmap subtable format which
         * this {@code GlyphIndexer} has been constructed for, or the index
         * zero (0) for a glyph which is not contained in this mapping. 
         * 
         * @param character The character used to locate the ID returned by
         *                  this method.
         * 
         * @return The ID for the given character, or zero if the character is
         *         not contained within this mapping.
         */
        public abstract int getGlyphId(int character);
    }
    
    private static class ByteIndexer extends GlyphIndexer {
        private byte[] indices;
        
        private ByteIndexer(ByteBuffer buffer,
                            int offset,
                            int length) {
            super(buffer, offset, length);
            indices = new byte[256];
            buffer.get(indices);
        }
        
        @Override
        public int getGlyphId(int character) {
            if (character < 0 || character >= 256) return 0;
            
            return indices[character];
        }
    }
    
    private static class SegmentDeltaIndexer extends GlyphIndexer {
        private ShortBuffer sbuffer;
        private int glyphIdRangeOffset;
        
        private short segments;
        private short[] endCodes;
        private short[] startCodes;
        private short[] idDeltas;
        private short[] idRangeOffset;
        
        private SegmentDeltaIndexer(ByteBuffer buffer,
                                    int offset,
                                    int length) {
            super(buffer, offset, length);
            
            sbuffer = buffer.asShortBuffer();
            segments = (short)(sbuffer.get() / 2);
            /* searchRange */ sbuffer.get();
            /* entrySelector */ sbuffer.get();
            /* rangeShift */ sbuffer.get();
            
            endCodes = new short[segments];
            startCodes = new short[segments];
            idDeltas = new short[segments];
            idRangeOffset = new short[segments];
            
            sbuffer.get(endCodes);
            /* reserved */ sbuffer.get();
            sbuffer.get(startCodes);
            sbuffer.get(idDeltas);
            sbuffer.get(idRangeOffset);
            glyphIdRangeOffset = sbuffer.position();
        }
        
        @Override
        public int getGlyphId(int character) {
            char code = (char)character;
            
            int seg = -1;
            for (int s = 0; s < segments; s++) {
                if (code <= endCodes[s] && code >= startCodes[s]) {
                    seg = s;
                    break;
                }
            }
            if (seg == -1) return 0;
            
            // FIXME: Memory address....
            return sbuffer.get(idRangeOffset[seg]
                               / 2
                               + (code - startCodes[seg])
                               + glyphIdRangeOffset
                               - (idRangeOffset.length - seg)
                               / 2);
        }
    }
    
    private final ByteBuffer buffer;
    private final int offset;
    private final int platformId;
    private final int encodingId;
    
    private EncodingRecord[] records;
    private GlyphIndexer indexer;
    
    /**
     * Creates and initializes the {@code CharacterMapper} for an OpenType
     * font. The buffer should be the same buffer that the font was loaded from
     * (i.e., the buffer returned from creating a new {@code OTFFileReader}).
     * The offset should be the location which is given in the table records
     * at the beginning of the font file. (This means that the offset is an
     * absolute position in memory.) Platform and encoding IDs specify
     * platform-specific behavior for this character mapper, according to the
     * constraints given in the OpenType specification. The platforms and
     * encodings supported by this character mapper are the constants which
     * this file defines, prefixed with <code>PLATFORM_...</code>.
     * 
     * @param buffer
     * @param offset
     * @param platformId
     * @param encodingId
     */
    /* package-private */ DefaultOTCMap(ByteBuffer buffer,
                                        int offset,
                                        int platformId,
                                        int encodingId) {
        if (platformId < 0 || platformId > 4)
            throw new IllegalArgumentException("Unsupported platform ID "
                                               + platformId);
        
        this.buffer = buffer;
        this.offset = offset;
        this.platformId = platformId;
        this.encodingId = encodingId;
        
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
                indexer = EncodingRecord.createIndexer(buffer.duplicate(),
                                                       offset + 4 + i * 8);
        }
        
        /*
         * TODO: If no compatible record is found, create default indexer
         *       instead.
         */
        
        System.out.println(indexer.getGlyphId('A'));
    }
    
    @Override
    public int getGlyphOffset(int character, int features) {
        return 0;
    }
}
