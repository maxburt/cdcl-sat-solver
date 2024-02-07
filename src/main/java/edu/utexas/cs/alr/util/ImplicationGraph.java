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

public class ImplicationGraph {
    private boolean verbose;
    private Map<Integer, Node> nodes; // Map variable to Node
    public Stack<Node> decisionStack; // To track decision levels

    public ImplicationGraph() {
        this.verbose = true;
        this.nodes = new HashMap<>();
        this.decisionStack = new Stack<>();
    }

    //function to add a decision node to the implication graph
    public void addDecision(Assignment assignment) {
        
        if (verbose == true) System.out.println("Adding decision node :  " + assignment);
        
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
        if (verbose == true) System.out.println("Adding implication node :  " + implied);
        //if (verbose == true) System.out.println("\tAntecedents passed in : " + antecedents.size());
        
        //adds a new node object to nodes map
        Node impliedNode = nodes.computeIfAbsent(implied.getLiteral().getVariable(), k -> new Node(implied));
        
        //loop through all antecedent assignments that were passed in
        for (Assignment antecedent : antecedents) {

            //get the corresponding Node from the nodes map, searched by using the literals as keys
            Node antecedentNode = nodes.get(antecedent.getLiteral().getVariable());
            if (antecedentNode != null) {
                //Link new node and antecedent node
                impliedNode.addAntecedent(antecedentNode);
                antecedentNode.addImplication(impliedNode);
            }
            else {
                System.err.println("Error getting antecedent from new implied assignment");
                System.exit(1);
            }
        }
    }

    //used for testing
    public void printAllNodes() {
        System.out.println("Printing Map of Nodes: ");
        for (Node node : nodes.values()) {
            System.out.print(node.getAssignment());
            if (node.isConflictNode()) {
                System.out.print(" CONFLICT NODE");
            }
            System.out.println();
        }
    }

    //add conflicting assignment to the implication graph
    public void addConflictNode(Assignment assignment, Clause clause) {
        if (verbose == true) System.out.println("Adding conflict node  : " + assignment);
        
        Node conflictNode = new Node(assignment);
        conflictNode.markAsConflictNode();
    
        // Add the conflict node to nodes map
        // Note: the key to the map is NEGATIVE for conflict node
        nodes.put(assignment.getLiteral().getVariable()  * -1, conflictNode);

        // Handle the conflict's antecedents (clauses)
        for (Literal literal : clause.getLiterals()) {
            int variable = literal.getVariable();
            Node antecedentNode = nodes.get(variable);
            if (antecedentNode != null) {
                // Add the antecedent node to the list of antecedents
                conflictNode.addAntecedent(antecedentNode);
                antecedentNode.addImplication(conflictNode);
            }
        }
        if (verbose == true) {
            conflictNode.printAntecedents();
        }
/* 
        // Backtrack to the appropriate decision level
        int toDecisionLevel = assignment.getDecisionLevel() - 1;
        backtrack(toDecisionLevel);
*/
    }

    public Node getConflictNode() {
        // Iterate through the nodes in the map
        for (Node node : nodes.values()) {
            // Check if the node is marked as a conflict node
            if (node.isConflictNode()) {
                return node;
            }
        }
        // If the conflict node is not found, return null or handle the case accordingly
        return null;
    }

    //removes conflict node from graph,
    //used for testing code
    public void removeConflictNode() {
        Node conflictNode = null;

        // Iterate through the nodes in the map
        Iterator<Map.Entry<Integer, Node>> iterator = nodes.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Integer, Node> entry = iterator.next();
            int variable = entry.getKey();
            Node node = entry.getValue();

            // Check if the node is marked as a conflict node
            if (node.isConflictNode()) {
                if (verbose) {
                    System.out.println("Removing conflict node from graph");
                }
                conflictNode = node;
                iterator.remove(); // Remove the conflict node from the map
                break; // Exit the loop once the conflict node is found
            }
        }

        // Remove references to the conflict node from antecedent nodes' implications
        if (conflictNode != null) {
            for (Node antecedent : conflictNode.getAntecedents()) {
                antecedent.removeImplication(conflictNode);
                if (verbose) {
                    System.out.println("Removing implications from antecedents");
                }
            }
        }
        else {
            System.err.println("No conflict node found to be removed...");
            System.exit(1);
        }
    }

     
    public Node findClosestCommonNode(List<List<Node>> paths, Node conflictNode) {
        // Create a counter map to keep track of how many times each node appears in the paths
        Map<Node, Integer> nodeCounter = new HashMap<>();
    
        // Iterate through each path and update the node counters, excluding the conflict node
        for (List<Node> path : paths) {
            Set<Node> pathNodes = new HashSet<>(path);
            pathNodes.remove(conflictNode);
    
            for (Node node : pathNodes) {
                nodeCounter.put(node, nodeCounter.getOrDefault(node, 0) + 1);
            }
        }
    
        // Find the node with the highest counter value
        Node closestCommonNode = null;

        for (Node node : paths.get(0)) {
            Integer count = nodeCounter.get(node);
            if (count != null && count == paths.size()) {
                closestCommonNode = node;
            }
        }
        return closestCommonNode;
    }
    
    
    /*
    // Finds the UIP (unique implication point)
    // Takes as input all paths from last decision node to conflict node
    public Node findClosestCommonNode(List<List<Node>> paths, Node conflictNode) {
        // Create a counter map to keep track of how many times each node appears in the paths
        Map<Node, Integer> nodeCounter = new HashMap<>();

        // Iterate through each path and update the node counters, excluding the conflict node
        for (List<Node> path : paths) {
            for (int i = 0; i < path.size(); i++) {
                Node node = path.get(i);
            
                // Exclude the conflict node from counting
                if (node != conflictNode) {
                    nodeCounter.put(node, nodeCounter.getOrDefault(node, 0) + 1);
                }
            }
        }
        // Find the node with the highest counter value
        Node closestCommonNode = null;
        int maxCount = 0;

        for (Map.Entry<Node, Integer> entry : nodeCounter.entrySet()) {
            Node node = entry.getKey();
            int count = entry.getValue();

            // If a node appears in all paths(has a count equal to the number of paths),
            // it's the closest common node (UIP).
            if (count == paths.size() && (closestCommonNode == null || count > maxCount)) {
                closestCommonNode = node;
                maxCount = count;
            }
        }

        return closestCommonNode;
    } 
    */

    // Function to find all paths from the most recent decision node to the conflict node
    public List<List<Node>> findAllPathsFromDecisionToConflict(Node conflictNode) {
        List<List<Node>> allPaths = new ArrayList<>();
        Stack<Node> path = new Stack<>();
        Node decisionNode = decisionStack.peek();   // get most recent decision node
        dfsFromDecisionToConflict(decisionNode, conflictNode, allPaths, path);
        if (verbose) System.out.println(allPaths.size() + " paths found"); 
        return allPaths;
    }

    // Depth-first search (DFS) to find all paths from decision node to conflict node
    private void dfsFromDecisionToConflict(Node currentNode, Node conflictNode, List<List<Node>> allPaths, Stack<Node> path) {
        // Push the current node onto the path stack
        System.out.println("Pushing " + currentNode.getAssignment() + " to a path");
        path.push(currentNode);        
        // If the current node is the conflict node, add the path to allPaths
        if (currentNode == conflictNode) {
            if (verbose )System.out.println("Current node equals conflict node. Adding a path");

            allPaths.add(new ArrayList<>(path));
        } else {
            // Recursively explore all implications
            for (Node implication : currentNode.getImplications()) {
                if (verbose) System.out.println("now searching " + implication.getAssignment()); 
                dfsFromDecisionToConflict(implication, conflictNode, allPaths, path);
            }
        }

        // Pop the current node from the path stack to backtrack

        Node popNode = path.pop();
        if (verbose) System.out.println("Popping " + popNode.getAssignment());
    }
    /////////

    //The function for making learned clause 
    public Clause createLearnedClause(Node UIP, Node conflictNode) {
        
        
        
        //Move Analyze conflict bulk of code to here
         

        
        //Step 0, create a starting clause based on incoming nodes to conflict node
        Clause learnedClause = generateStartingClause(conflictNode);
        if (learnedClause == null) {
            System.err.println("Error generating starting clause");
            System.exit(1);
        }
        if (verbose) {
            System.out.print("Starting clause generated  ");
            CNFConverter.printClause(learnedClause);

        }
        while (true) {
            //Step 1, check if clause is finished being made
            
            if (isClauseLearned(UIP.getAssignment().getLiteral(), learnedClause)) {
                if (verbose) {
                    System.out.println("Finished making learned clause");
                }
                break;
            }
            
            //Step 2: Pick most recently assigned literal in clause
            Node mostRecent = getMostRecentAssignedLiteral(learnedClause); 
            if (mostRecent == null) {
                System.err.println("Error getting most recently assigned literal from clause");
                System.exit(1);
            }

            //Step 3: Create a clause that is implied by mostRecent and any antecedent of mostRecent
            Clause newClause = createImpliedClause(mostRecent);
            if (newClause == null) {
                System.err.println("Error creating implied clause");
                System.exit(1);
            }

            //Step 4: Perform resolution between the two clauses, newClause and learnedClause
            learnedClause = performResolution(newClause, learnedClause);
            if (learnedClause == null) {
                System.err.println("Error performing resolution");
                System.exit(1);
            }
            if (verbose) {
                System.out.print("Learned clause is now  ");
                CNFConverter.printClause(learnedClause);
            }
        }
        return learnedClause;
    }

    private Clause performResolution(Clause clause1, Clause clause2) {
        // Create a new clause to store the result of resolution
        Clause resultClause = new Clause();

        // Iterate through the literals in clause1 and clause2
        for (Literal literal1 : clause1.getLiterals()) {
            for (Literal literal2 : clause2.getLiterals()) {
                // Check if the two literals are complementary (negations of each other)
                if (literal1.isNegationOf(literal2)) {
                    // Combine the non-complementary literals from both clauses
                    for (Literal nonComplementaryLiteral1 : clause1.getLiterals()) {
                        if (!nonComplementaryLiteral1.equals(literal1)) {
                            resultClause.addLiteral(nonComplementaryLiteral1);
                        }
                    }
                    for (Literal nonComplementaryLiteral2 : clause2.getLiterals()) {
                        if (!nonComplementaryLiteral2.equals(literal2)) {
                            resultClause.addLiteral(nonComplementaryLiteral2);
                        }
                    }
                    // Return the result clause after resolution
                    return resultClause;
                }
            }
        }

        // If no resolution is possible, return null
        return null;
    }

    //Helper function used in creating the learned clause algorithm
    private Clause createImpliedClause(Node node) {
        
        //Get most recently assigned antecedent node
        Node mostRecentAntecedentNode = null;
        List<Node> antecedents = node.getAntecedents();
        int decisionLevel = 0;
        for (Node antecedent : antecedents) {
            if (antecedent.getAssignment().getDecisionLevel() > decisionLevel) {
                mostRecentAntecedentNode = antecedent;
                decisionLevel = antecedent.getAssignment().getDecisionLevel();         }
        }
        System.out.println("Most recent antecedent is " + mostRecentAntecedentNode.getAssignment());

        //Create new clause with current node literal,
        //and negated version of antecedent to create new implied clause
        Clause clause = new Clause();
        clause.addLiteralToFront(node.getAssignment().getLiteral());
        clause.addLiteralToFront(mostRecentAntecedentNode.getAssignment().getLiteral().negate());
        
        
        System.out.print("Implied clause is " );
        CNFConverter.printClause(clause);
        //System.out.println("Triggered");
        
        return clause;
    }

    //Takes clause and returns literal that has been assigned most recently
    private Node getMostRecentAssignedLiteral(Clause clause) {
        Literal mostRecentLiteral = null;
        int mostRecentDecisionLevel = 0;
    
        for (Literal literal : clause.getLiterals()) {
            // Find the assignment for the literal in the nodes map
            Node assignmentNode = findAssignmentForLiteral(literal);
    
            if (assignmentNode != null) {
                int decisionLevel = assignmentNode.getAssignment().getDecisionLevel();
    
                // Check if this assignment has a higher decision level
                if (decisionLevel > mostRecentDecisionLevel) {
                    mostRecentDecisionLevel = decisionLevel;
                    mostRecentLiteral = literal;
                }
            }
            else {
                System.err.println("Error in getMostRecentAssignedLiteral function. Could not find assignment node for literal");
                System.exit(1);
            }
        }
        //Note: returns node
        return findAssignmentForLiteral(mostRecentLiteral);
    }

    private void performResolution(Node UIP, Clause clause) {

    }

    //Generate starting clause for creating learned clause
    private Clause generateStartingClause(Node conflictNode) {
        Clause startingClause = new Clause();
        
        // Collect literals from the antecedent nodes
        for (Node antecedent : conflictNode.getAntecedents()) {
            //Create new literals with opposite values of nodes
            Literal newClauseLiteral = antecedent.getAssignment().getLiteral().negate();
            
            //Add new literals to starting clause
            startingClause.addLiteral(newClauseLiteral);
        }
        return startingClause;
    }

    //checks if clause is finished being learned,
    //if UIP is the only variable in the clause from the current decision level
    private Boolean isClauseLearned(Literal UIPLiteral, Clause clause) {
        
        int UIPVariable = UIPLiteral.getVariable();  
        Boolean containsUIP = false;
        int currentDecisionLevel = decisionStack.peek().getAssignment().getDecisionLevel();
        List<Literal> literals = clause.getLiterals();
        int currentDecisionLevelLiteralCount = 0;

        for (Literal literal : literals) {

            //Check if UIP variable is present in the clause
            if (literal.getVariable() == UIPVariable) {
                containsUIP = true;
            }

            Node literalNode = findAssignmentForLiteral(literal);
            if (literalNode == null) {
                System.err.println("Couldn't find node for literal");
                System.exit(1);
            }
            int literalDecisionLevel = literalNode.getAssignment().getDecisionLevel();
            // Check if the literal is from the current decision level
            if (literalDecisionLevel == currentDecisionLevel) {
                
                // Increment the count for literals from the current decision level
                currentDecisionLevelLiteralCount++;

                // If there is more than one literal from the current decision level, the clause is not learned
                if (currentDecisionLevelLiteralCount > 1) {
                    return false;
                }
            }
        }

            if (currentDecisionLevelLiteralCount == 1 && containsUIP) {
                System.out.println("Created a new learned clause ");
                CNFConverter.printClause(clause);
            }
            //return true if the current decision level count is only 1, and the 1 is the same as UIP literal
            return currentDecisionLevelLiteralCount == 1 && containsUIP;
    }

    //helper function to take a literal, and find the corresponding node that Assigned it
    private Node findAssignmentForLiteral(Literal literal) {
        for (Node node : nodes.values()) {
            
            Assignment assignment = node.getAssignment();
            
            if (assignment.getLiteral().getVariable() == literal.getVariable()) {
                // Found a matching assignment
                return node;
            }
        }
        System.err.println("Could not find assignment node for literal");
        return null;
    }

    // Helper function to check if one literal is the negation of another
    private boolean isNegationOf(Literal literal1, Literal literal2) {
        return literal1.getVariable() == literal2.getVariable() && literal1.isNegated() != literal2.isNegated();
    }

    //Helper function for backtracking to second highest decision level in a clause
    public int getSecondHighestDecisionLevel(Clause clause) {
        List<Literal> literals = clause.getLiterals();

        //if size of clause is only 1, return 1 less than the single literals decision level
        if (literals.size() == 1) {
            int decisionLevel = getDecisionLevelForLiteral(literals.get(0)) - 1;
            return decisionLevel;
        }
        int highestDecisionLevel = Integer.MIN_VALUE;
        int secondHighestDecisionLevel = Integer.MIN_VALUE;
    
        for (Literal literal : literals) {
            int decisionLevel = getDecisionLevelForLiteral(literal);
            
            if (decisionLevel > highestDecisionLevel) {
                // Update the second highest decision level
                secondHighestDecisionLevel = highestDecisionLevel;
                // Update the highest decision level
                highestDecisionLevel = decisionLevel;
            } else if (decisionLevel > secondHighestDecisionLevel && decisionLevel < highestDecisionLevel) {
                // Update the second highest decision level
                secondHighestDecisionLevel = decisionLevel;
            }
        }
        return secondHighestDecisionLevel;
    }

    // Helper function to get the decision level for a given literal
    private int getDecisionLevelForLiteral(Literal literal) {
    int variableToFind = literal.getVariable();

    for (Node node : nodes.values()) {
        Assignment assignment = node.getAssignment();

        if (assignment != null && assignment.getLiteral().getVariable() == variableToFind) {
            return assignment.getDecisionLevel();
        }
    }
    return Integer.MIN_VALUE; // Return a default value if not found
}

    //Backtracks implication graph to decision level
    public void backtrack(int toDecisionLevel) {
        
        //for some reason this helped
        removeConflictNode();
        
        backtrackDecisionStack(toDecisionLevel);
        removeNodesAboveDecisionLevel(toDecisionLevel);
    }

    private void backtrackDecisionStack(int backtrackLevel) {
        while (!decisionStack.isEmpty() && decisionStack.peek().getAssignment().getDecisionLevel() > backtrackLevel) {
            Node node = decisionStack.pop();
        }
    }

    //main backtracking function to remove all nodes from the map above a given decision level
    private void removeNodesAboveDecisionLevel(int decisionLevelToRemove) {
        List<Node> nodesToRemove = new ArrayList<>();
        for (Node node : nodes.values()) {
            int nodeDecisionLevel = node.getAssignment().getDecisionLevel();
    
            if (nodeDecisionLevel > decisionLevelToRemove) {
                // Mark the node for removal
                nodesToRemove.add(node);
    
                // Remove the node from its antecedents' implications
                for (Node antecedent : node.getAntecedents()) {
                    antecedent.removeImplication(node);
                }
            }
        }
    
        // Remove the marked nodes from the map
        for (Node nodeToRemove : nodesToRemove) {
            nodes.remove(nodeToRemove.getAssignment().getLiteral().getVariable());
        }
    }
    
}