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
package jtxt.sfnt.ttf;

import java.awt.Color;
import java.awt.image.RenderedImage;

/**
 * A {@code RasterFont} is an instance of a font (meaning that certain 
 * attributes are already defined, such as size, style, and the font-family)
 * which is capable of rendering text as a bitmap image.
 * 
 * <p>
 * A RasterFont takes a character or string of characters which need to be
 * rendered, and returns the respective glyph(s) in the form of a
 * {@link java.awt.image.RenderedImage}.
 * </p>
 */
public interface RasterFont {
    int MONOSPACE_LETTERFORM = 0,
        SUBPIXEL_ANTIALIAS = 1,
        GRAYSCALE_ANTIALIAS = 2,
        NO_ANTIALIAS = 4;
    
    RenderedImage getGlyph(char character,
                           Color color,
                           int xPad,
                           int yPad,
                           int hints);
    
    default RenderedImage[] getGlyphs(String text,
                                      Color[] colors,
                                      int[] colorIndices,
                                      int xPad,
                                      int yPad,
                                      int hints) {
        if (colorIndices.length != text.length())
            throw new IllegalArgumentException("The length of the color index "
                                               + "array must be the same as "
                                               + "the length of the string.");
        
        RenderedImage[] glyphs = new RenderedImage[text.length()];
        for (int i = 0; i < text.length(); i++)
            glyphs[i] = getGlyph(text.charAt(i),
                                 colors[colorIndices[i]],
                                 0,
                                 yPad,
                                 hints);
        
        return glyphs;
    }
}
