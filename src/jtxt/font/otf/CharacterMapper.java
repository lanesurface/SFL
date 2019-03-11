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
 * For a given character (which, in Java, means a valid Unicode code point),
 * a {@code CharacterMapper} determines the index in the file that the given
 * glyph data is contained at.
 */
public interface CharacterMapper {
    /**
     * As not all font files may support so-called features for glyph index
     * mapping, the default behavior for any CharacterMapper is to never use
     * any other supplemental information for locating glyph data. Nonetheless,
     * since mappers for fonts which support or contain features and for those
     * which don't both implement this interface, both must be able to receive
     * a feature flag.
     */
    int NO_FEATURES = 0;
    
    /**
     * For the Unicode character and the features requested, this method will
     * return an index into the font file 
     * 
     * @param character
     * @param features
     * 
     * @return
     */
    int getGlyphOffset(char character,
                       int features);
}
