## SFnt Library
The 'sfnt' format is a table-based font storage format originally defined by Apple
for the Macintosh computer system and later extended by Microsoft and Adobe, 
renamed OpenType, for the inclusion of advanced typographic features and support
for CFF PostScript data. 

This library aims to implement a parsing mechanism for SFNT data. The uses of this
are similar to the popular FreeType library used by many devices to display
scalable fonts. Java itself, under the hood, uses FreeType for some of it's
font technology. The issue with using Java's built-in font parser is the
reliance on Java2D (and primarily the device-dependent `Graphics` implementation)

### On the Use of this Library in Font Display Technologies
Because rendering characteristics are so variable, and the facilities provided by
Java are poorly equipped for this purpose, the actual implementation of a rasterizer
has been left up to the client's disgression. That is to say that this library
primarily concerns itself with the extraction and interpretation of font data, not
with the actual display of it. Though the outline data is quite complex in nature,
the format in which it is stored after parsing should not be difficult to
understand, and thus it should be quite easy for a person knowlegable in computer
graphics technology to render these glyphs without much effort on their part. It is
the intention of this library to be unassuming about the goals of an individual
client and therefore flexible in its applications.