package edu.utexas.cs.alr.util;
import java.util.*;
public class Clause {
    private final List<Literal> literals = new ArrayList<>();

    public void addLiteral(Literal literal) {
        literals.add(literal);
    }

    //Add a literal to the start of the list
    public void addLiteralToFront(Literal literal) {
        literals.add(0, literal); 
    }

    // Function to check if the clause contains a specific literal
    public boolean containsLiteralExactly(Literal literal) {
        for (Literal l: literals) {
            if (l.getVariable() == literal.getVariable() && l.isNegated() == literal.isNegated()) {
                return true;
            }
        }
        return false;
    }

    // Getters
    public List<Literal> getLiterals() {
        return literals;
    }
}