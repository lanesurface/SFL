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
        raster = Raster.createPackedRaster(DataBuffer.TYPE_INT,
                                           width,
                                           height,
                                           4,
                                           8,
                                           null);
        clear();
    }
    
    public void clear() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                raster.setDataElements(x, y, new int[] { 255,
                                                         255,
                                                         255,
                                                         255 });
    }
    
    public void rasterize(PathIterator path,
                          int xOff,
                          int yOff) {
        float[] coords = new float[6];
        
        float startX = xOff,
              startY = yOff,
              lastX = startX,
              lastY = startY;
        while (!path.isDone()) {
            switch (path.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                lastX = coords[0];
                lastY = coords[1];
                
                break;
            case PathIterator.SEG_LINETO:
                float currX = coords[0],
                      currY = coords[1];
                drawLine(lastX,
                         lastY,
                         coords[0],
                         coords[1]);
                
                lastX = currX;
                lastY = currY;
                
                break;
            case PathIterator.SEG_QUADTO:
                float ctrlX = coords[0],
                      ctrlY = coords[1],
                      endX = coords[2],
                      endY = coords[3];
                
                /*
                 * In the future, I will add the mechanism for subdivision of
                 * these curves by this rasterization engine; for now, I will
                 * assume the steepness of the curve is relatively consistent.
                 */
                drawQuadCurve(lastX,
                              lastY,
                              ctrlX,
                              ctrlY,
                              endX,
                              endY,
                              10);
                
                lastX = endX;
                lastY = endY;
                
                break;
            case PathIterator.SEG_CUBICTO: break; // TODO
            case PathIterator.SEG_CLOSE:
                drawLine(lastX,
                         lastY,
                         startX,
                         startY);
                
                /*
                 * Now that we've closed this Shape, the last coordinates are
                 * obviously invalid; however, there should never be a case
                 * where they are used after a SEG_CLOSE operation and a 
                 * MOVE_TO does not follow.
                 */
                
                break;
            }
        }
    }
    
    private void drawLine(float x0,
                          float y0,
                          float x1,
                          float y1) {
        /*
         * Use a simple DDA method for computing the pixel coordinates of
         * this line.
         */
        
        float dX = x1 - x0,
              dY = y1 - y0,
              steps = Math.max(Math.abs(dX), Math.abs(dY));
        double xDelta = dX / steps,
               yDelta = dY / steps;
        
        int[] pixColor = new int[] { 0,
                                     0,
                                     0,
                                     0 };
        for (int i = 0; i < steps; i++)
            raster.setPixel((int)(x0 * xDelta),
                            (int)(y0 * yDelta),
                            pixColor);
    }
    
    private void drawQuadCurve(float startX,
                               float startY,
                               float ctrlX,
                               float ctrlY,
                               float endX,
                               float endY,
                               int subdivisions) {
        float lastX = startX,
              lastY = startY;
        
        double ti = 1.d / subdivisions;
        for (double t = 0.d; t <= 1; t += ti) {
            float x = calcIntermediate(startX,
                                       ctrlX,
                                       endX,
                                       t),
                  y = calcIntermediate(startY,
                                       ctrlY,
                                       endY,
                                       t);
            
            drawLine(lastX,
                     lastY,
                     x,
                     y);
            
            lastX = x;
            lastY = y;
        }
    }
    
    private float calcIntermediate(float start,
                                   float ctrl,
                                   float end,
                                   double t) {
        /*
         * Formula for intermediate point on quadratic Bézier curve is:
         *   B(t) = (1 - t)^2 * P0
         *          + 2(1 - t) * P1
         *          + t^2 * P2
         *   (where 0 <= t <= 1).
         */
        
        return (float)(Math.pow(1 - t, 2) * start
                       + 2 * (1 - t) * ctrl
                       + Math.pow(t, 2) * end);
    }
}
