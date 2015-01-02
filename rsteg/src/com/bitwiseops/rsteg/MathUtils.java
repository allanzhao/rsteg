package com.bitwiseops.rsteg;

public final class MathUtils {
    private MathUtils() {}
    
    public static int ceilDivide(int n, int d) {
        return (n + d - 1) / d;
    }
}
