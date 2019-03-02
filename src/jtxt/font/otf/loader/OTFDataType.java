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

import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * 
 */
public enum OTFDataType {
    UINT8(8),
    INT8(8),
    UINT16(16),
    INT16(16),
    UINT24(24),
    UINT32(32),
    INT32(32),
    FIXED(32),
    FWORD(16),
    UFWORD(16),
    F2DOT14(16),
    LONGDATETIME(64) {
        @Override
        public ZonedDateTime getDataAsJavaType(byte[] data) { return null; }
    },
    TAG(32),
    OFFSET16(16),
    OFFSET32(32);
    
    public final int bits;
    
    OTFDataType(int size) {
        this.bits = size;
    }
    
    public int getNumBytes() { return bits / 8; }
    
    public Object getDataAsJavaType(byte[] data) {
        int len = data.length,
            repr = 0;
        
        if (len != getNumBytes())
            throw new IllegalArgumentException("The number of bytes "
                                               + "in the data array is "
                                               + "incompatible with this "
                                               + "type.");
        
        for (int i = 0; i < len; i++)
            repr |= (data[i] & 0xFF)
                    << (len - i - 1)
                    * 8;
        
        return repr;
    }
    
    public OTFDataType[] array(int length) {
        OTFDataType[] types = new OTFDataType[length];
        /*
         * Filling the types array with instances of `this` puts the constant
         * which this method was called on into the array.
         */
        Arrays.fill(types,
                    this);
        
        return types;
    }
}