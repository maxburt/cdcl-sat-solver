package edu.utexas.cs.alr.util;

public class Literal {
    private final int variable;
    private final boolean isNegated;

    public Literal(int variable, boolean isNegated) {
        this.variable = variable;
        this.isNegated = isNegated;
    }

    // Getters
    public int getVariable() {
        return variable;
    }

    public boolean isNegated() {
        return isNegated;
    }


    // Returns a negated version of the literal
    // i.e. if literal is !x1, it returns x1,
    // if literal is x1 it returns !x1.
    public Literal negate() {
        // Create a new Literal with the same variable but opposite negation status
        return new Literal(this.variable, !this.isNegated);
    }
    
}