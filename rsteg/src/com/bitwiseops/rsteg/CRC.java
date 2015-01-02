package com.bitwiseops.rsteg;

/**
 * Implements a CRC (cyclic redundancy check) of degree <= 32.
 */
public class CRC {
    private final int revPoly;
    private int value;
    
    /**
     * Constructs a CRC with a degree specified by the location of the most
     * significant one bit in the defining polynomial <code>poly</code>.
     */
    public CRC(int poly) {
        int degree = Integer.numberOfTrailingZeros(Integer.highestOneBit(poly));
        revPoly = Integer.reverse(poly) >>> (32 - degree);
        reset();
    }
    
    /**
     * Constructs a CRC with an explicit degree <code>degree</code> and a
     * defining polynomial <code>poly</code>. The leading coefficient can be
     * omitted from <code>poly</code>, enabling construction of a 32 bit CRC.
     */
    public CRC(int poly, int degree) {
        revPoly = Integer.reverse(poly) >>> (32 - degree);
        reset();
    }
    
    /**
     * Returns the current value.
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Resets the current value.
     */
    public void reset() {
        value = 0;
    }
    
    /**
     * Updates the CRC with the given byte and returns the new value.
     */
    public int update(byte d) {
        value ^= d & 0xff;
        for(int j = 0; j < 8; j++) {
            if((value & 1) != 0) {
                value >>>= 1;
                value ^= revPoly;
            } else {
                value >>>= 1;
            }
        }
        return value;
    }
    
    /**
     * Updates the CRC with the given int and returns the new value.
     */
    public int updateWithInt(int d) {
        update((byte)d);
        update((byte)(d >>> 8));
        update((byte)(d >>> 16));
        return update((byte)(d >>> 24));
    }
    
    /**
     * Updates the CRC with the given bytes and returns the new value.
     */
    public int update(byte[] data, int offset, int length) {
        for(int i = 0; i < length; i++) {
            value ^= data[i + offset] & 0xff;
            for(int j = 0; j < 8; j++) {
                if((value & 1) != 0) {
                    value >>>= 1;
                    value ^= revPoly;
                } else {
                    value >>>= 1;
                }
            }
        }
        return value;
    }
}
