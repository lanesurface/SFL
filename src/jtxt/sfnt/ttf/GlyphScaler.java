/*
 * Copyright 2019 Lane W. Surface
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package jtxt.sfnt.ttf;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

import jtxt.sfnt.ttf.parser.Glyph;

/**
 * 
 */
public class GlyphScaler {
    public final int dpi,
                     pointSize,
                     unitsPerEm;
    private final double dsc;

    public GlyphScaler(int dpi, int pointSize, int unitsPerEm) {
        this.dpi = dpi;
        this.pointSize = pointSize;
        this.unitsPerEm = unitsPerEm;
        // The conversion ratio for FUnit -> device space coordinates.
        dsc = dpi
              * (1 / 72.d)
              * pointSize
              / unitsPerEm;
    }

    /**
     * Scales the given {@code Glyph} from FUnit to device space coordinates.
     * 
     * @param shape The Glyph to scale to device space.
     * 
     * @return A {@code Path2D} object which has been scaled using an affine
     *         transformation, according to the DPI, UPEM, and point size this
     *         Scaler was initialized with.
     */
    public Path2D scale(Glyph glyph) {
        return scale(glyph.getPath());
    }

    public Path2D scale(Shape shape) {
        AffineTransform trans = AffineTransform.getScaleInstance(dsc,
                                                                 -dsc);
        return (Path2D)trans.createTransformedShape(shape);
    }
}
