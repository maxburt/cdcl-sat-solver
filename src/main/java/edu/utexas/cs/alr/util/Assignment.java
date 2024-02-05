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

//This class is used to represent when a variable gets assigned a value
//in the CDCLsolver function there is a stack of these objects.
//The stack keeps track of of all decisions and implications and is essential
//for backtracking and creating the conflic clauses effectively.
class Assignment {
    private final Literal literal; // The literal being assigned
    private final boolean value; // The assigned truth value (true or false)
    private final int decisionLevel; // The decision level at which the assignment was made
    private final AssignmentType type; // Enum to indicate decision or implication
    
    //constructor
    public Assignment(Literal literal, boolean value, int decisionLevel, AssignmentType type) {
        this.literal = literal;
        this.value = value;
        this.decisionLevel = decisionLevel;
        this.type = type;
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

    public boolean isDecision() {
        return type == AssignmentType.DECISION;
    }
    
    public boolean isImplication() {
        return type == AssignmentType.IMPLICATION;
    }
    
    public AssignmentType getType() {
        return type;
    }

    public enum AssignmentType {
        DECISION,
        IMPLICATION
    }

    @Override
    public String toString() {
        // Represents the literal assignment in a readable format
        String literalRepresentation = (literal.isNegated() ? "!" : "") + "x" + literal.getVariable();
        literalRepresentation += (this.type == Assignment.AssignmentType.DECISION ? "  Decision" : "  Implication");
        return literalRepresentation + " = " + value + " at level " + decisionLevel;
    }
}