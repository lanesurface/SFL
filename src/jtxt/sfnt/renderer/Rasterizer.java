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
package jtxt.sfnt.renderer;

import java.awt.geom.PathIterator;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * 
 */
public class Rasterizer {
    enum AAMode { OFF,
                  GRAY,
                  RGB,
                  BGR }
    
    private final int width,
                      height;
    private final WritableRaster raster;
    private final AAMode aaMode;
    
    public Rasterizer(int width,
                      int height,
                      AAMode aaMode) {
        this.width = width;
        this.height = height;
        this.aaMode = aaMode;
        raster = Raster.createBandedRaster(DataBuffer.TYPE_INT,
                                           width,
                                           height,
                                           3,
                                           null);
    }
    
    public void rasterize(PathIterator path,
                          int xOff,
                          int yOff) {
        float[] coords = new float[6];
        while (!path.isDone()) {
            switch (path.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                break;
            case PathIterator.SEG_LINETO:
                drawLine(0,
                         0,
                         0,
                         0);
                
                break;
            default:
                break;
            }
        }
    }
    
    private void drawLine(int x0,
                          int y0,
                          int x1,
                          int y1) {
        /*
         * Use a simple DDA method for computing the pixel coordinates of
         * this line.
         */
        
        int dX = x1 - x0,
            dY = y1 - y0,
            steps = Math.max(Math.abs(dX), Math.abs(dY));
        
        double xDelta = dX / steps,
               yDelta = dY / steps;
        for (int i = 0; i < steps; i++)
            raster.setPixel((int)(x0 * xDelta),
                            (int)(y0 * yDelta),
                            new int[] { 0, 0, 0 });
    }
}
