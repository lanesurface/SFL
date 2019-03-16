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
package jtxt.font.otf;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.RenderedImage;

import jtxt.font.otf.loader.OTFFileReader;

/**
 * 
 */
public class OpenTypeFont {
    private CharacterMapper cmapper;
    private OTFFileReader fontFile;
    private final String name;
    
    public OpenTypeFont(java.nio.file.Path path, String name) {
        fontFile = new OTFFileReader(path.toFile());
        cmapper = fontFile.createCharacterMapper();
        this.name = name;
    }
    
    public OpenTypeFace createTypeFace(int size, int renderAttribs) {
        return new OpenTypeFace(this,
                                size,
                                renderAttribs);
    }
    
    public static class OpenTypeFace implements RasterFont,
                                                VectorFont {
        private final OpenTypeFont masterFont;
        private final int size;
        private final int renderAttribs;
        
        public OpenTypeFace(OpenTypeFont masterFont,
                            int size,
                            int renderAttribs) {
            this.masterFont = masterFont;
            this.size = size;
            this.renderAttribs = renderAttribs;
        }
        
        public Path2D getGlyphPath(char character, int hints) {
            
            return null;
        }
        
        @Override
        public RenderedImage getGlyph(char character,
                                      Color color,
                                      int xPad,
                                      int yPad,
                                      int hints) {
            
            return null;
        }
    }
}
