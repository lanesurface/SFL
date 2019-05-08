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
package jtxt.sfnt.ttf;

import static java.awt.geom.AffineTransform.getTranslateInstance;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JComponent;
import javax.swing.JFrame;

import jtxt.sfnt.ttf.parser.Glyph;
import jtxt.sfnt.ttf.parser.Metrics;
import jtxt.sfnt.ttf.parser.OTFFileReader;

/**
 * 
 */
public class OpenTypeFont implements RasterFont, VectorFont {
    /*
     * These attributes provide additional information to the parser which
     * allows it to make determinations based on the style which the text
     * will be rendered in.
     */
    public static final int PLAIN   = 0,
                            BOLD    = 1 << 0,
                            ITALLIC = 1 << 1,
                            OBLIQUE = 1 << 2;
    
    private OTFFileReader fontFile;
    private final Metrics metrics;
    private final GlyphScaler scaler;
    private final int size,
                      attributes,
                      dpi;
    
    public OpenTypeFont(Path path, int size, int attributes) {
        fontFile = new OTFFileReader(path.toFile());
        this.size = size;
        this.attributes = attributes;
        dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        metrics = fontFile.getMetrics(size, dpi);
        scaler = new GlyphScaler(dpi, size, fontFile.getUPEM());
    }
    
    // Temporary rendering mechanism for testing font parsing.
    public GlyphRenderer createGlyphRenderer(Graphics2D graphics) {
        return new GlyphRenderer() {
            @Override
            public void draw(char character, int x, int y) {
                Glyph glyph = fontFile.getGlyph(character);
                drawPath(scaler.scale(glyph), x, y);
            }

            @Override
            public void draw(String string, int x, int y) {
                int xOff = x;
                for (int i = 0; i < string.length(); i++) {
                    char chr = string.charAt(i);
                    Glyph glyph = fontFile.getGlyph(chr);
                    drawPath(scaler.scale(glyph), xOff, y);
                    xOff += scaler.scale(glyph.getBounds())
                        .getBounds()
                        .getWidth();
                    
//                    xOff += metrics.getAdvanceWidth(g);
                }
            }
            
            private void drawPath(Path2D path, int x, int y) {
                AffineTransform trans = getTranslateInstance(x,
                                                             y);
                graphics.fill(trans.createTransformedShape(path));
            }
        };
    }
    
    @Override
    public RenderedImage getGlyph(char character,
                                  Color color,
                                  int xPad,
                                  int yPad,
                                  int hints) { return null; }
    
    public static void main(String[] args) {
        Path path = Paths.get("C:",
                              "Windows",
                              "Fonts",
                              "TIMES.TTF");
        OpenTypeFont font = new OpenTypeFont(path, 12, PLAIN);
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
                
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0,
                                  0,
                                  width,
                                  height);
                
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                          RenderingHints.VALUE_ANTIALIAS_ON);
                
                graphics.setColor(Color.BLACK);
                GlyphRenderer renderer = font.createGlyphRenderer(graphics);
                renderer.draw("This is a sentence rendered in my font " +
                              "parser!",
                              0,
                              height / 2);
            }
        });
    }
}
