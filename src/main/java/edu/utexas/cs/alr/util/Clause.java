package edu.utexas.cs.alr.util;
import java.util.*;
public class Clause {
    int number;
    private final List<Literal> literals = new ArrayList<>();
    private Literal watchLiteral1;
    private Literal watchLiteral2;

    public Clause() {
        number = 0;
        watchLiteral1 = null;
        watchLiteral2 = null;
    }
    public Clause(int x) {
        number = x;
        watchLiteral1 = null;
        watchLiteral2 = null;
    } 

    public void setClauseNumber(int x) {
        number = x;
    }
    public int getClauseNumber() {
        return number;
    }

    public void initWatchLiterals(){
        if (literals.get(0) != null) this.watchLiteral1 = literals.get(0);
        if (literals.get(1) != null) this.watchLiteral2 = literals.get(1);
    }

    public Literal getWatchLiteral1() {
        return watchLiteral1;
    }

    public Literal getWatchLiteral2() {
        return watchLiteral2;
    }
    //Check current state of clause and update watch literals
    public void updateWatchLiterals(Stack<Assignment> assignmentStack) {

    }

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