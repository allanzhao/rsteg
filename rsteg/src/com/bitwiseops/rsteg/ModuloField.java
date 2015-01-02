package com.bitwiseops.rsteg;

/**
 * Implements a Galois field with a prime number of elements. This class is
 * meant for debugging use.
 */
public class ModuloField implements Field {
    private final int modulus;
    
    public ModuloField(int modulus) {
        this.modulus = modulus;
    }
    
    @Override
    public int add(int x, int y) {
        return (x + y) % modulus;
    }

    @Override
    public int sub(int x, int y) {
        return ((x - y) % modulus + modulus) % modulus;
    }

    @Override
    public int mul(int x, int y) {
        return x * y % modulus;
    }

    @Override
    public int div(int x, int y) {
        if(y == 0) {
            throw new ArithmeticException("Division by zero.");
        }
        return x * reciprocal(y) % modulus;
    }

    @Override
    public int negate(int x) {
        return (modulus - x) % modulus;
    }

    @Override
    public int reciprocal(int x) {
        if(x == 0) {
            throw new IllegalArgumentException("Zero does not have a reciprocal.");
        }
        return egcd(x, modulus)[1];
    }

    @Override
    public int pow(int x, int p) {
        int result = 1;
        while(p > 0) {
            if((p & 1) != 0) {
                result = result * x % modulus;
            }
            p >>>= 1;
            x = x * x % modulus;
        }
        return result;
    }

    private int[] egcd(int x, int y) {
        // Plenty of room for improvement
        if(y == 0) {
            return new int[]{x, 1, 0};
        } else {
            int[] result = egcd(y, x % y);
            return new int[]{result[0], result[2], ((result[1] - x / y * result[2]) % modulus + modulus) % modulus};
        }
    }

    @Override
    public int getSize() {
        return modulus;
    }
}
