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
public final class GlyphLocator {
    private short[] glyphOffsets;
    private boolean useLongAddresses;
    
    /* package-private */ GlyphLocator(ByteBuffer buffer,
                                       int offset,
                                       int numGlyphs,
                                       boolean useLongAddresses) {
        this.useLongAddresses = useLongAddresses;
        
        glyphOffsets = new short[numGlyphs];
        ((ByteBuffer)buffer.position(offset))
                           .asShortBuffer()
                           .get(glyphOffsets);
    }
    
    public short getAddressForId(int glyphId) {
        int offset = glyphOffsets[glyphId];
        
        return (short)(useLongAddresses
                       ? offset
                       : offset * 2);
    }
}
