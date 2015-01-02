package com.bitwiseops.rsteg;

public final class LinearAlgebraUtils {
    private LinearAlgebraUtils() {}
    
    /**
     * Performs Gauss-Jordan reduction on a matrix using field arithmetic.
     */
    public static void rowReduce(int[][] matrix, Field field) {
        int width = matrix[0].length;
        int height = matrix.length;
        if(width != height + 1) {
            throw new IllegalArgumentException("Matrix is not in the proper form.");
        }
        
        // Elimination step
        for(int column = 0; column < height; column++) {
            //Ensure that the element of this column in the diagonal is nonzero
            if(matrix[column][column] == 0) {
                for(int row = column + 1; row < height; row++) {
                    if(matrix[row][column] != 0) {
                        rowSwap(matrix, column, row);
                        break;
                    }
                }
            }
            
            int diagElement = matrix[column][column];
            if(diagElement != 0) {
                rowScale(matrix, column, field.reciprocal(diagElement), field);
                
                for(int row = column + 1; row < height; row++) {
                    rowAddMultiple(matrix, row, column, field.negate(matrix[row][column]), field);
                }
            }
        }
        
        // Back-substitution step
        for(int column = height - 1; column > 0; column--) {
            for(int row = 0; row < column; row++) {
                rowAddMultiple(matrix, row, column, field.negate(matrix[row][column]), field);
            }
        }
    }
    
    /**
     * Swaps rows <code>ra</code> and <code>rb</code>.
     */
    public static void rowSwap(int[][] matrix, int ra, int rb) {
        int[] temp = matrix[ra];
        matrix[ra] = matrix[rb];
        matrix[rb] = temp;
    }
    
    /**
     * Multiplies every element in row <code>r</code> by a scalar <code>s</code>.
     */
    public static void rowScale(int[][] matrix, int r, int s, Field field) {
        int width = matrix[0].length;
        for(int column = 0; column < width; column++) {
            matrix[r][column] = field.mul(matrix[r][column], s);
        }
    }
    
    /**
     * Adds <code>s</code> times row <code>rs</code> to row <code>rd</code>.
     */
    public static void rowAddMultiple(int[][] matrix, int rd, int rs, int s, Field field) {
        int width = matrix[0].length;
        for(int column = 0; column < width; column++) {
            matrix[rd][column] = field.add(matrix[rd][column], field.mul(matrix[rs][column], s));
        }
    }
}
