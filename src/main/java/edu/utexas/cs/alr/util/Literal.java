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
}