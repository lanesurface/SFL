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

/**
 * Determines the identifier for the given character. The ID returned by a
 * {@code GlyphIndexer} can be used to lookup the address in the
 * <code>loca</code> table.
 */
@FunctionalInterface
public interface GlyphIndexer {
    /**
     * Finds the ID for the given glyph for the cmap subtable format which
     * this {@code GlyphIndexer} has been constructed for.
     * 
     * @param character The character used to locate the ID returned by
     *                  this method.
     * 
     * @return The ID for the given character, or zero if the character is
     *         not contained within this mapping.
     */
    int getGlyphId(int character);
}