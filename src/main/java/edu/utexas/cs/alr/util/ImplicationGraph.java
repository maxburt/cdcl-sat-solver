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
    private Stack<Node> decisionStack; // To track decision levels

    public ImplicationGraph() {
        this.verbose = true;
        this.nodes = new HashMap<>();
        this.decisionStack = new Stack<>();
    }

    //function to add a decision node to the implication graph
    public void addDecision(Assignment assignment) {
        
        if (verbose == true) System.out.println("Adding decision :  " + assignment);
        
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
        
        //adds a new node object to nodes map
        Node impliedNode = nodes.computeIfAbsent(implied.getLiteral().getVariable(), k -> new Node(implied));
        
        //loop through all antecedent assignments that were passed in
        for (Assignment antecedent : antecedents) {

            //get the corresponding Node from the nodes map, searched by using the literals as keys
            Node antecedentNode = nodes.get(antecedent.getLiteral().getVariable());
                if (antecedentNode != null) {
                //add the antecedent node to the list of antecedents
                //within the implied node, thus establishing a relationship
                //between the two nodes, an "edge"
                impliedNode.addAntecedent(antecedentNode);
                antecedentNode.addImplication(impliedNode);
            }
        }
    }

    //add conflicting assignment to the implication graph
    public void addConflictNode(Assignment assignment, Clause clause) {
        if (verbose == true) System.out.println("Conflict node assignment  : " + assignment);
        
        Node conflictNode = new Node(assignment);
        conflictNode.markAsConflictNode();
    
        // Add the conflict node to nodes map
        // Note the key to the map is negative
        // to allow a temporary duplicate assignment in the map
        nodes.put(assignment.getLiteral().getVariable() * -1, conflictNode);

        // Handle the conflict's antecedents (clauses)
        for (Literal literal : clause.getLiterals()) {
            int variable = literal.getVariable();
            Node antecedentNode = nodes.get(variable);

            if (antecedentNode != null) {
                // Add the antecedent node to the list of antecedents
                conflictNode.addAntecedent(antecedentNode);
                antecedentNode.addImplication(conflictNode);

             //   if (verbose == true) conflictNode.printAntecedents();
            }
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
                conflictNode = node;
                iterator.remove(); // Remove the conflict node from the map
                break; // Exit the loop once the conflict node is found
            }
        }

        // Remove references to the conflict node from antecedent nodes' implications
        for (Node antecedent : conflictNode.getAntecedents()) {
            antecedent.removeImplication(conflictNode);
        }
    }
    
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
    

    // Function to find all paths from the most recent decision node to the conflict node
    public List<List<Node>> findAllPathsFromDecisionToConflict(Node conflictNode) {
        List<List<Node>> allPaths = new ArrayList<>();
        Stack<Node> path = new Stack<>();
        Node decisionNode = decisionStack.peek();   // get most recent decision node
        dfsFromDecisionToConflict(decisionNode, conflictNode, allPaths, path);
        return allPaths;
    }

    // Depth-first search (DFS) to find all paths from decision node to conflict node
    private void dfsFromDecisionToConflict(Node currentNode, Node conflictNode, List<List<Node>> allPaths, Stack<Node> path) {
        // Push the current node onto the path stack
        path.push(currentNode);

        // If the current node is the conflict node, add the path to allPaths
        if (currentNode == conflictNode) {
            allPaths.add(new ArrayList<>(path));
        } else {
            // Recursively explore all implications
            for (Node implication : currentNode.getImplications()) {
                dfsFromDecisionToConflict(implication, conflictNode, allPaths, path);
            }
        }

        // Pop the current node from the path stack to backtrack
        path.pop();
    }
    public Clause createLearnedClause(Node UIP, Node conflictNode) {
        Clause clause = generateStartingClause(conflictNode); 
        return clause;
    }

    private Clause generateStartingClause(Node conflictNode) {
        Clause startingClause = new Clause();
        // Collect literals from the antecedent nodes
        for (Node antecedent : conflictNode.getAntecedents()) {
            startingClause.addLiteral(antecedent.getAssignment().getLiteral());
        }
        return startingClause;
    }


    //need to fix this function
    public void backtrack(int toDecisionLevel) {
        while (!decisionStack.isEmpty() && decisionStack.peek().getAssignment().getDecisionLevel() > toDecisionLevel) {
            Node node = decisionStack.pop();
            nodes.remove(node.getAssignment().getLiteral().getVariable());
        }
    }
}