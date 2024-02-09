//  run with 
//      java -cp target/pa1-1.0-SNAPSHOT-jar-with-dependencies.jar edu.utexas.cs.alr.SATDriver < <path to input file>
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
    public boolean verbose;
    public List<Clause> clauses; // The CNF formula as a list of clauses
    public List<Clause> learnedClauses; // conflict Clauses learned through implication graph, initially empty
    private ImplicationGraph implicationGraph;  //contains Nodes that contain assigments and antecedents
    private Stack<Assignment> assignmentStack; // To track variable assignments
    private int currentDecisionLevel;

    //Constructor
    public CDCLSolver(List<Clause> clauses) {
        this.verbose = true;
        this.clauses = clauses;
        this.learnedClauses = new ArrayList<>();
        this.implicationGraph = new ImplicationGraph();
        this.assignmentStack = new Stack<>();
        this.currentDecisionLevel = 0;
    }

    // Main method to solve the SAT problem
    public boolean solve(){
        while (true) {

            //Checks learned clauses to see if they contain
            //unit clauses and their negations
            if (hasContradictoryUnitClause(learnedClauses)) {
                System.out.println("Has contradictory unit clauses in learned clause");
                return false;
            }
            
            boolean decisionMade = makeDecision();

            if (!decisionMade) {    //No decision made, so its either satisfied currently, or it cant be satisfied
                return isSatisfied();
            }

            /* 
            //check if the decision caused a conflict
            Clause falseClause = getFalseClause();
            if (falseClause != null) {
                implicationGraph.addConflictNode(assignmentStack.peek(), null, falseClause);
                System.exit(1);
            }
            */
            //Start the BCP process
            Clause conflict = unitPropagation();    //if conflict encountered, returns a clause
            if (conflict != null) {     //BCP lead to a conflict

                if (currentDecisionLevel == 0) {
                    return false; // Conflict at base level, so UNSAT
                }
                //Fix analyze conflict
                Clause learnedClause = analyzeConflict();
                //Fix backtrack
                backtrack(learnedClause);
                printAssignmentStack();

            } else if (isSatisfied()) {
                return true; // All variables assigned without conflict, so SAT
            }
        }
    }

    //currently finds first unassigned literal from first unsatisfied clause
    private boolean makeDecision() {

        //look for first unassigned literal in the first unsatisfied clause of learned clauses
        Literal decisionLiteral = findUnassignedLiteralFromUnsatisfiedClauses(learnedClauses);

        if (decisionLiteral == null) {
            
            // If no suitable literal is found in learned clauses, check original clauses
            decisionLiteral = findUnassignedLiteralFromUnsatisfiedClauses(clauses);
        }

        if (decisionLiteral != null) {
            //Check if a satisfying decision would
            //falsify a unit clause in the learnedClauses and clauses list
            boolean value = false;
            if (wouldFalsifyUnitClause(decisionLiteral)) {
                value = decisionLiteral.isNegated(); 
            }                
            else {
                value = !decisionLiteral.isNegated();
            }            
            
            currentDecisionLevel++;
            //Add new assignment to stack
            Assignment decision = new Assignment(decisionLiteral, value, currentDecisionLevel, Assignment.AssignmentType.DECISION);
            assignmentStack.push(decision);
            
            //Add assignment to implication graph
            implicationGraph.addDecision(decision);
            return true; // A decision has been made
        }
        
        // No decision could be made, indicating all clauss are assigned
        // formula could be satisfied or a deadlock situation
        return false; 
    }

    //function that starts BCP
    //if a conflict is detected it returns the unit clause
    //in which a conflict is occuring
    private Clause unitPropagation() {   
        boolean changeMade;
        do {
            changeMade = false;

            for (Clause clause : getAllClauses()) { //loop through all clauses, learned and original
                
                // skip satisfied clauses
                if (clauseIsSatisfied(clause)) {
                    continue;
                }             

                //what makes a clause a unit clause
                if (isUnitClause(clause)) { //clause is an unsatisfied unit clause
                    Literal unitLiteral = getUnassignedLiteral(clause);
                    //check if assigning the unitLiteral would cause a conflict
                    if (unitLiteral == null || isConflict(unitLiteral, clause)) {
                        return clause; // Conflict conflict clause
                    }
                    
                    //perform unit propogation on the other clauses
                    propagate(unitLiteral, clause);
                    
                    changeMade = true;
                }
            }
        } while (changeMade); //if a change has been made, start the loop again

    return null; // No conflicts detected
    }


    //This function essentially analyzes the implcation
    //to create a new learned clause, and returns that clause as well as adds it
    //to learnedClause list object
    private Clause analyzeConflict() {
        if (verbose) System.out.println("Analyzing conflict...");
        //Get conflict node from implication graph
        Node conflictNode = implicationGraph.getConflictNode();
        Node UIP = null;
        if (conflictNode != null) {
            if (verbose) System.out.println("Got conflict node..."); 

            //Get all paths from last decision node to conflict node

            List<List<Node>> paths = implicationGraph.findAllPathsFromDecisionToConflict(conflictNode); 
            if (paths == null) {
                System.err.println("Error last decision node to conflict node");
                System.exit(1);
            }

            //Find Unique implication point closest to conflict node
            UIP = implicationGraph.findClosestCommonNode(paths, conflictNode);

            if (UIP == null) {
                System.err.println("Could not find UIP from paths");
                System.exit(1);
            } else {
                if (verbose) System.out.println("Found UIP. UIP is " + UIP.getAssignment());
            }
        } else {
            System.err.println("No conflict node detected in graph");
            System.exit(1);
        }

        //use the conflict node and UIP to create a new clause to add to the learnedClauses list
        Clause learnedClause = implicationGraph.createLearnedClause(UIP, conflictNode, assignmentStack);
        if (learnedClause != null) {
            //Add learned clause to learnedClause map
            learnedClauses.add(0, learnedClause);
            if (verbose) {
                System.out.println("Added learned clause to list");
                CNFConverter.printClauses(learnedClauses);
            }
        } else {
            System.err.println("Error creating learned clause from implication graph");
            System.exit(1);
        }
        return learnedClause;
    }

    //Backtrack to a decision level that makes the new learned clause an asserting clause
    //in the next decision step
    private void backtrack(Clause learnedClause) {
        int backtrackLevel = implicationGraph.getSecondHighestDecisionLevel(learnedClause);
        System.out.println("Backtrack level is " + backtrackLevel);
        currentDecisionLevel = backtrackLevel;

        //Delete all nodes whose decision level is greater than backtrack level
        backtrackAssignmentList(backtrackLevel);
        implicationGraph.backtrack(backtrackLevel);
    }

    //helper function to backtrack assignment stack
    private void backtrackAssignmentList(int backtrackLevel) {
        Iterator<Assignment> iterator = assignmentStack.iterator();
    
        while (iterator.hasNext()) {
            Assignment assignment = iterator.next();
            int decisionLevel = assignment.getDecisionLevel();
            
            if (decisionLevel > backtrackLevel) {
                // Remove the assignment if its decision level is greater than backtrackLevel
                iterator.remove();
            }
        }
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


//checks if assigning the literal to evaluate to true (adding a unit clause) would
//make any of the clauses false
private boolean isConflict(Literal literal, Clause unitClause) {
    return testAssignmentForConflict(literal, unitClause);
}

//this function creates a temporary assignment of the literal to evaluate to true
//if a confict is detected it adds a conflict node to the graph
private boolean testAssignmentForConflict(Literal literal, Clause unitClause) {
    // Temporarily add the assignment
    Assignment assignment = new Assignment(literal, !literal.isNegated(), currentDecisionLevel, Assignment.AssignmentType.IMPLICATION);
    assignmentStack.push(assignment);
    // Check if any clause becomes false
    for (Clause clause : getAllClauses()) {
        if (!clauseIsSatisfied(clause) && allLiteralsFalse(clause)) {
            
            //Keep the assignment
            //assignmentStack.pop(); // Revert the temporary assignment
            
            //Get antecedents for 
            List<Assignment> antecedents = findAntecedentsForUnitClause(unitClause);
            //Remove literal in clause that corresponds to assigment

            // Create an iterator for the antecedents list
            Iterator<Assignment> iterator = antecedents.iterator();

            // Remove literal in clause that corresponds to assignment
            while (iterator.hasNext()) {
                Assignment ant = iterator.next();
                if (ant.getLiteral().equals(assignment.getLiteral())) {
                    iterator.remove(); // Remove the assignment from the list
                    break; // Exit the loop after the first removal
                }
            }
            
            //run addConflict
            implicationGraph.addConflictNode(assignment, antecedents, clause);

            return true; // Conflict detected
        }
    }
    // No conflict found, revert the temporary assignment
    assignmentStack.pop();
    return false; // No conflict detected
}

//helper function that looks at a clause and returns true if
//all literals are assigned to false
private boolean allLiteralsFalse(Clause clause) {
    for (Literal literal : clause.getLiterals()) {
        if (!literalIsAssigned(literal) || literalIsTrue(literal)) {
            // If any literal is unassigned or true, the clause is not unsatisfiable
            return false;
        }
    }
    // All literals are assigned and false, making the clause unsatisfiable
    return true;
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
                if (assignment.getLiteral().getVariable() == literal.getVariable()/*  && 
                    assignment.getValue() != literal.isNegated()*/) {
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

    // Function to check if a list of learned clauses contains a unit clause and its negation
    private boolean hasContradictoryUnitClause(List<Clause> learnedClauses) {
        for (Clause clause : learnedClauses) {
            // Check each clause for being a unit clause
            if (clause.getLiterals().size() == 1) {
                Literal literal = clause.getLiterals().get(0);
                Literal negation = new Literal(literal.getVariable(), !literal.isNegated());
                // Check if the negation of the unit clause is also present in the learned clauses
                if (containsUnitLiteral(learnedClauses, negation)) {
                    return true; // Found a unit clause and its negation
                }
            }
        }
        return false; // No contradictory unit clause found
    }
    
    // Helper function to check if a list of clauses contains a specific literal
    private static boolean containsUnitLiteral(List<Clause> clauses, Literal literal) {
        for (Clause clause : clauses) {
            if (clause.getLiterals().size() == 1 && clause.containsLiteralExactly(literal)) {
                return true;
            }
        }
        return false;
    }

    // Helper function to determine if satisfying a given decision literal
    // would falsify a unit clause within the learned list and 
    private boolean wouldFalsifyUnitClause(Literal decisionLiteral) {
        for (Clause clause : learnedClauses) {
           
            if (clause.getLiterals().size() == 1) {
                if (clause.getLiterals().get(0).getVariable() == decisionLiteral.getVariable()) {
                    if (clause.getLiterals().get(0).isNegated() != decisionLiteral.isNegated()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //For debugging, prints assignment stack
    private void printAssignmentStack() {
        System.out.println("Assignment stack is ....");
        for (Assignment assignment : assignmentStack) {
            System.out.println(assignment);
        }
    }

    //Checks if assignment stack has falsified a clause
    //for checking if decision node made a clause false
    private Clause getFalseClause() {
        for (Clause clause : getAllClauses()) {
            if (allLiteralsFalse(clause)) {
                return clause;
            }
        }
        return null;
    }
}