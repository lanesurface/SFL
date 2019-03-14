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

import jtxt.font.otf.CharacterMapper;

/**
 * 
 */
public class DefaultOTCMap implements CharacterMapper {
    private final ByteBuffer buffer;
    private final int offset;
    private final int platformId;
    private final int encodingId;
    
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
        private short[] indices;
        
        private SegmentDeltaIndexer(ByteBuffer buffer,
                                    int offset,
                                    int length) {
            super(buffer, offset, length);
        }
        
        @Override
        public int getGlyphId(int character) { return 0; }
    }
    
    private GlyphIndexer[] indexers;
    private EncodingRecord[] records;
    private final int recordIndex;
    private int format;
    
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
     * 
     * @throws UnsupportedEncodingScheme
     */
    /* package-private */ DefaultOTCMap(ByteBuffer buffer,
                                        int offset,
                                        int platformId,
                                        int encodingId)
       throws UnsupportedEncodingScheme
{
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
        int ri = -1;
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
            if (pId == platformId && eId == encodingId) ri = i;
        }
        if (ri < 0)
            throw new UnsupportedEncodingScheme("The provided encoding ID was "
                                                + "not found in this font.");
        recordIndex = ri;
        
        buffer.position(offset + records[recordIndex].offset);
        format = buffer.getShort();
        initializeCharacterIndexer(format);
    }
    
    private GlyphIndexer initializeCharacterIndexer(int format) {
        System.out.println("offset="
                           + offset
                           + "\nformat="
                           + format);
        
        int length = buffer.getShort();
        /* language */ buffer.getShort();
        ByteBuffer buffer = this.buffer.duplicate();
        switch (format) {
        case 0:
            return new ByteIndexer(buffer,
                                   offset,
                                   length);
        case 4:
            return new SegmentDeltaIndexer(buffer,
                                           offset,
                                           length);
        default:
            return null; // TODO: Need default impl.
        }
    }
    
    @Override
    public int getGlyphOffset(int character, int features) {
        return 0;
    }
}
