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

/**
 * Maps the IDs of glyphs in this font to the address of their respective glyph
 * data. The addresses returned by this class are relative to the beginning of
 * the <code>glyf</code> table, as they are stored in the file, and so they need
 * to be added to the offset of that table to locate the actual address.
 */
public interface GlyphLocator {
    static GlyphLocator getInstance(ByteBuffer buffer,
                                    int offset,
                                    int numGlyphs,
                                    boolean useLongAddresses) {
        if (useLongAddresses) return new LongGlyphLocator(buffer,
                                                          offset,
                                                          numGlyphs);
        
        return new ShortGlyphLocator(buffer,
                                     offset,
                                     numGlyphs);
    }
    
    /**
     * 
     * 
     * @param glyphId
     * 
     * @return
     */
    static int findLengthOfDataRegion(GlyphLocator locator, int glyphId) {
        return locator.getAddressOfId(glyphId + 1)
               - locator.getAddressOfId(glyphId);
    }
    
    static final class ShortGlyphLocator implements GlyphLocator {
        private short[] addresses;
        
        public ShortGlyphLocator(ByteBuffer buffer,
                                 int offset,
                                 int numGlyphs) {
            addresses = new short[numGlyphs];
            ((ByteBuffer)buffer.position(offset))
                               .asShortBuffer()
                               .get(addresses);
        }
        
        @Override
        public int getAddressOfId(int glyphId) {
            return addresses[glyphId];
        }
    }
    
    static final class LongGlyphLocator implements GlyphLocator {
        private int[] addresses;
        
        public LongGlyphLocator(ByteBuffer buffer,
                                int offset,
                                int numGlyphs) {
            addresses = new int[numGlyphs];
            ((ByteBuffer)buffer.position(offset))
                               .asIntBuffer()
                               .get(addresses);
        }
        
        @Override
        public int getAddressOfId(int glyphId) {
            return addresses[glyphId];
        }
    }
    
    /**
     * For the given ID belonging to a glyph in this font, this method will
     * return the offset of that glyph in this font, assuming that the ID is
     * a valid index. If the ID is outside the range of the glyphs in the data
     * region, an {@code IndexOutOfBoundsException} may be thrown. (This method
     * does not return the <code>.notdef</code> address for invalid indices;
     * the mapping of characters to that glyph is left up components which
     * deal with the <code>cmap</code> table.)
     * 
     * @param glyphId The ID of a valid glyph in this font.
     * 
     * @return The address of the glyph belonging to the given ID.
     */
    int getAddressOfId(int glyphId);
}
