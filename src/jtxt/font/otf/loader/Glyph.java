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

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;

/**
 * 
 */
public abstract class Glyph {
    protected ByteBuffer buffer;
    protected Rectangle2D bounds;
    protected final int offset,
                        ptSize;
    
    protected final short numContours,
                          xMin,
                          yMin,
                          xMax,
                          yMax;
    
    public abstract Path2D getPath();
    
    protected Glyph(ByteBuffer buffer,
                    int offset,
                    int ptSize) {
        this.buffer = buffer;
        this.offset = offset;
        this.ptSize = ptSize;
        
        buffer.position(offset);
        numContours = buffer.getShort();
        xMin = buffer.getShort();
        yMin = buffer.getShort();
        xMax = buffer.getShort();
        yMax = buffer.getShort();
        bounds = new Rectangle2D.Float(xMin,
                                       yMin,
                                       xMax,
                                       yMax);
    }
    
    public static class SimpleGlyph extends Glyph {
        private enum Flag {
            ON_CURVE_POINT,
            X_SHORT_VECTOR,
            Y_SHORT_VECTOR,
            REPEAT_FLAG,
            X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR,
            Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR,
            OVERLAP_SIMPLE;
        
            boolean instructing(byte flag) {
                byte binPosition = (byte)Math.pow(2,
                                                  ordinal());
                
                return (binPosition & flag) == binPosition;
            }
        }
        
        private final short[] endPoints;
        private final byte[] flags;
        private final short[] xCoordinates,
                              yCoordinates;
        
        SimpleGlyph(ByteBuffer buffer,
                    int offset,
                    int ptSize,
                    int dataRegionLength) {
            super(buffer, offset, ptSize);
            
            endPoints = new short[numContours];
            buffer.asShortBuffer()
                  .get(endPoints);
//            buffer.get(null,
//                       0,
//                       buffer.getShort());
            flags = expandFlags(offset
                                + numContours
                                * 2
                                + buffer.getShort());
            
            xCoordinates = yCoordinates = null;
        }
        
        private byte[] expandFlags(int offset) {
            byte[] flags = new byte[endPoints[numContours - 1]];
            
            for (int i = 0; i < flags.length; i++) {
                byte flag = buffer.get(offset + i);
                flags[i] = flag;
                
                if (Flag.REPEAT_FLAG.instructing(flag)) {
                    byte n = buffer.get(offset + i + 1);
                    
                    for (int j = 0; j < n; j++)
                        flags[i + j + 1] = flag;
                    i += n;
                }
            }
            
            return flags;
        }
        
        @Override
        public Path2D getPath() {
            QuadCurve2D curve = new QuadCurve2D.Float(0,
                                                      0,
                                                      0,
                                                      0,
                                                      0,
                                                      0);
            return new Path2D.Float(curve, AffineTransform.getScaleInstance(0,
                                                                            0));
        }
        
        @Override
        public String toString() {
            String res = "";
            for (int i = 0; i < flags.length; i++)
                res += "f(" + i
                            + ")="
                            + Integer.toBinaryString(flags[i])
                            + "\n";
            
            return res;
        }
    }
    
    public static class CompositeGlyph { /* TODO */ }
}
