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

public class SatUtil {
    public static boolean checkSAT(Expr expr)
    {   
        //converts CNF formula to a list of Clause objects
        List<Clause> clauses = CNFConverter.convertToClauses(expr);
        CDCLSolver solver = new CDCLSolver(clauses);
       
        //printing out the clauses for reference
        CNFConverter.printClauses(solver.clauses);
        return solver.solve();

    }
}
