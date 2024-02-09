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

    public List<Literal> getLiterals() {
        return literals;
    }

    public void removeDuplicates() {
        List<Literal> newList = new ArrayList<>(literals);
        for (int idx = 0; idx < newList.size() - 1; idx++) {
            for (int idx2 = idx + 1; idx2 < newList.size(); idx2++) {
                Literal literal1 = newList.get(idx);
                Literal literal2 = newList.get(idx2);
        
                if (literal1.getVariable() == literal2.getVariable() && literal1.isNegated() == literal2.isNegated()) {
                    // Duplicate found, remove it from the newList
                    newList.remove(idx2);
                    idx2--; // Decrement idx2 because the size of newList has decreased
                }
            } 
        }
  
        // Update the original list with the filtered list
        literals.clear();
        literals.addAll(newList);

    }
}