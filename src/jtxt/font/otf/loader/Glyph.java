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
    protected final int offset,
                        dotsPerInch,
                        unitsPerEm;
    protected final short numContours;
    
    protected Glyph(ByteBuffer buffer,
                    int offset,
                    int dotsPerInch,
                    int unitsPerEm) {
        this.buffer = buffer;
        this.offset = offset;
        this.dotsPerInch = dotsPerInch;
        this.unitsPerEm = unitsPerEm;
        
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
     * coordinates) according to device-specific properties and the unit
     * conversion factor determined by the font designer, using
     * <code>pointSize</code> to determine the height of the glyph.
     * 
     * @param pointSize The size of the text that should be returned from this
     *                  method, where one point is equal to <code>1/72</code>
     *                  of an inch. (So a value of 72 here would cause the
     *                  glyph returned from this method to be rendered at a
     *                  size of one inch on the destination device.)
     * 
     * @return A path for this glyph in device-space.
     */
    public abstract Path2D getPath(int pointSize);
    
    /**
     * A {@code SimpleGlyph} is a glyph which defines all of the contours
     * required for drawing it.
     */
    public static final class SimpleGlyph extends Glyph {
        // Flags
        private static final byte ON_CURVE_POINT = 1 << 0,
                                  X_SHORT_VECTOR = 1 << 1,
                                  Y_SHORT_VECTOR = 1 << 2,
                                  REPEAT  = 1 << 3,
                                  X_DELTA = 1 << 4,
                                  Y_DELTA = 1 << 5,
                                  OVERLAP_SIMPLE = 1 << 6;
        
        private final short numCoordinates;
        private final short[] endPoints,
                              xCoords,
                              yCoords;
        private final byte[] flags,
                             instructions;
        
        public SimpleGlyph(ByteBuffer buffer,
                           int offset,
                           int dotsPerInch,
                           int unitsPerEm) {
            super(buffer, offset, dotsPerInch, unitsPerEm);
            
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
                
                if ((REPEAT & flag) > 0) {
                    byte n = buffer.get();
                    while (n-- > 0) flags[++i] = flag;
                }
            }
            
            xCoords = readCoordinates(X_SHORT_VECTOR, X_DELTA);
            yCoords = readCoordinates(Y_SHORT_VECTOR, Y_DELTA);
        }
        
        private short[] readCoordinates(int shortVector,
                                        int delta) {
            short[] coords = new short[numCoordinates];
            
            short val = 0;
            for (int i = 0; i < numCoordinates; i++) {
                byte flag = flags[i];
                if ((shortVector & flag) > 0)
                    val += buffer.get() * Math.pow(-1,
                                                   ~((flag & delta)
                                                    >> (int)Math.sqrt(delta)));
                else if ((delta & ~flag) > 0)
                    val += buffer.getShort();
                
                coords[i] = val;
            }
            
            return coords;
        }
        
        byte[] getInstructions() {
            return instructions;
        }
        
        @Override
        public Path2D getPath(int pointSize) {
            Path2D path = new Path2D.Float(Path2D.WIND_NON_ZERO);
            
            int prevX = (int)bounds.getX(),
                prevY = (int)bounds.getY(),
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
                                                 x,
                                                 y), false);
                    prevX = x;
                    prevY = y;
                }
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
