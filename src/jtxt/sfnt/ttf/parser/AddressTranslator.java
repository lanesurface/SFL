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

import static jtxt.sfnt.ttf.parser.CharacterMapper.*;

/**
 * Maps the IDs of glyphs in this font to the address of their respective glyph
 * data. The addresses returned by this class are relative to the beginning of
 * the <code>glyf</code> table, as they are stored in the file, and so they need
 * to be added to the offset of that table to locate the actual address.
 */
public class AddressTranslator {
    private CharacterMapper cmapper;
    private boolean saddr;
    private int goff,
                loff,
                addresses[];
    
    public AddressTranslator(ByteBuffer buffer,
                             int loff,
                             int coff,
                             int goff,
                             int numGlyphs,
                             boolean saddr) {
        this.loff = loff;
        this.saddr = saddr;
        this.goff = goff;
        cmapper = new CharacterMapper(buffer.duplicate(),
                                      coff,
                                      PLATFORM_WINDOWS,
                                      PLATFORM_WINDOWS_UNICODE_BMP);
        addresses = new int[numGlyphs];
        
        buffer.position(loff);
        if (saddr) {
            short[] addresses = new short[numGlyphs];
            buffer.asShortBuffer().get(addresses);
            for (int i = 0; i < addresses.length; i++)
                this.addresses[i] = addresses[i];
        }
        else buffer.asIntBuffer().get(addresses);
    }
    
    public int lookup(char character) {
        return lookupId(cmapper.findId(character));
    }
    
    public int lookupId(int index) {
        return goff + (int)(addresses[index] * Math.pow(2, saddr
                                                           ? 1
                                                           : 0));
    }
}
