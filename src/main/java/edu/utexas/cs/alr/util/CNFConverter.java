package edu.utexas.cs.alr.util;
import java.util.*;
import edu.utexas.cs.alr.ast.Expr;
import edu.utexas.cs.alr.ast.*;

//class for converting CNF formula to a list of Clause objects
public class CNFConverter {

    //main function to convert formula to list of Clauses
    public static List<Clause> convertToClauses(Expr expr) {
        List<Clause> clauses = new ArrayList<>();
        
        //Converts expression to a list of clause objects
        extractClauses(expr, clauses);
        
        //Remove duplicate literals from each clause in list of clauses
        for (Clause clause : clauses) {
            clause.removeDuplicates();
        }

        //Removes clauses that contain x v !x, for all x. Tautology
        removeContradictoryClauses(clauses);
        return clauses;
    }

    //function to print a list of Clauses
        public static void printClause(Clause clause) {
            System.out.print("Clause " + ": ");

            List<Literal> literals = clause.getLiterals();
            for (int j = 0; j < literals.size(); j++) {
                Literal literal = literals.get(j);
                String literalStr = (literal.isNegated() ? "!" : "") + "x" + literal.getVariable();
                
                System.out.print(literalStr);
                if (j < literals.size() - 1) {
                    System.out.print(" | "); 
                }
            }
            System.out.println();


    }
    public static void printClauses(List<Clause> clauses) {
        for (int i = 0; i < clauses.size(); i++) {
            Clause clause = clauses.get(i);
            System.out.print("Clause " + (i + 1) + ": ");

            List<Literal> literals = clause.getLiterals();
            for (int j = 0; j < literals.size(); j++) {
                Literal literal = literals.get(j);
                String literalStr = (literal.isNegated() ? "!" : "") + "x" + literal.getVariable();
                
                System.out.print(literalStr);
                if (j < literals.size() - 1) {
                    System.out.print(" | "); 
                }
            }
            System.out.println();
        }
    }


    private static void extractClauses(Expr expr, List<Clause> clauses) {
        if (expr instanceof AndExpr) {
            AndExpr andExpr = (AndExpr) expr;
            extractClauses(andExpr.getLeft(), clauses);
            extractClauses(andExpr.getRight(), clauses);
        } else {
            Clause clause = new Clause();
            extractLiterals(expr, clause);
            clauses.add(clause);
        }
    }

    private static void extractLiterals(Expr expr, Clause clause) {
        if (expr instanceof OrExpr) {
            OrExpr orExpr = (OrExpr) expr;
            extractLiterals(orExpr.getLeft(), clause);
            extractLiterals(orExpr.getRight(), clause);
        } else if (expr instanceof NegExpr) {
            NegExpr negExpr = (NegExpr) expr;
            VarExpr varExpr = (VarExpr) negExpr.getExpr();
            clause.addLiteral(new Literal((int) varExpr.getId(), true));
        } else if (expr instanceof VarExpr) {
            VarExpr varExpr = (VarExpr) expr;
            clause.addLiteral(new Literal((int) varExpr.getId(), false));
        }
    }

    public static void removeContradictoryClauses(List<Clause> clauses) {
        // Iterate over each clause in the list
        for (int i = 0; i < clauses.size(); i++) {
            Clause clause = clauses.get(i);
            boolean containsContradiction = false;

            // Check each literal in the current clause
            for (Literal literal : clause.getLiterals()) {
                Literal oppositeLiteral = new Literal(literal.getVariable(), !literal.isNegated());

                // If the clause contains the opposite of the current literal, mark it for removal
                if (clause.containsLiteralExactly(oppositeLiteral)) {
                    containsContradiction = true;
                    break;
                }
            }

            // If the clause contains a contradiction, remove it from the list
            if (containsContradiction) {
                clauses.remove(i);
                i--; // Decrement index to account for removed element
            }
        }
    }
}