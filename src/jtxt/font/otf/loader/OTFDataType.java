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
    
    /* package-private */ Object getDataAsJavaType(byte[] data) {
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
    
    private static long constructBits(byte[] data,
                                      int bytes) {
        if (bytes > data.length || data.length > Long.BYTES)
            throw new IllegalArgumentException("The data cannot be properly "
                                               + "converted to the requested "
                                               + "type.");
        
        long repr = 0;
        
        for (int i = 0; i < data.length; i++)
            repr |= (data[i] & 0xFF)
                    << (data.length - i - 1)
                    * 8;
        
        return repr;
    }
    
    public static long getLong(byte[] data) {
        return (long)constructBits(data,
                                   Long.BYTES);
    }
    
    public static int getInteger(byte[] data) {
        return (int)constructBits(data,
                                  Integer.BYTES);
    }
    
    public static short getShort(byte[] data) {
        return (short)constructBits(data,
                                    Short.BYTES);
    }
    
    public static byte getByte(byte[] data) {
        return (byte)constructBits(data,
                                   Byte.BYTES);
    }
    
    public static float getF2Dot14(byte[] data) {
        /*
         * An f2.14 is a fixed-point decimal number which has two integer
         * digits and fourteen decimal digits.
         * 
         * The fixed-point decimal is converted into a single-precision float-
         * ing point number (which has 16 additional bits of accuracy that
         * cannot be utilized--this is due to the size of floating point
         * numbers in Java).
         */
        
        int i = ~(data[0] >> 2) + 1;
        // TODO: ???
        
        return createIEEEFloat(false,
                               (byte)0b1000_0001,
                               0b011_0000_0000_0000_0000_0000);
    }
    
    /*
     * Masks which are used to extract various components of the integer
     * representation of an IEEE-754 floating-point number.
     */
    private static final int SIGN = 0x80000000,
                             EXPONENT = 0x7F800000,
                             MANTISSA = 0x007FFFFF;
    
    private static float createIEEEFloat(boolean negative,
                                         byte exponent,
                                         int mantissa) {
        return Float.intBitsToFloat(negative ? 1 : 0 << 31
                                    ^ exponent << 23
                                    ^ mantissa & MANTISSA);
    }
}