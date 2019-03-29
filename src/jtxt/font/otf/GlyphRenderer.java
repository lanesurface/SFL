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

/**
 * 
 */
public interface GlyphRenderer {
    /**
     * Renders the given character onto the graphics context, using the
     * coordinates provided as the position for the lower-left corner of the
     * bounding box for the resulting {@code Glyph}. Note that this method
     * will interpret the coordinates as relative to the graphics environment,
     * meaning that all translations are preserved in the resulting
     * rasterization.
     * 
     * @param character The character to render.
     * @param x The x translation for this character.
     * @param y The y translation for this character.
     */
    void drawGlyph(char character,
                   int x,
                   int y);
    
    /**
     * Draws the given String to the screen, placing the first Glyph at the
     * given (x,&nbsp;y) coordinate and laying out all subsequent Glyphs at
     * positions respective to the advance width defined in the font.
     *
     * @param string The string of text to render.
     * @param x The x position of the first Glyph rendered.
     * @param y The y position of the first Glyph rendered.
     */
    void drawString(String string,
                    int x,
                    int y);
}
