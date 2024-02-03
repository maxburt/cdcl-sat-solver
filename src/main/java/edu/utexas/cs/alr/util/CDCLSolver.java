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
    public List<Clause> learnedClauses; // conflict Clauses learned through implication graph, initially empty
    private ImplicationGraph implicationGraph;  //contains Nodes that contain assigments and antecedents
    private Stack<Assignment> assignmentStack; // To track variable assignments
    private int currentDecisionLevel;


    //Constructor
    public CDCLSolver(List<Clause> clauses) {
        this.clauses = clauses;
        this.learnedClauses = new ArrayList<>();
        this.implicationGraph = new ImplicationGraph();
        this.assignmentStack = new Stack<>();
        this.currentDecisionLevel = 0;
    }


    // Main method to solve the SAT problem
    public boolean solve() {
        while (true) {

            boolean decisionMade = makeDecision();
            if (!decisionMade) {    //No decision made, so its either satisfied currently, or it cant be satisfied
                return isSatisfied();
            }

            //perform BCP as much as possible or until a conflict occurs
            boolean conflict = unitPropagation();

            if (conflict) {
                if (currentDecisionLevel == 0) {
                    return false; // Conflict at base level, so UNSAT
                }
                analyzeConflict();
                backtrack();


            } else if (isSatisfied()) {
                return true; // All variables assigned without conflict, so SAT
            }
        }
    }

    //currently finds first unassigned literal from first unsatisfied clause
    //doesn't use decision heuristic or watch literals YET
    private boolean makeDecision() {

        //look for decision literal in learnedClauses
        Literal decisionLiteral = findUnassignedLiteralFromUnsatisfiedClauses(learnedClauses);
        if (decisionLiteral == null) {
            // If no suitable literal is found in learned clauses, check original clauses
            decisionLiteral = findUnassignedLiteralFromUnsatisfiedClauses(clauses);
        }

        //Make a decision on the literal
        if (decisionLiteral != null) {
            boolean value = true; // dumb approach, assigns value to TRUE arbitrarily
            currentDecisionLevel++;
            Assignment decision = new Assignment(decisionLiteral, value, currentDecisionLevel, Assignment.AssignmentType.DECISION);
            assignmentStack.push(decision);
            implicationGraph.addDecision(decision);
            return true; // A decision has been made
        }
        
        // No decision could be made, indicating all clauses are satisfied or a deadlock situation
        return false; 
    }

    //function that starts BCP
    //returns true if a conflict is detected
    //returns false if no conflicts
    private boolean unitPropagation() {
        boolean changeMade;
        do {
            changeMade = false;

            for (Clause clause : getAllClauses()) { //loop through all clauses, learned and original
                
                // skip satisfied clauses
                if (clauseIsSatisfied(clause)) {
                    continue;
                }

                if (isUnitClause(clause)) { //clause is an unsatisfied unit clause
                    
                    Literal unitLiteral = getUnassignedLiteral(clause);
                    
                    if (unitLiteral == null || isConflict(unitLiteral)) {
                        return true; // Conflict detected
                    }

                    //perform unit propogation on the other clauses
                    propagate(unitLiteral, clause);
                    
                    changeMade = true;
                }
            }
        } while (changeMade); //if a change has been made, start the loop again

    return false; // No conflicts detected, no more changes made
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







    //helper function to combine original clauses and learned clauses.
    //used in BCP function
    private List<Clause> getAllClauses() {
        List<Clause> allClauses = new ArrayList<>(clauses);
        allClauses.addAll(learnedClauses);
        return allClauses;
    }


    //helper function that determines if a clause is currently
    //a unit clause under the current assignment stack
    private boolean isUnitClause(Clause clause) {
        int unassignedCount = 0;
        for (Literal literal : clause.getLiterals()) {
            if (!literalIsAssigned(literal)) {
                unassignedCount++;
                if (unassignedCount > 1) {
                    return false; // More than one unassigned literal
                }
            } else if (literalIsTrue(literal)) {
                return false; // Clause is already satisfied
            }
        }
        return unassignedCount == 1;
    }


    //returns first unassigned literal in a clause
    private Literal getUnassignedLiteral(Clause clause) {
        for (Literal literal : clause.getLiterals()) {
            if (!literalIsAssigned(literal)) {
                return literal;
            }
        }
        return null; // No unassigned literal found
    }


    //checks if the negation of the literal is already been assgined true
    private boolean isConflict(Literal literal) {
        // Check if assigning this literal would cause a conflict
        // This usually means checking if the negation of the literal has already been assigned true
        Literal negatedLiteral = literal.negate();
        return literalIsAssigned(negatedLiteral) && literalIsTrue(negatedLiteral);
    }


    //this function assigns unitLiteral to make it evaluate true
    //i.e. if unit clause is !x, it assigns x to false
    //if unit clause is x, it assigns x to true
    private void propagate(Literal unitLiteral, Clause unitClause) {
        
        boolean value = !unitLiteral.isNegated();
    
        Assignment impliedAssignment = new Assignment(unitLiteral, value, currentDecisionLevel, Assignment.AssignmentType.IMPLICATION);
    
        // Determine the antecedents from the assignment stack
        // this function searches assignment stack and finds the assignments that forced
        // the assigment in the unit clause
        List<Assignment> antecedents = findAntecedentsForUnitClause(unitClause);
    
        // Add the implication to the implication graph with its antecedents
        implicationGraph.addImplication(impliedAssignment, antecedents);
    
        // Push the implied assignment onto the assignment stack
        assignmentStack.push(impliedAssignment);
    }


    
    //looks at a unit clause found through BCP
    //and looks through Assignment stack to find all Assignments that forced
    //the unit clause to exist
    private List<Assignment> findAntecedentsForUnitClause(Clause unitClause) {
        List<Assignment> antecedents = new ArrayList<>();
        // Iterate through the assignment stack to find assignments that made the other literals false
        for (Assignment assignment : assignmentStack) {
            // Check if the assignment is related to any literal in the clause
            for (Literal literal : unitClause.getLiterals()) {
                // The assignment must correspond to a literal in the clause and make it false
                if (assignment.getLiteral().getVariable() == literal.getVariable() && 
                    assignment.getValue() != literal.isNegated()) {
                    antecedents.add(assignment);
                }
            }
        }
        return antecedents;
    }

    //helper function used in makeDecision function
    //returns the first unassigned literal in the first unsatisfied clause in a list of clauses
    private Literal findUnassignedLiteralFromUnsatisfiedClauses(List<Clause> clauses) {
        for (Clause clause : clauses) {
            if (!clauseIsSatisfied(clause)) {
                for (Literal literal : clause.getLiterals()) {
                    if (!literalIsAssigned(literal)) {
                        return literal; // Found an unassigned literal in an unsatisfied clause
                    }
                }
            }
        }
        return null; // No unassigned literal found in unsatisfied clauses
    }


    //helper function returns the last Decision assignment
    private Assignment getLastDecision() {
        // Iterate backward through the assignment stack to find the last decision
        // This is necessary to link the implication to its antecedent decision
        ListIterator<Assignment> iterator = assignmentStack.listIterator(assignmentStack.size());
        while (iterator.hasPrevious()) {
            Assignment assignment = iterator.previous();
            if (assignment.getType() == Assignment.AssignmentType.DECISION) {
                return assignment;
            }
        }
        return null; // No decision found, which should not happen if propagate is called correctly
    }


    //helper function determines if a literal evaluates to true under current stack of assignments
    private boolean literalIsTrue(Literal literal) {
        for (Assignment assignment : assignmentStack) { //loop through all assignments
            if (assignment.getLiteral().getVariable() == literal.getVariable()) {   //literal is assigned
                if (literal.isNegated()) {
                    return assignment.getValue() == false; // Expect the assignment value to be false for a negated literal to be true
                } else {
                    return assignment.getValue() == true; // Expect the assignment value to be true for a non-negated literal to be true
                }
            }
        }
        return false; // Literal is not assigned, so default to false
    }

    //helper function to determine if a literal has already been assigned
    private boolean literalIsAssigned(Literal literal) {
        for (Assignment assignment : assignmentStack) {
            Literal assignedLiteral = assignment.getLiteral();
            if (assignedLiteral.getVariable() == literal.getVariable()){
                return true; // The exact literal is already assigned
            }
        }
        return false; // The literal is not yet assigned
    }

    //helper function to determine if a given clause is satisfied
    private boolean clauseIsSatisfied(Clause clause) {
        for (Literal literal : clause.getLiterals()) {
            if (literalIsTrue(literal)) {
                return true; 
            }
        }
        return false;
    }

    //function to check if the formula is satisfied
    private boolean isSatisfied() {             //check learned clauses
        for (Clause clause : learnedClauses) {  
            if (!clauseIsSatisfied(clause)) {
                return false;
            }
        }
        for (Clause clause : clauses) {         //check original clauses
            if (!clauseIsSatisfied(clause)) {   
                return false; 
            }
        }
        return true; // all clauses are satisfied
    }    


    //Definition of a Node class, used in implication graph
    class Node {
        private Assignment assignment;
        private List<Node> antecedents; // Nodes that imply this node

        public Node(Assignment assignment) {
            this.assignment = assignment;
            this.antecedents = new ArrayList<>();
        }

        public void addAntecedent(Node antecedent) {
            antecedents.add(antecedent);
        }
    }

    
    class ImplicationGraph {

        private Map<Integer, Node> nodes; // Map variable to Node
        private Stack<Node> decisionStack; // To track decision levels

        public ImplicationGraph() {
            this.nodes = new HashMap<>();
            this.decisionStack = new Stack<>();
        }

        //function to add a decision node to the implication graph
        public void addDecision(Assignment assignment) {
            //node is a decision node, therefore it has no antecedents
            Node node = new Node(assignment);
           
            //add node to nodes map object
            nodes.put(assignment.getLiteral().getVariable(), node);
            
            //add it to decision stack as well
            decisionStack.push(node);
        }

        
        //function to add an implied node to the implication graph 
        //takes as input the assignment that was just implied via BCP, as well as the list of
        //other previous assignments that forced this assignment (antecedents)
        public void addImplication(Assignment implied, List<Assignment> antecedents) {
            //adds a new node object to nodes map
            Node impliedNode = nodes.computeIfAbsent(implied.getLiteral().getVariable(), k -> new Node(implied));
            
            //loop through all antecedents passed in
            for (Assignment antecedent : antecedents) {
                //get the corresponding antecedent Node from the nodes map
                Node antecedentNode = nodes.get(antecedent.getLiteral().getVariable());
                    if (antecedentNode != null) {
                    //add the antecedent node to the list of antecedents
                    //within the implied node, thus establishing a relationship
                    //between the two nodes, an "edge"
                    impliedNode.addAntecedent(antecedentNode);
                }
            }
        }


        //need to fix this function
        public void backtrack(int toDecisionLevel) {
            while (!decisionStack.isEmpty() && decisionStack.peek().assignment.getDecisionLevel() > toDecisionLevel) {
                Node node = decisionStack.pop();
                nodes.remove(node.assignment.getLiteral().getVariable());
            }
        }
    }
}