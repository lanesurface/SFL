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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;

import jtxt.sfnt.ttf.CharacterMapper;

/**
 * A {@code Glyph} is the representation of a character in a font. Characters
 * define the function of a letter form, and then the character is indexed and
 * located in the font file, where it is constructed from it's outline data
 * here. Some Glyphs are a combination of two or more "Simple" glyphs. This
 * allows for compact representations of outlines like accents.
 * 
 * @see CharacterMapper
 * @see GlyphLocator
 */
public abstract class Glyph {
    protected ByteBuffer buffer;
    protected Rectangle2D bounds;
    protected final int offset,
                        dotsPerInch,
                        unitsPerEm,
                        pointSize;
    protected final short numContours;
    
    protected Glyph(ByteBuffer buffer,
                    int offset,
                    int dotsPerInch,
                    int unitsPerEm,
                    int pointSize) {
        this.buffer = buffer;
        this.offset = offset;
        this.dotsPerInch = dotsPerInch;
        this.unitsPerEm = unitsPerEm;
        this.pointSize = pointSize;
        
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
        return scale(bounds).getBounds2D();
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
    public abstract Path2D getPath();
    
    /**
     * Scales the given {@code Shape} from FUnit to device space coordinates.
     * 
     * @param shape The Shape to scale to device space.
     * 
     * @return A {@code Path2D} object which has been scaled using an affine
     *         transformation, according to the DPI, UPEM, and point size
     *         this Glyph was initialized with.
     */
    protected Path2D scale(Shape shape) {
        // The conversion ratio for FUnit -> device space coordinates.
        double dsc = dotsPerInch
                     * (1 / 72.d)
                     * pointSize
                     / unitsPerEm;
        AffineTransform trans = AffineTransform.getScaleInstance(dsc,
                                                                 -dsc);
        return (Path2D)trans.createTransformedShape(shape);
    }
    
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
        
        /**
         * A class which represents an (x,&nbsp;y) Cartesian coordinate and
         * that coordinate's associated flag. This simplifies the
         * representation of these elements during the curve construction
         * process and reduces the number of variables which must be kept track
         * of there.
         */
        private static final class Coordinate {
            private final int flag,
                              x,
                              y;
            
            Coordinate(int flag,
                       int x,
                       int y) {
                this.flag = flag;
                this.x = x;
                this.y = y;
            }
            
            boolean onCurve() {
                return (ON_CURVE_POINT & flag) > 0;
            }
            
            @Override
            public String toString() {
                return String.format("Coordinate: [flag=%s, x=%d, y=%d]%n",
                                     Integer.toBinaryString(flag),
                                     x,
                                     y);
            }
        }
        
        private final short numCoordinates;
        private final short[] endPoints;
        private Coordinate[] coords;
        private final byte[] flags,
                             instructions;
        
        public SimpleGlyph(ByteBuffer buffer,
                           int offset,
                           int dotsPerInch,
                           int unitsPerEm,
                           int pointSize) {
            super(buffer,
                  offset,
                  dotsPerInch,
                  unitsPerEm,
                  pointSize);
            
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
            
            short[] xCoords = readCoordinates(X_SHORT_VECTOR, X_DELTA);
            short[] yCoords = readCoordinates(Y_SHORT_VECTOR, Y_DELTA);
            
            coords = new Coordinate[numCoordinates];
            for (int i = 0; i < numCoordinates; i++)
                coords[i] = new Coordinate(flags[i],
                                           xCoords[i],
                                           yCoords[i]);
        }
        
        private short[] readCoordinates(int shortVector,
                                        int delta) {
            short[] coords = new short[numCoordinates];
            
            short val = 0;
            for (int i = 0; i < numCoordinates; i++) {
                byte flag = flags[i];
                if ((shortVector & flag) > 0) {
                    short sign = (short)Math.pow(-1,
                                                 ~((flag & delta)
                                                   >> (int)Math.sqrt(delta)));
                    val += (buffer.get() & 0xFF) * sign;
                }
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
        public Path2D getPath() {
            Path2D path = new Path2D.Float(Path2D.WIND_NON_ZERO);
            
            int point = 0;
            for (int contour = 0; contour < endPoints.length; contour++) {
                Coordinate start = coords[point],
                           last = start;
                
                path.moveTo(start.x, start.y);
                for (; point <= endPoints[contour]; point++) {
                    Coordinate curr = coords[point];
                    if (curr.onCurve()) {
                        if (last.onCurve()) path.lineTo(curr.x,
                                                        curr.y);
                        else path.quadTo(last.x,
                                         last.y,
                                         curr.x,
                                         curr.y);
                    }
                    else if (!last.onCurve())
                        path.quadTo(last.x,
                                    last.y,
                                    (last.x + curr.x) / 2.d,
                                    (last.y + curr.y) / 2.d);
                    
                    last = curr;
                }
                
                path.closePath();
            }
            
            return scale(path);
        }
    }
    
    public static class CompositeGlyph { /* TODO */ }
}