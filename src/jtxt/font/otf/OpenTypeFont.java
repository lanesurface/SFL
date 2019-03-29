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
package jtxt.font.otf;

import static java.awt.geom.AffineTransform.getTranslateInstance;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.RenderedImage;
import java.nio.file.Paths;

import javax.swing.JComponent;
import javax.swing.JFrame;

import jtxt.font.otf.loader.OTFFileReader;

/**
 * 
 */
public class OpenTypeFont {
    private OTFFileReader fontFile;
    private final String name;
    
    public OpenTypeFont(java.nio.file.Path path, String name) {
        fontFile = new OTFFileReader(path.toFile());
        this.name = name;
    }
    
    public OpenTypeFace createTypeFace(int size, int renderAttribs) {
        return new OpenTypeFace(this,
                                size,
                                renderAttribs);
    }
    
    public static class OpenTypeFace implements RasterFont,
                                                VectorFont {
        private final OpenTypeFont masterFont;
        private final int size,
                          renderAttribs,
                          dpi;
        
        public OpenTypeFace(OpenTypeFont masterFont,
                            int size,
                            int renderAttribs) {
            this.masterFont = masterFont;
            this.size = size;
            this.renderAttribs = renderAttribs;
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        }
        
        public GlyphRenderer createGlyphRenderer(Graphics2D graphics) {
            return new GlyphRenderer() {
                @Override
                public void drawGlyph(char character,
                                      int x,
                                      int y) {
                    AffineTransform trans = getTranslateInstance(x,
                                                                 y);
                    Path2D path = getGlyphPath(character, 0);
                    graphics.draw(trans.createTransformedShape(path));
                }

                @Override
                public void drawString(String string,
                                       int x,
                                       int y) {
                    // TODO
                }
            };
        }
        
        public Path2D getGlyphPath(char character, int hints) {
            return masterFont.fontFile.getGlyph(character,
                                                dpi,
                                                0).getPath(size);
        }
        
        @Override
        public RenderedImage getGlyph(char character,
                                      Color color,
                                      int xPad,
                                      int yPad,
                                      int hints) {
            
            return null;
        }
    }
    
    public static void main(String[] args) {
        OpenTypeFont font = new OpenTypeFont(Paths.get("C:",
                                                       "Windows",
                                                       "Fonts",
                                                       "TIMES.TTF"), null);
        OpenTypeFace face = font.createTypeFace(72, 0);
        JFrame frame = new JFrame("Font test");
        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.add(new JComponent() {
            @Override
            public void paint(Graphics g) {
                Graphics2D graphics = (Graphics2D)g;
                int width = getWidth(),
                    height = getHeight();
                
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0,
                                  0,
                                  width,
                                  height);
                
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                          RenderingHints.VALUE_ANTIALIAS_ON);
                
                graphics.setColor(Color.WHITE);
                GlyphRenderer renderer = face.createGlyphRenderer(graphics);
                renderer.drawGlyph('a',
                                   0,
                                   0);
            }
        });
    }
}
