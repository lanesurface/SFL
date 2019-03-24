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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
    private CharacterMapper cmapper;
    private OTFFileReader fontFile;
    private final String name;
    
    public OpenTypeFont(java.nio.file.Path path, String name) {
        fontFile = new OTFFileReader(path.toFile());
        cmapper = fontFile.createCharacterMapper();
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
        private final int size;
        private final int renderAttribs;
        
        public OpenTypeFace(OpenTypeFont masterFont,
                            int size,
                            int renderAttribs) {
            this.masterFont = masterFont;
            this.size = size;
            this.renderAttribs = renderAttribs;
        }
        
        public Path2D getGlyphPath(char character, int hints) {
            return masterFont.fontFile.getGlyph(character,
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
                                                       "CALIBRI.TTF"), null);
        Path2D path = font.createTypeFace(72, 0).getGlyphPath('N',
                                                              0);
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
                
                graphics.setColor(Color.WHITE);
                graphics.translate(width / 2,
                                   height / 2);
                graphics.draw(path);
            }
        });
    }
}
