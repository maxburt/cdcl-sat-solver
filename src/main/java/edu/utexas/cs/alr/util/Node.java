package edu.utexas.cs.alr.util;

import java.util.*;

public class Node {
    private Assignment assignment;
    private List<Node> antecedents; // Nodes that imply this node, if a decision node, remains blank
    private List<Node> implications; // Nodes that this node implies
    private boolean isConflictNode; // marker for if its a conflict node

    public Node(Assignment assignment) {
        this.assignment = assignment;
        this.antecedents = new ArrayList<>();
        this.implications = new ArrayList<>();
        this.isConflictNode = false;
    }

    public Node() {
        this.assignment = null;
        this.antecedents = new ArrayList<>();
        this.implications = new ArrayList<>();
        this.isConflictNode = false;
    }

    public void markAsConflictNode() {
        this.isConflictNode = true;
    }

    public void addAntecedent(Node antecedent) {
        antecedents.add(antecedent);
    }

    public void addImplication(Node impliedNode) {
        implications.add(impliedNode);
    }

    public void removeImplication(Node removeMe) {
        implications.remove(removeMe);
    }

    public boolean isConflictNode() {
        return isConflictNode;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public List<Node> getAntecedents() {
        return antecedents;
    }

    public List<Node> getImplications() {
        return implications;
    }

    public void printAntecedents() {
        System.out.println("\tAntecedents for this node are ... ");
        for (Node antecedent : antecedents) {
            System.out.println("\t" + antecedent.assignment);
        }
    }

    public void printAntecedentsShort() {
        for (Node antecedent : antecedents) {
            System.out.print("\t" + antecedent.assignment.getLiteral());
        }
        System.out.println();
    }

    public void printImplications() {
        System.out.println("Implications for this node are ... ");
        for (Node implication : implications) {
            if (implication.isConflictNode()) {
                System.out.println("\t" + "Conflict node");
            } else {
                System.out.println("\t" + implication.assignment);
            }
        }
    }
}
