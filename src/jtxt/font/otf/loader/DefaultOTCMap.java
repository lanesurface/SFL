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

import jtxt.font.otf.CharacterMapper;

/**
 * 
 */
public class DefaultOTCMap implements CharacterMapper {
    private final ByteBuffer buffer;
    private final int offset;
    private final int platformId;
    private final int format;
    
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
    
    private EncodingRecord[] records;
    
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
        this.format = encodingId;
        
        buffer.position(offset);
        /* version */ buffer.getShort();
        short numTables = buffer.getShort();
        records = new EncodingRecord[numTables];
        for (int i = 0; i < numTables; i++)
            records[i] = new EncodingRecord(buffer.getShort(),
                                            buffer.getShort(),
                                            buffer.getInt());
        System.out.println(Arrays.toString(records));
    }
    
    @Override
    public int getGlyphOffset(int character, int features) {
        // TODO
        return 0; // .notdef
    }
}
