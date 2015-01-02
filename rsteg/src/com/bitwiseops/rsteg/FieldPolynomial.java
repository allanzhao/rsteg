package com.bitwiseops.rsteg;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a polynomial in a finite field.
 */
public class FieldPolynomial {
    private ArrayList<Integer> coefficients;
    private final Field field;
    
    public FieldPolynomial(FieldPolynomial other) {
        this.coefficients = new ArrayList<Integer>(other.coefficients);
        this.field = other.field;
    }
    
    public FieldPolynomial(int[] coefficients, Field field) {
        this(coefficients, 0, coefficients.length, field);
    }
    
    public FieldPolynomial(int[] coefficients, int start, int length, Field field) {
        this.coefficients = new ArrayList<Integer>(coefficients.length);
        for(int i = start; i < start + length; i++) {
            this.coefficients.add(coefficients[i]);
        }
        this.field = field;
    }
    
    public FieldPolynomial(int value, Field field) {
        this.coefficients = new ArrayList<Integer>();
        this.coefficients.add(value);
        this.field = field;
    }
    
    public FieldPolynomial(Field field) {
        this.coefficients = new ArrayList<Integer>();
        this.field = field;
    }
    
    public int getDegree() {
        for(int i = coefficients.size() - 1; i > 0; i--) {
            if(getCoefficient(i) != 0) {
                return i;
            }
        }
        return 0;
    }
    
    public boolean isZero() {
        return getDegree() == 0 && getCoefficient(0) == 0;
    }
    
    public int getLeadingCoefficient() {
        for(int i = coefficients.size() - 1; i > 0; i--) {
            int coeff = getCoefficient(i);
            if(coeff != 0) {
                return coeff;
            }
        }
        return getCoefficient(0);
    }
    
    public List<Integer> findZeroes() {
        // gets the job done
        List<Integer> zeroes = new ArrayList<Integer>();
        for(int x = 0; x < field.getSize(); x++) {
            if(evaluate(x) == 0) {
                zeroes.add(x);
            }
        }
        return zeroes;
    }
    
    public FieldPolynomial add(FieldPolynomial addend) {
        checkCompatible(addend);
        int maxSize = Math.max(coefficients.size(), addend.coefficients.size());
        for(int i = 0; i < maxSize; i++) {
            setCoefficient(i, field.add(getCoefficient(i), addend.getCoefficient(i)));
        }
        return this;
    }
    
    public FieldPolynomial add(int addend) {
        setCoefficient(0, field.add(getCoefficient(0), addend));
        return this;
    }
    
    public FieldPolynomial sub(FieldPolynomial subtrahend) {
        checkCompatible(subtrahend);
        int maxSize = Math.max(coefficients.size(), subtrahend.coefficients.size());
        for(int i = 0; i < maxSize; i++) {
            setCoefficient(i, field.sub(getCoefficient(i), subtrahend.getCoefficient(i)));
        }
        return this;
    }
    
    public FieldPolynomial sub(int subtrahend) {
        setCoefficient(0, field.sub(getCoefficient(0), subtrahend));
        return this;
    }
    
    public FieldPolynomial mul(FieldPolynomial multiplicand) {
        checkCompatible(multiplicand);
        FieldPolynomial result = new FieldPolynomial(field);
        for(int j = 0; j < multiplicand.coefficients.size(); j++) {
            for(int i = 0; i < coefficients.size(); i++) {
                result.setCoefficient(i + j, field.add(result.getCoefficient(i + j), field.mul(getCoefficient(i), multiplicand.getCoefficient(j))));
            }
        }
        set(result);
        return this;
    }
    
    public FieldPolynomial mul(int multiplicand) {
        for(int i = 0; i < coefficients.size(); i++) {
            setCoefficient(i, field.mul(getCoefficient(i), multiplicand));
        }
        return this;
    }
    
    public FieldPolynomial div(FieldPolynomial divisor) {
        return div(divisor, new FieldPolynomial(field));
    }
    
    public FieldPolynomial div(FieldPolynomial divisor, FieldPolynomial remainderOut) {
        checkCompatible(divisor);
        checkCompatible(remainderOut);
        if(divisor.isZero()) {
            throw new ArithmeticException("Division by zero.");
        }
        FieldPolynomial quotient = new FieldPolynomial(field);
        remainderOut.set(this);
        int divisorLeadingCoefficient = divisor.getLeadingCoefficient();
        int divisorDegree = divisor.getDegree();
        while(!remainderOut.isZero() && remainderOut.getDegree() >= divisorDegree) {
            int t = field.div(remainderOut.getLeadingCoefficient(), divisorLeadingCoefficient);
            int qi = remainderOut.getDegree() - divisorDegree;
            quotient.setCoefficient(qi, t);
            for(int i = 0; i <= divisorDegree; i++) {
                remainderOut.setCoefficient(i + qi, field.sub(remainderOut.getCoefficient(i + qi), field.mul(divisor.getCoefficient(i), t)));
            }
        }
        return quotient;
    }
    
    public FieldPolynomial div(int divisor) {
        for(int i = 0; i < coefficients.size(); i++) {
            setCoefficient(i, field.div(getCoefficient(i), divisor));
        }
        return this;
    }
    
    public int evaluate(int x) {
        int y = getCoefficient(0);
        for(int i = 1; i < coefficients.size(); i++) {
            y = field.add(y, field.mul(getCoefficient(i), field.pow(x, i)));
        }
        return y;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(int i = coefficients.size() - 1; i >= 0; i--) {
            int coeff = getCoefficient(i);
            if(coeff != 0) {
                if(!first) {
                    sb.append("+");
                } else {
                    first = false;
                }
                sb.append(coeff);
                if(i > 1) {
                    sb.append("*x^");
                    sb.append(i);
                } else if(i == 1) {
                    sb.append("*x");
                }
            }
        }
        return sb.toString();
    }
    
    public int getCoefficient(int power) {
        if(power < coefficients.size()) {
            return coefficients.get(power);
        } else {
            return 0;
        }
    }
    
    public void setCoefficient(int power, int value) {
        if(power < coefficients.size()) {
            coefficients.set(power, value);
        } else {
            if(value != 0) {
                coefficients.ensureCapacity(power + 1);
                while(coefficients.size() < power) {
                    coefficients.add(0);
                }
                coefficients.add(value);
            }
        }
    }
    
    public FieldPolynomial set(FieldPolynomial other) {
        coefficients.clear();
        coefficients.addAll(other.coefficients);
        return this;
    }
    
    public FieldPolynomial set(int value) {
        coefficients.clear();
        setCoefficient(0, value);
        return this;
    }
    
    public FieldPolynomial copy() {
        return new FieldPolynomial(this);
    }
    
    public static FieldPolynomial lagrangeInterpolate(int[] values, int offset, int length, Field field) {
        FieldPolynomial result = new FieldPolynomial(field);
        FieldPolynomial delta = new FieldPolynomial(field);
        FieldPolynomial m = new FieldPolynomial(field);
        m.setCoefficient(1, 1);
        for(int i = 0; i < length; i++) {
            delta.set(1);
            int d = 1;
            for(int j = 0; j < length; j++) {
                if(i != j) {
                    m.setCoefficient(0, field.negate(j));
                    delta.mul(m);
                    d = field.mul(d, field.sub(i, j));
                }
            }
            delta.mul(field.mul(field.reciprocal(d), values[i + offset]));
            result.add(delta);
        }
        return result;
    }
    
    private void checkCompatible(FieldPolynomial other) {
        if(!other.field.equals(this.field)) {
            throw new IllegalArgumentException("Polynomials have different fields.");
        }
    }
}
