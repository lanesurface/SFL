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
    protected final int offset;
    protected final short numContours;
    
    protected Glyph(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
        
        buffer.position(offset);
        numContours = buffer.getShort();
        short xMin = buffer.getShort(),
              yMin = buffer.getShort(),
              xMax = buffer.getShort(),
              yMax = buffer.getShort();
        bounds = new Rectangle2D.Float(xMin,
                                       yMin,
                                       xMax,
                                       yMax);
    }
    
    public Rectangle2D getBounds() {
        return bounds;
    }
    
    public abstract Path2D getPath(int dotsPerInch,
                                   int unitsPerEm,
                                   int pointSize);
    
    /**
     * A {@code SimpleGlyph} is a glyph which defines all of the contours required for
     * drawing it. 
     */
    public static final class SimpleGlyph extends Glyph {
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
        
        private final short[] endPoints,
                              xCoords,
                              yCoords;
        private final byte[] flags,
                             instructions;
        
        SimpleGlyph(ByteBuffer buffer,
                    int offset,
                    int dataRegionLength) {
            super(buffer, offset);
            
            int lo = buffer.position();
            endPoints = new short[numContours];
            buffer.asShortBuffer()
                  .get(endPoints);
            buffer.position(lo + numContours * 2);
            instructions = new byte[buffer.getShort()];
            buffer.get(instructions);
            flags = new byte[endPoints[numContours - 1] + 1];
            
            for (int i = 0; i < flags.length; i++) {
                byte flag = (byte)(buffer.get() & 0xFF);
                flags[i] = flag;
                System.out.println("flag("
                                   + i
                                   + ")="
                                   + flag
                                   + ", bin="
                                   + Integer.toBinaryString(flag));
                
                if (Flag.REPEAT_FLAG.instructing(flag)) {
                    byte n = buffer.get();
                    System.out.println("Repeat " + n + " times...");
                    
                    while (n-- > 0) flags[++i] = flag;
                }
            }
            
            xCoords = yCoords = null;
        }
        
        byte[] getInstructions() {
            return instructions;
        }
        
        @Override
        public Path2D getPath(int dotsPerInch,
                              int unitsPerEm,
                              int pointSize) {
            Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            
            for (int contour = 0; contour < endPoints.length; contour++) {
                for (int point = endPoints[contour];
                     point < endPoints[contour];
                     point += 3)
                {
                    path.append(new QuadCurve2D.Float(xCoords[point],
                                                      yCoords[point],
                                                      xCoords[point + 1],
                                                      yCoords[point + 1],
                                                      xCoords[point + 2],
                                                      yCoords[point + 2]),
                                true);
                }
            }
            
            // The conversion ratio for pixel -> device space coordinates.
            double dsc = dotsPerInch
                         * (1 / 72.d)
                         * pointSize
                         / unitsPerEm;
            return new Path2D.Float(path,
                                    AffineTransform.getScaleInstance(dsc,
                                                                     dsc));
        }
    }
    
    public static class CompositeGlyph { /* TODO */ }
}
