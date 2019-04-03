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

/**
 * Handles the interpretation and scaling of glyph metrics for a given font
 * file. Note that the metrics defined by an instance of this class are only
 * valid for a specific font face; the metrics for a bold and itallic face
 * are not the same.
 * 
 * <p>
 * The placement and layout of Glyphs on a device is a low-level function of
 * this library and, unless explicitly necessitated by the application of this
 * technology in a circumstance where the provided layout is insufficient,
 * should not be handled by a client-implementation. For arranging strings and
 * long pieces of text on the screen, see the methods provided by the given
 * font face being used to extract glyph information from the font file.
 * </p>
 */
public class Metrics {
    private static class HMetricProvider {
        static final class HMetricEntry {
            public final short aw,
                               lsb;
            
            public HMetricEntry(short aw,
                                short lsb) {
                this.aw = aw;
                this.lsb = lsb;
            }
        }
        
        private HMetricEntry[] entries;
        
        HMetricProvider(ByteBuffer buffer,
                        short hMetricEntries,
                        int numGlyphs) {
            short lastAW = 0;
            for (int i = 0; i < numGlyphs; i++) {
                if (i >= hMetricEntries) {
                    entries[i] = new HMetricEntry(lastAW,
                                                  buffer.getShort());
                    continue;
                }
                
                entries[i] = new HMetricEntry(buffer.getShort(),
                                              buffer.getShort());
            }
        }
    }
    
    private final int ptSize,
                      unitsPerEm,
                      dpi;
    private HMetricProvider hmtx;
    
    public Metrics(OTFFileReader reader,
                   int ptSize,
                   int unitsPerEm,
                   int dpi) {
        this.ptSize = ptSize;
        this.unitsPerEm = unitsPerEm;
        this.dpi = dpi;
        
        ByteBuffer hmBuffer = reader.getBufferForTable(OTFFileReader.hmtx);
        hmtx = new HMetricProvider(hmBuffer,
                                   (short)0,
                                   0);
        // TODO: We need some sort of common interface for a scaler.
    }
    
    public int getAdvanceWidh(Glyph glyph) {
        return hmtx.entries[glyph.id].aw;
    }
    
    public int getLeftSideBearing(Glyph glyph) {
        return hmtx.entries[glyph.id].lsb;
    }
    
    public int getRightSideBearing(Glyph glyph) {
        HMetricProvider.HMetricEntry entry = hmtx.entries[glyph.id];
        
        return (int)(entry.aw
                     - (entry.lsb
                     + glyph.getBounds().getWidth()));
    }
}
