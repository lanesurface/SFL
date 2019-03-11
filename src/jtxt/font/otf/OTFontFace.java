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
import java.awt.image.RenderedImage;
import java.nio.file.Path;

import jtxt.font.otf.loader.OTFFileReader;

/**
 * 
 */
public class OTFontFace implements RasterFont {
    private CharacterMapper cmapper;
    private OTFFileReader fontFile;
    
    private final int size;
    private final String name;
    private final int renderAttribs;
    
    public OTFontFace(Path path,
                      int size,
                      String name,
                      int renderAttribs) {
        fontFile = new OTFFileReader(path.toFile());
        this.size = size;
        this.name = name;
        this.renderAttribs = renderAttribs;
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
