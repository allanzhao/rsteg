package com.bitwiseops.rsteg;

import java.util.Random;

public final class RStegTestUtils {
    private RStegTestUtils() {}
    
    public static void corruptRandomBits(Bitfield2D bitfield, int numErrors) {
        Random random = new Random();
        for(int i = 0; i < numErrors; i++) {
            int x = random.nextInt(bitfield.getWidth());
            int y = random.nextInt(bitfield.getWidth());
            bitfield.setBit(x, y, random.nextInt(2));
        }
    }
    
    public static void corruptRandomBlocks(Bitfield2D bitfield, int numErrors, int blockSize) {
        Random random = new Random();
        for(int i = 0; i < numErrors; i++) {
            int x = random.nextInt(bitfield.getWidth() - blockSize + 1);
            int y = random.nextInt(bitfield.getHeight() - blockSize + 1);
            for(int row = 0; row < blockSize; row++) {
                bitfield.setBits(x, y + row, blockSize, random.nextInt());
            }
        }
    }
    
    public static Bitfield2D crop(Bitfield2D bitfield, int left, int right, int top, int bottom) {
        Bitfield2D croppedBitfield = new Bitfield2D(bitfield.getWidth() - left - right, bitfield.getHeight() - top - bottom);
        croppedBitfield.copyBits(bitfield, left, top, 0, 0, croppedBitfield.getWidth(), croppedBitfield.getHeight());
        return croppedBitfield;
    }
    
    /**
     * Verify that <code>field</code> is actually a finite field
     */
    public static boolean checkFiniteField(Field field, int maxErrors) {
        int errors = 0;
        int size = field.getSize();
        
        // Check table-based multiplication (GFPow2 only)
        if(field instanceof GFPow2) {
            GFPow2 gfPow2 = (GFPow2)field;
            for(int y = 0; y < size; y++) {
                for(int x = 0; x < size; x++) {
                    int mul = gfPow2.mul(x, y);
                    int slowMul = gfPow2.slowMul(x, y);
                    if(mul != slowMul) {
                        System.out.println(String.format("Multiplication error! 0x%02x*0x%02x: 0x%02x != 0x%02x", x, y, mul, slowMul));
                        errors++;
                        if(errors >= maxErrors) {
                            return false;
                        }
                    }
                }
            }
        }
        
        // Check division
        for(int y = 1; y < size; y++) {// don't divide by zero
            for(int x = 0; x < size; x++) {
                int q = field.div(x, y);
                int p = field.mul(q, y);
                if(x != p) {
                    System.out.println(String.format("Division error! 0x%02x/0x%02x: 0x%02x != 0x%02x", x, y, p, x));
                    errors++;
                    if(errors >= maxErrors) {
                        return false;
                    }
                }
            }
        }
        
        // Check finite-fieldness?
        for(int y = 1; y < size; y++) {// multiplying by zero every time isn't interesting
            boolean[] hit = new boolean[size];
            
            for(int x = 0; x < size; x++) {
                int p = field.mul(x, y);
                hit[p] = true;
            }
            
            for(int i = 0; i < size; i++) {
                if(!hit[i]) {
                    System.out.println("Not a finite field.");
                    errors++;
                    if(errors >= maxErrors) {
                        return false;
                    }
                    break;
                }
            }
        }
        
        return true;
    }
}
