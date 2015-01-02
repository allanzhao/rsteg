package com.bitwiseops.rsteg;

/**
 * Implements a Galois field with 2^n elements.
 * Credit goes to http://cdstahl.org/?p=110 and http://web.eecs.utk.edu/~plank/plank/papers/CS-07-593/
 * for an explanation of the algorithms.
 */
public class GFPow2 implements Field {
    private final int poly;// Irreducible polynomial defining the field
    private final int size;
    private final int degree;
    private final int tableModulus;
    private final int[] logs;
    private final int[] antilogs;
    
    public GFPow2(int poly, int genPoly) {
        this.poly = poly;
        
        size = Integer.highestOneBit(poly);
        degree = Integer.numberOfTrailingZeros(size);
        tableModulus = size - 1;
        
        logs = new int[size];
        antilogs = new int[size];
        int x = 1;
        for(int i = 1; i < size; i++) {
            x = slowMul(x, genPoly);
            logs[x] = i;
            antilogs[i] = x;
        }
        logs[1] = 0;
        antilogs[0] = 1;
    }
    
    public GFPow2(int poly) {
        this(poly, Integer.highestOneBit(poly) - 1);
    }
    
    @Override
    public int add(int x, int y) {
        return x ^ y;
    }
    
    @Override
    public int sub(int x, int y) {
        return x ^ y;
    }
    
    @Override
    public int mul(int x, int y) {
        if(x == 0 || y == 0) {
            return 0;
        } else {
            return antilogs[(logs[x] + logs[y]) % tableModulus];
        }
    }
    
    @Override
    public int div(int x, int y) {
        if(x == 0) {
            return 0;
        } else if(y == 0) {
            throw new ArithmeticException("Division by zero.");
        } else {
            return antilogs[(logs[x] - logs[y] + tableModulus) % tableModulus];
        }
    }
    
    @Override
    public int negate(int x) {
        return x;
    }
    
    @Override
    public int reciprocal(int x) {
        if(x == 0) {
            throw new IllegalArgumentException("Zero does not have a reciprocal.");
        }
        return antilogs[tableModulus - logs[x]];
    }
    
    @Override
    public int pow(int x, int p) {
        if(p == 0) {
            return 1;
        } else if(x == 0) {
            return 0;
        }
        return antilogs[logs[x] * p % tableModulus];
    }

    public int slowMul(int a, int b) {
        int p = 0;
        for(int i = 0; i < degree; i++) {
            if((b & 1) != 0) {
                p ^= a;
            }
            a <<= 1;
            if((a & size) != 0) {// carry occurred
                a ^= poly;
            }
            b >>>= 1;
        }
        return p;
    }

    @Override
    public int getSize() {
        return size;
    }
}
