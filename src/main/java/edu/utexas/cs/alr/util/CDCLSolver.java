package edu.utexas.cs.alr.util;

import edu.utexas.cs.alr.ast.Expr;

import edu.utexas.cs.alr.ast.*;
import edu.utexas.cs.alr.parser.ExprBaseListener;
import edu.utexas.cs.alr.parser.ExprLexer;
import edu.utexas.cs.alr.parser.ExprParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;

import static edu.utexas.cs.alr.ast.ExprFactory.*;
import static edu.utexas.cs.alr.util.ExprWalker.dfsWalk;

public class CDCLSolver {

    public List<Clause> clauses; // The CNF formula as a list of clauses
    private ImplicationGraph implicationGraph;
    private Stack<Assignment> assignmentStack; // To track variable assignments
    private int currentDecisionLevel;

    public CDCLSolver(List<Clause> clauses) {
        this.clauses = clauses;
        this.implicationGraph = new ImplicationGraph();
        this.assignmentStack = new Stack<>();
        this.currentDecisionLevel = 0;
    }

    // Main method to solve the SAT problem
    public boolean solve() {
        while (true) {
            boolean decisionMade = makeDecision();
            if (!decisionMade) {
                return false; // No decision can be made, so UNSAT
            }
            boolean conflict = unitPropagation();
            if (conflict) {
                if (currentDecisionLevel == 0) {
                    return false; // Conflict at base level, so UNSAT
                }
                analyzeConflict();
                backtrack();
            } else if (isSatisfiable()) {
                return true; // All variables assigned without conflict, so SAT
            }
        }
    }

    private boolean makeDecision() {
      // Find an unassigned variable
      for (Clause clause : clauses) {
        for (Literal literal : clause.getLiterals()) {
            if (!isAssigned(literal)) {
                // Assign a truth value (let's say true) to the unassigned variable
                boolean value = true; // You can implement a heuristic to choose the value
                currentDecisionLevel++;
                Assignment decision = new Assignment(literal, value, currentDecisionLevel);
                assignmentStack.push(decision);
                implicationGraph.addDecision(decision);
                return true;
            }
        }
    }
    return false; // no variable can be assigned
    }

    private boolean unitPropagation() {
        // Implement unit propagation logic here
        // Propagate the implications of the decisions
        // Update implicationGraph and assignmentStack
        // Return true if a conflict is detected
        return false;
    }

    private void analyzeConflict() {
        // Implement conflict analysis here
        // Analyze the implication graph to learn a new clause
        // Determine the backtracking level
    }

    private void backtrack() {
        // Implement backtracking logic here
        // Undo assignments and implications made after the conflict level
        // Update implicationGraph and assignmentStack
    }

    private boolean isSatisfiable() {
        // Check if all variables have been assigned without conflict

        // return assignmentStack.size() ==  /*number of unique variables*/;
        return true;
    }



    //helper function to determine if a literal has already been assigned
    private boolean isAssigned(Literal literal) {
        for (Assignment assignment : assignmentStack) {
            Literal assignedLiteral = assignment.getLiteral();
            if (assignedLiteral.getVariable() == literal.getVariable()){
                return true; // The exact literal is already assigned
            }
        }
        return false; // The literal is not yet assigned
    }
    

    class ImplicationGraph {
    // Implementation of Implication Graph

            class Node {
            Assignment assignment;
            List<Node> antecedents; // Nodes that imply this node

            Node(Assignment assignment) {
                this.assignment = assignment;
                this.antecedents = new ArrayList<>();
            }

            void addAntecedent(Node antecedent) {
                antecedents.add(antecedent);
            }
        }

        private Map<Integer, Node> nodes; // Map variable to Node
        private Stack<Node> decisionStack; // To track decision levels

        public ImplicationGraph() {
            this.nodes = new HashMap<>();
            this.decisionStack = new Stack<>();
        }

        public void addDecision(Assignment assignment) {
            Node node = new Node(assignment);
            nodes.put(assignment.getLiteral().getVariable(), node);
            decisionStack.push(node);
        }

        public void addImplication(Assignment implied, Assignment antecedent) {
            Node impliedNode = nodes.computeIfAbsent(implied.getLiteral().getVariable(), 
                                                 k -> new Node(implied));
            Node antecedentNode = nodes.get(antecedent.getLiteral().getVariable());
            if (antecedentNode != null) {
                impliedNode.addAntecedent(antecedentNode);
            }
        }

        public void backtrack(int toDecisionLevel) {
            while (!decisionStack.isEmpty() && decisionStack.peek().assignment.getDecisionLevel() > toDecisionLevel) {
                Node node = decisionStack.pop();
                nodes.remove(node.assignment.getLiteral().getVariable());
            }
        }
    }

    //represents an assignment of a variable to a value
    class Assignment {
        private final Literal literal; // The literal being assigned
        private final boolean value; // The assigned truth value (true or false)
        private final int decisionLevel; // The decision level at which the assignment was made

        //constructor
        public Assignment(Literal literal, boolean value, int decisionLevel) {
            this.literal = literal;
            this.value = value;
            this.decisionLevel = decisionLevel;
        }
        // Getters
        public Literal getLiteral() {
            return literal;
        }
    
        public boolean getValue() {
            return value;
        }
    
        public int getDecisionLevel() {
            return decisionLevel;
        }

        @Override
        public String toString() {
            // Represents the literal assignment in a readable format
            String literalRepresentation = (literal.isNegated() ? "¬" : "") + "x" + literal.getVariable();
            return literalRepresentation + " = " + value + " at level " + decisionLevel;
        }
    }
}
