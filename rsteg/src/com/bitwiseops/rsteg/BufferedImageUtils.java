package com.bitwiseops.rsteg;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public final class BufferedImageUtils {
    private BufferedImageUtils() {}
    
    public static void putBitplane(BufferedImage bufferedImage, int band, int bitIndex, Bitfield2D bitfield) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        if(width != bitfield.getWidth() || height != bitfield.getHeight()) {
            throw new IllegalArgumentException("Mismatched image and bitfield dimensions.");
        }
        
        WritableRaster raster = bufferedImage.getRaster();
        int sampleArraySize = raster.getNumDataElements() * width * height;
        
        switch(bufferedImage.getType()) {
        case BufferedImage.TYPE_INT_ARGB:
            int[] sampleArray = new int[sampleArraySize];
            raster.getDataElements(0, 0, width, height, sampleArray);
            int shiftAmount = 24 - 8 * band + bitIndex;
            int mask = 1 << shiftAmount;
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x += Bitfield2D.BITS_PER_ELEMENT) {
                    int element = bitfield.data[bitfield.posToIndex(x, y)];
                    int baseIndex = y * width + x;
                    for(int xi = 0; xi < Bitfield2D.BITS_PER_ELEMENT && x + xi < width; xi++) {
                        sampleArray[baseIndex + xi] = (sampleArray[baseIndex + xi] & ~mask)
                                | (((element >>> xi) << shiftAmount) & mask);
                    }
                }
            }
            raster.setDataElements(0, 0, width, height, sampleArray);
            break;
        default:
            throw new UnsupportedOperationException("Image type not supported.");
        }
    }
    
    public static void getBitplane(BufferedImage bufferedImage, int band, int bitIndex, Bitfield2D bitfield) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        if(width != bitfield.getWidth() || height != bitfield.getHeight()) {
            throw new IllegalArgumentException("Mismatched image and bitfield dimensions.");
        }
        
        WritableRaster raster = bufferedImage.getRaster();
        int sampleArraySize = raster.getNumDataElements() * width * height;
        
        switch(bufferedImage.getType()) {
        case BufferedImage.TYPE_INT_ARGB:
            int[] sampleArray = new int[sampleArraySize];
            raster.getDataElements(0, 0, width, height, sampleArray);
            int shiftAmount = 24 - 8 * band + bitIndex;
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x += Bitfield2D.BITS_PER_ELEMENT) {
                    int element = 0;
                    int baseIndex = y * width + x;
                    for(int xi = 0; xi < Bitfield2D.BITS_PER_ELEMENT && x + xi < width; xi++) {
                        element |= ((sampleArray[baseIndex + xi] >>> shiftAmount) & 1) << xi;
                    }
                    bitfield.data[bitfield.posToIndex(x, y)] = element;
                }
            }
            break;
        default:
            throw new UnsupportedOperationException("Image type not supported.");
        }
    }
}
