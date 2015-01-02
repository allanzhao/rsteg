package com.bitwiseops.rsteg;

/**
 * Implements a two-dimensional bitfield.
 */
public class Bitfield2D {
    static final int BITS_PER_ELEMENT = 32;
    static final int BITS_PER_ELEMENT_SHIFT = 5;
    static final int BIT_INDEX_MASK = 0b11111;
    
    private final int width, height;
    final int widthInElements;
    final int[] data;
    
    public Bitfield2D(int width, int height) {
        this.width = width;
        this.height = height;
        this.widthInElements = widthInElements(this.width);
        this.data = new int[this.widthInElements * this.height];
    }
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Returns one if the bit at (<code>x</code>, <code>y</code>) is
     * set and zero otherwise.
     */
    public int getBit(int x, int y) {
        return (data[posToIndex(x, y)] & (1 << (x & BIT_INDEX_MASK))) >>> (x & BIT_INDEX_MASK);
    }
    
    /**
     * Sets the bit at (<code>x</code>, <code>y</code>) to one if
     * <code>value</code> is nonzero and zero otherwise.
     */
    public void setBit(int x, int y, int value) {
        int index = posToIndex(x, y);
        int mask = 1 << (x & BIT_INDEX_MASK);
        if(value != 0) {
            data[index] |= mask;
        } else {
            data[index] &= ~mask;
        }
    }
    
    /**
     * Returns <code>length</code> bits in the direction of increasing x, 
     * starting at (<code>x</code>, <code>y</code>). The result is in 
     * little-endian order, with the bit at (<code>x</code>, <code>y</code>)
     * in the least significant bit.
     */
    public int getBits(int x, int y, int length) {
        int index = posToIndex(x, y);
        int bitIndex = x & BIT_INDEX_MASK;
        int mask = (length < BITS_PER_ELEMENT) ? (1 << length) - 1 : -1;
        int mask0 = mask << bitIndex;
        int mask1 = (bitIndex != 0) ? mask >>> (BITS_PER_ELEMENT - bitIndex) : 0;
        int result = (data[index] & mask0) >>> bitIndex;
        if(mask1 != 0) {
            result |= (data[index + 1] & mask1) << (BITS_PER_ELEMENT - bitIndex);
        }
        return result;
    }
    
    /**
     * Sets <code>length</code> bits to <code>value</code> in the direction of
     * increasing x, starting at (<code>x</code>, <code>y</code>).
     * <code>value</code> is interpreted as being in little-endian order, with
     * the least significant bit stored to (<code>x</code>, <code>y</code>).
     */
    public void setBits(int x, int y, int length, int value) {
        int index = posToIndex(x, y);
        int bitIndex = x & BIT_INDEX_MASK;
        int mask = (length < BITS_PER_ELEMENT) ? (1 << length) - 1 : -1;
        int mask0 = mask << bitIndex;
        int mask1 = (bitIndex != 0) ? mask >>> (BITS_PER_ELEMENT - bitIndex) : 0;
        data[index] = (data[index] & ~mask0) | ((value << bitIndex) & mask0);
        if(mask1 != 0) {
            data[index + 1] = (data[index + 1] & ~mask1) | ((value >>> (BITS_PER_ELEMENT - bitIndex)) & mask1);
        }
    }
    
    /**
     * Similar behavior to <code>getBits(int x, int y, int length)</code>, but
     * "wraps" to the next row every <code>wrap</code> bits.
     */
    public int getBits(int x, int y, int length, int wrap) {
        int rows = (length - 1) / wrap + 1;
        int result = 0;
        for(int iy = 0; iy < rows; iy++) {
            int sublength;
            if(iy < rows - 1) {
                sublength = wrap;
            } else {
                sublength = length % wrap;
                if(sublength == 0) {
                    sublength = wrap;
                }
            }
            result |= getBits(x, y + iy, sublength) << (iy * wrap);
        }
        return result;
    }
    
    /**
     * Similar behavior to <code>setBits(int x, int y, int length, int value)</code>, but
     * "wraps" to the next row every <code>wrap</code> bits.
     */
    public void setBits(int x, int y, int length, int value, int wrap) {
        int rows = (length - 1) / wrap + 1;
        for(int iy = 0; iy < rows; iy++) {
            int sublength;
            if(iy < rows - 1) {
                sublength = wrap;
            } else {
                sublength = length % wrap;
                if(sublength == 0) {
                    sublength = wrap;
                }
            }
            setBits(x, y + iy, sublength, value >>> (iy * wrap));
        }
    }
    
    /**
     * Copies a <code>width</code> by <code>height</code> sized block of bits
     * from <code>src</code> to this bitfield.
     * (<code>srcX</code>, <code>srcY</code>), (<code>dstX</code>, <code>dstY
     * </code>) specify the block's top left corner in <code>src</code> and
     * this bitfield, respectively.
     */
    public void copyBits(Bitfield2D src, int srcX, int srcY, int dstX, int dstY, int width, int height) {
        int widthElements = MathUtils.ceilDivide(width, BITS_PER_ELEMENT);
        for(int iy = 0; iy < height; iy++) {
            for(int ix = 0; ix < widthElements; ix++) {
                int sublength;
                if(ix < widthElements - 1) {
                    sublength = BITS_PER_ELEMENT;
                } else {
                    sublength = width & BIT_INDEX_MASK;
                    if(sublength == 0) sublength = BITS_PER_ELEMENT;
                }
                setBits(dstX + (ix << BITS_PER_ELEMENT_SHIFT), dstY + iy, sublength, src.getBits(srcX + (ix << BITS_PER_ELEMENT_SHIFT), srcY + iy, sublength));
            }
        }
    }
    
    int posToIndex(int x, int y) {
        return y * widthInElements + (x >>> BITS_PER_ELEMENT_SHIFT);
    }
    
    private static int widthInElements(int x) {
        return ((x - 1) >>> BITS_PER_ELEMENT_SHIFT) + 1;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                sb.append(getBit(x, y) != 0 ? '1' : '0');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
