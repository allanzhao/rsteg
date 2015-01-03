RSteg - Robust steganography tool
=================================

Like other steganography programs, RSteg can hide a message in an image and reveal it again. The message can still be extracted if the carrier image is subjected to minor editing such as drawing. RSteg's primary feature, however, is the ability to resist dimensional changes such as image cropping and padding without losing the message.

RSteg is only robust in the sense that it can decode carrier images that have been resized, unlike many other steganography programs. Bugs may (and probably will) be present, and no effort is made to hide the message from more than casual observation.

A brief overview of the encoding and decoding process is given below. If you want to find out the details, take a look at [RStegCodec.java](https://github.com/allanzhao/rsteg/blob/master/rsteg/src/com/bitwiseops/rsteg/RStegCodec.java).

Command line arguments are parsed with the [argparse4j](https://github.com/tatsuhiro-t/argparse4j) library.

Example command line usage
--------------------------

    $ java -jar RSteg.jar encode cover_image.png output_image.png -m "Hello world!"
    $ java -jar RSteg.jar decode output_image.png
    Hello world!

Goals
-----

1. Must resist modifications within the image (drawing, adding text)
2. Must resist cropping
3. Must resist padding or addition of a border
4. If the image is too damaged to recover the message, fail gracefully

Techniques used
---------------

RSteg uses a number of techniques to achieve these goals:

* Reed-Solomon error correction
* Cyclic redundancy check (CRC)
* Interleaving
* Randomization

Some (non-technical) terminology used in the code:

* *tile*: 4x4 group of pixels, containing a 12-bit payload and a 4-bit coordinate within its patch (for alignment purposes)
* *patch*: 4x4 group of tiles, containing 14 data tiles and 2 tiles of metadata

##### Reed-Solomon error correction

RSteg splits the message into fixed-size packets and encodes them into Reed-Solomon codewords. The codewords are always a fixed length (256), but the number of message symbols per codeword can be selected. This permits a tradeoff between error correction capability and data capacity. The decoder marks all missing or incomplete tiles as well as many corrupted tiles as erased symbols.

##### Cyclic redundancy check (CRC)

The metadata in each patch (4x4 group of tiles) is protected by an 8-bit CRC. The contents of the patch are invalidated if the CRC is incorrect. As a final check, the message itself is appended with a 32-bit CRC.

##### Interleaving

Each patch contains symbols from 14 different codewords, and the patches are randomly scattered across the image to avoid placing symbols from the same codeword near each other. This way, localized damage should not affect a single codeword too much.

##### Randomization

As mentioned before, patches are randomly scattered to reduce the probability of a single codeword being damaged excessively. Randomization is also used to ensure that every valid tile has even parity. If a tile would have odd parity, one of its index bits is randomly chosen and flipped. This does not affect the decodability of the image excessively and saves a bit in every tile. Enforcing even parity is essential to the first decoding stage.

Encoding process overview
-------------------------

1. Prepend the message bytes with a size word, and append a CRC checksum to create the augmented message.
2. Split the augmented message into fixed size packets.
3. Encode each packet into a Reed-Solomon codeword.
4. Assign the symbols in each codeword to patches (ensuring that symbols in each patch are from different codewords).
5. Number each patch sequentially and give each patch a copy of the metadata. The patch number and metadata are protected with an 8-bit CRC.
6. Randomly scatter the patches throughout the image.
7. Pack the data in each patch into tiles, each with its 4 bit coordinate within the patch.
8. Flip one of the coordinate bits in each tile to ensure even parity, if necessary.
9. Write the tiles to the carrier image in their correct positions.

Decoding process overview
-------------------------

1. Detect the alignment of tiles and patches within the image.
2. Read the tiles from the carrier image, assigning valid ones to the appropriate codewords.
3. Decode the Reed-Solomon codewords into packets.
4. Concatenate the packets to form a decoded message.
5. Verify the message checksum.

Step 1 (detecting alignment) is accomplished using the 4 bits of coordinate information in each tile. The carrier image may be cropped or padded, so the image to be decoded may have incomplete tiles on the edges. Moreover, the patches in the image may have any of 16x16=256 alignments. The first stage of decoding involves determining which of these 256 alignments is the correct one.

Knowing that valid tiles have even parity, the location of the first whole tile in the top-left corner of the image can be determined. For each of the 16 possible locations, the number of tiles with even parity (assuming that location possibility) is counted. The location possibility with the greatest number of even-parity tiles is selected.

A similar method is used to determine the location of this top-left tile within its patch. This time, however, the 4-bit indices in each tile are used. Some of these indices will be incorrect due to the parity adjustment and image modification, but the correct alignment should still prevail.

Combining these two substeps gives the most likely alignment of tiles and patches within the image.

Future enhancements
-------------------

There is currently no way to tell the Reed-Solomon decoder which symbols are erased, so an arbitrary value is substituted instead. Handling known erasures differently from general errors would allow for a greater error tolerance.
