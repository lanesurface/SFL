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
package jtxt.font.otf.loader;

/**
 * Indicates that a encoding ID has been provided to a {@code CharacterMapper}
 * which that font does not support.
 */
public class UnsupportedEncodingScheme extends IllegalArgumentException {
    public UnsupportedEncodingScheme() {
        super();
    }
    
    public UnsupportedEncodingScheme(String reason) {
        super(reason);
    }
}
