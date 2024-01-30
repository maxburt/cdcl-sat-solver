package edu.utexas.cs.alr.util;
import java.util.*;
public class Clause {
    private final List<Literal> literals = new ArrayList<>();

    public void addLiteral(Literal literal) {
        literals.add(literal);
    }

    // Getters
    public List<Literal> getLiterals() {
        return literals;
    }
}