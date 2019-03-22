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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
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
    
    /**
     * Gets the outline of this glyph as a {@code Path2D} object which can be
     * rendered by a call to {@link Graphics2D#draw(java.awt.Shape)}. The path
     * which is returned is converted into device space (i.e. pixel
     * coordinates) according to the parameters supplied to this method. The
     * value of <code>dotsPerInch</code> can be obtained on a per-device basis
     * with the {@code Toolkit} utility class. The <code>unitsPerEm</code> are
     * defined in the header table for this font.
     * 
     * @param dotsPerInch The resolution of the screen that this {@code Glyph}
     *                    will be rendered to.
     * @param unitsPerEm The granularity of the EM square which all coordinate
     *                   data relative to. (The EM coordinate system chosen by
     *                   the font designer.)
     * @param pointSize The size of the text that should be returned from this
     *                  method, where one point is equal to <code>1/72</code>
     *                  of an inch. (So a value of 72 here would cause the
     *                  glyph returned from this method to be rendered at a
     *                  size of one inch on the destination device.)
     * 
     * @return A {@code Glyph} according to the specified parameters.
     */
    public abstract Path2D getPath(int dotsPerInch,
                                   int unitsPerEm,
                                   int pointSize);
    
    /**
     * A {@code SimpleGlyph} is a glyph which defines all of the contours
     * required for drawing it.
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
        
        private final short numCoordinates;
        private final short[] endPoints,
                              xCoords,
                              yCoords;
        private final byte[] flags,
                             instructions;
        
        public SimpleGlyph(ByteBuffer buffer,
                           int offset,
                           int dataRegionLength) {
            super(buffer, offset);
            
            int lo = buffer.position();
            endPoints = new short[numContours];
            buffer.asShortBuffer()
                  .get(endPoints);
            numCoordinates = (short)(endPoints[numContours - 1] + 1);
            buffer.position(lo + numContours * 2);
            instructions = new byte[buffer.getShort()];
            buffer.get(instructions);
            flags = new byte[numCoordinates];
            
            for (int i = 0; i < flags.length; i++) {
                byte flag = buffer.get();
                flags[i] = flag;
                
                if (Flag.REPEAT_FLAG.instructing(flag)) {
                    byte n = buffer.get();
                    while (n-- > 0) flags[++i] = flag;
                }
            }
            
            xCoords = readCoordinates(Flag.X_SHORT_VECTOR,
                                      Flag.X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR);
            yCoords = readCoordinates(Flag.Y_SHORT_VECTOR,
                                      Flag.Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR);
        }
        
        private short[] readCoordinates(Flag shortVectorFlag,
                                        Flag deltaFlag) {
            short[] coords = new short[numCoordinates];
            for (int i = 0; i < numCoordinates; i++) {
                byte flag = flags[i];
                if (shortVectorFlag.instructing(flag)) {
                    byte val = buffer.get();
                    if (deltaFlag.instructing(flag))
                        coords[i] = val;
                    else  coords[i] = (byte)-val;
                    
                    continue;
                }
                
                if (deltaFlag.instructing(flag)) coords[i] = coords[i - 1];
                else coords[i] = buffer.getShort();
            }
            
            return coords;
        }
        
        byte[] getInstructions() {
            return instructions;
        }
        
        @Override
        public Path2D getPath(int dotsPerInch,
                              int unitsPerEm,
                              int pointSize) {
            Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            
            int prevX = 0,
                prevY = 0,
                point = 0;
            for (int contour = 0; contour < endPoints.length; contour++) {
                for (; point < endPoints[contour]; point++) {
                    /*
                     * FIXME: The coordinate data makes up a series of bezier
                     *        splines which define this contour, but I'm not
                     *        sure how the ON_CURVE flag governs the way that
                     *        these splines should be constructed. (I do know,
                     *        however, that they are quadratic for TTF data.)
                     */
                    int x = xCoords[point],
                        y = yCoords[point];
                    path.append(new Line2D.Float(prevX,
                                                 prevY,
                                                 prevX + x,
                                                 prevY + y), true);
                    prevX = x;
                    prevY = y;
                }
                
//                path.append(new Line2D.Float(0.f,
//                                             0.f,
//                                             0.f,
//                                             0.f), false);
            }
            
            // The conversion ratio for FUnit -> device space coordinates.
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
