package com.bitwiseops.rsteg;

/**
 * Implements a Reed-Solomon code with general error correction capability.
 */
public class ReedSolomon {
    private final Field field;
    private int messageSize;
    private int numCheckSymbols;
    
    public ReedSolomon(Field field, int messageSize, int numCheckSymbols) {
        if(numCheckSymbols % 2 != 0) {
            throw new IllegalArgumentException("numCheckSymbols must be even.");
        }
        this.field = field;
        this.messageSize = messageSize;
        this.numCheckSymbols = numCheckSymbols;
    }
    
    /**
     * Encodes a message using polynomial interpolation.
     */
    public int[] encode(int[] message, int offset, int length) {
        if(length != messageSize) {
            throw new IllegalArgumentException("Message has incorrect size.");
        }
        FieldPolynomial p = FieldPolynomial.lagrangeInterpolate(message, offset, length, field);
        int[] codeword = new int[messageSize + numCheckSymbols];
        System.arraycopy(message, offset, codeword, 0, messageSize);
        for(int i = messageSize; i < messageSize + numCheckSymbols; i++) {
            codeword[i] = p.evaluate(i);
        }
        return codeword;
    }
    
    /**
     * Decodes a message using the Berlekamp-Welch algorithm.
     */
    public int[] decode(int[] codeword) {
        int codewordLength = messageSize + numCheckSymbols;
        if(codeword.length != codewordLength) {
            throw new IllegalArgumentException("Codeword has incorrect size.");
        }
        int[][] matrix = new int[codewordLength][];
        int k = numCheckSymbols / 2;
        int na = messageSize + k;
        for(int row = 0; row < codewordLength; row++) {
            int[] matrixRow = new int[codewordLength + 1];
            for(int column = 0; column < na; column++) {
                matrixRow[column] = field.pow(row, column);
            }
            for(int column = na; column < codewordLength; column++) {
                matrixRow[column] = field.negate(field.mul(field.pow(row, column - na), codeword[row]));
            }
            matrixRow[codewordLength] = field.mul(field.pow(row, k), codeword[row]);
            matrix[row] = matrixRow;
        }
        LinearAlgebraUtils.rowReduce(matrix, field);
        
        FieldPolynomial q = new FieldPolynomial(field);
        FieldPolynomial e = new FieldPolynomial(field);
        for(int ai = 0; ai < na; ai++) {
            q.setCoefficient(ai, matrix[ai][codewordLength]);
        }
        for(int bi = 0; bi < k; bi++) {
            e.setCoefficient(bi, matrix[na + bi][codewordLength]);
        }
        e.setCoefficient(k, 1);
        FieldPolynomial p = q.div(e);
        
        int[] message = new int[messageSize];
        for(int i = 0; i < messageSize; i++) {
            message[i] = p.evaluate(i);
        }
        
        return message;
    }
}
