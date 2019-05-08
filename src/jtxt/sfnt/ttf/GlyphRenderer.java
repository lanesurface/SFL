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

/**
 * Defines how text processed by the font parser should ultimately be
 * rendered on the destination device. That is to say that properties of
 * rendering are defined by the implementation of this interface, including
 * such things as the anti-aliasing methods used to increase the legibility
 * of this rasterized image.
 * 
 * <p>The type of rendering technology used, as well
 * as the methods taken to achieve the final product, are left up to the
 * client's discretion. The various graphics technologies make it difficult
 * to predict the best way to go about this. Preferences for rasterization
 * techniques also make the implementation a very personal choice. However,
 * the outline data can be easily rendered by the native Java2D
 * {@code Graphics2D} class if so desired, though the results are suboptimal
 * and you are certainly better off using the provided mechanism in that class
 * for rendering text instead.</p>
 */
public interface GlyphRenderer {
    /**
     * Renders the given character to the destination device. The y-coordinate
     * will be used as the position for the baseline of this character, and 
     * the x-coordinate will appear at the leftmost extreme on the horizontal
     * axis of the resulting glyph.
     * 
     * @param character The character to render.
     * @param x The x translation for this character.
     * @param y The y translation for this character.
     */
    void draw(char character,
              int x,
              int y);
    
    /**
     * Draws the given String to the screen, placing the first glyph at the
     * given (x,&nbsp;y) coordinate and laying out all subsequent glyphs at
     * positions respective to the advance width defined in the font.
     *
     * @param string The string of text to render.
     * @param x The x position of the first Glyph rendered.
     * @param y The y position of the first Glyph rendered.
     */
    void draw(String string,
              int x,
              int y);
}
