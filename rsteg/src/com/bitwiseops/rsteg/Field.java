package com.bitwiseops.rsteg;

/**
 * Represents a finite field.
 */
public interface Field {
    /**
     * Adds two elements in this field.
     */
    public int add(int x, int y);
    
    /**
     * Subtracts y from x in this field.
     */
    public int sub(int x, int y);
    
    /**
     * Multiplies two elements in this field.
     */
    public int mul(int x, int y);
    
    /**
     * Divides x by y in this field.
     */
    public int div(int x, int y);
    
    /**
     * Returns the additive inverse of x in this field.
     */
    public int negate(int x);
    
    /**
     * Returns the multiplicative inverse (reciprocal) of x in this field.
     */
    public int reciprocal(int x);
    
    /**
     * Raises x to the power of p in this field.
     */
    public int pow(int x, int p);
    
    /**
     * Returns the number of elements in this field.
     */
    public int getSize();
}
