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
 * Determines the address within the <code>glyf</code> table that the data for
 * a given glyph is contained at.
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

    // IDs specified for each platform in the OTF specification.
    int PLATFORM_UNICODE = 0,
        PLATFORM_OS_X = 1,
        PLATFORM_WINDOWS = 3,
        PLATFORM_CUSTOM = 4;

    // The subset of encodings which are supported by this character mapper.
    int PLATFORM_UNICODE_ID = 5,
        PLATFORM_OS_X_ID = 0,
        PLATFORM_WINDOWS_SYMBOL_ID = 0,
        PLATFORM_WINDOWS_UNICODE_BMP = 1,
        PLATFORM_WINDOWS_UNICODE_FULL = 10;

    /**
     * For the given character, this method returns the ID used to locate that
     * character in this font file.
     *
     * @param character The (Unicode) character to locate in this font.
     * @param features If the character mapper supports features, this field
     *                 may contain those which may be applied to the character.
     * 
     * @return The ID used to locate this character within the font.
     */
    int getGlyphId(char character,
                   int features);
}
