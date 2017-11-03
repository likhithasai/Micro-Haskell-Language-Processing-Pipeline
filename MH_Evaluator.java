
// File:   MH_Evaluator.java
// Date:   October 2013, modified October 2016

// Java template file for typechecker component of Informatics 2A Assignment 1.
// Rudimentary runtime system for the MH language.
// Illustrates the ideas of small-step operational semantics (Lecture 27!).

import java.util.* ;
import java.math.* ;
import java.io.* ;

// Small modification of parser needed to parse standalone expressions:

class Expr_Parser extends MH_Parser implements PARSER {

    String[] tableEntry (String nonterm, String tokClass) {
	if ((tokClass == null) && 
	    (nonterm.equals("#Prog") || nonterm.equals("#TypeOps") ||
             nonterm.equals("#Args") || nonterm.equals("#Rest0") ||
             nonterm.equals("#Rest1") || nonterm.equals("#Rest2"))) 
           return super.epsilon ;
        else return super.tableEntry (nonterm,tokClass) ;
    }
}

// Added 2016: Class providing explicit support for shared subexpressions (using REF).
// Used to implement call-by-need evaluation.

class Exp_Bank {

    Vector<MH_EXP> shared ;  // kept non-private for debugging purposes

    MH_EXP lookup (int i) {
	if (i < shared.size()) return shared.get(i) ;
        else return null ;
    }

    int add (MH_EXP e) {
        shared.add(e) ;
        return shared.size() - 1 ;
    }

    void update (int i, MH_EXP e) {
        shared.set(i,e) ;
    }

    Exp_Bank () {
        shared = new Vector<MH_EXP>(10) ; // initial size
    }
}


class MH_Evaluator {

    static Exp_Bank exp_bank ;   // created afresh for each call to evaluate

    static boolean reducible (MH_EXP e) {
        // if (e.isREF()) return reducible (exp_bank.lookup (e.index())) ; else
	return ! (e.isNUM () || e.isBOOLEAN() || e.isLAMBDA()) ;
    }

    static int chaseREFs (int i) {
        MH_EXP a = exp_bank.lookup(i) ;
        if (a.isREF()) return chaseREFs (a.index()) ;
        else return i ;
    }

    static MH_EXP subst (MH_EXP e, String v1, MH_EXP e1) 
	throws RuntimeError {
	// returns e with e1 substituted for all free occurrences of
	// the variable v1
        return subst1 (e,v1,e1) ;
    }

    static MH_EXP subst1 (MH_EXP e, String v1, MH_EXP e1) 
	throws RuntimeError {
	if (e.isVAR()) {
	    if (e.value().equals(v1)) {
               /* lastRewriteTime = time ; */
	       return e1 ;
	    } else return e ;
	} else if (e.isNUM() || e.isBOOLEAN()) {
	    return e ;
	} else if (e.isAPP()) {
	    return new MH_Exp_Impl 
		(subst1 (e.first(),v1,e1), subst1 (e.second(),v1,e1)) ;
	} else if (e.isINFIX()) {
	    return new MH_Exp_Impl 
		(subst1 (e.first(),v1,e1), e.infixOp(), 
		 subst1 (e.second(),v1,e1)) ;
	} else if (e.isIF()) {
	    return new MH_Exp_Impl 
		(subst1 (e.first(),v1,e1), subst1 (e.second(),v1,e1),
		 subst1 (e.third(),v1,e1)) ;
	} else if (e.isLAMBDA()) {
	    String v2 = e.value() ;
	    if (v2.equals(v1)) return e ;
	    else return new MH_Exp_Impl (v2, subst1 (e.first(),v1,e1)) ;
        } else if (e.isREF()) {
            return e ;   // expr referenced by i is closed - no action required
	} else throw new RuntimeError() ;
    }

    static String getValue (MH_EXP e) {
        if (e.isREF()) return getValue (exp_bank.lookup (e.index())) ;
        else return e.value() ;
    }

    static MH_EXP reduce (MH_EXP e, MH_Exp_Env env) 
	throws RuntimeError, UnknownVariable {
	if (e.isVAR()) {
	    return env.valueOf(e.value()) ;
	} else if (e.isINFIX()) {
	    MH_EXP e1 = e.first() ;
	    MH_EXP e2 = e.second() ;
	    String i = e.infixOp() ;
	    if (reducible(e1)) {
		return new MH_Exp_Impl (reduce(e1,env),i,e2) ;
	    } else if (reducible(e2)) {
		return new MH_Exp_Impl (e1,i,reduce(e2,env)) ;
	    } else {
		BigInteger v1 = new BigInteger (getValue(e1)) ;
		BigInteger v2 = new BigInteger (getValue(e2)) ;
		switch (i) {
		case "+": return new MH_Exp_Impl 
			("NUM", v1.add(v2).toString()) ;
		case "-": return new MH_Exp_Impl
 			("NUM", v1.subtract(v2).toString()) ;
		case "==": return new MH_Exp_Impl
			("BOOLEAN", (v1.equals(v2)?"True":"False")) ;
		case "<=": return new MH_Exp_Impl
			("BOOLEAN", (v1.compareTo(v2)<=0?"True":"False")) ;
		default: throw new RuntimeError() ;
		}
	    }
	} else if (e.isIF()) {
	    MH_EXP e1 = e.first() ;
	    MH_EXP e2 = e.second() ;
	    MH_EXP e3 = e.third() ;
	    if (reducible(e1)) {
		return new MH_Exp_Impl (reduce(e1,env),e2,e3) ;
	    } else if (getValue(e1).equals("True")) {
		return e2 ;
	    } else if (getValue(e1).equals("False")) {
		return e3 ;
	    } else throw new RuntimeError() ;
	} else if (e.isAPP()) {
	    MH_EXP e1 = e.first() ;
	    MH_EXP e2 = e.second() ;
	    if (reducible(e1)) {
		return new MH_Exp_Impl (reduce(e1,env),e2) ;
	    } else if (e1.isLAMBDA()) {  
		String var = e1.value() ;
		MH_EXP body = e1.first() ;
                // For call-by-name:
                // return subst (body,var,e2) ;
	        // For call-by-need: register e2 as a potentially shared subexpression if it's not already
                if (e2.isREF()) return subst (body,var,e2) ;
                else {
                    int i2 = exp_bank.add (e2) ;
                    MH_EXP r2 = new MH_Exp_Impl (i2) ;
		    return subst (body,var,r2) ;
                }
	    } else throw new RuntimeError() ;
        } else if (e.isREF()) {  // for call-by-need
            int i = e.index() ;
            MH_EXP d = exp_bank.lookup (i) ;
	    if (reducible(d)) {
                MH_EXP d1 = reduce(d,env) ;
                if (d1.isREF()) {
                    // eliminate needless ref chains
                    int k1 = chaseREFs(d1.index()) ; 
                    MH_EXP r1 = new MH_Exp_Impl (k1) ;
                    exp_bank.update (i,r1) ;
                    return r1 ;
                } else {
                    exp_bank.update (i,d1) ;
                    return e ;
                }
            } else {
                return d ;
            }
	} else throw new RuntimeError() ;
    }

    static MH_EXP evaluate (MH_EXP e, MH_Exp_Env env) 
	throws RuntimeError, UnknownVariable {
        exp_bank = new Exp_Bank() ;
	MH_EXP d = e ;
	while (reducible(d)) {
            // System.out.println ("** " + d.toString(exp_bank.shared)) ;  // for debugging
	    d = reduce(d,env) ;
	} ;
	return d ;
    }

    static String printForm (MH_EXP e) {
	if (e.isNUM() || e.isBOOLEAN()) return e.value() ;
        else if (e.isREF()) return printForm (exp_bank.lookup (e.index())) ;
	else return "-" ;
    }

    static class RuntimeError extends Exception {} ;


// Interactive loop:

    public static void main (String[] args) throws Exception {
	// processes MH program from specified file and then enters
	// interactive read-eval loop.

	Reader fileReader = new BufferedReader (new FileReader (args[0])) ;
        PARSER File_Parser = MH_Type_Impl.MH_Parser1 ;
        PARSER Expr_Parser = new Expr_Parser() ;
	MH_Typechecker.MH_Type_Env typeEnv = null ;
	MH_Exp_Env runEnv = null ;
	// load MH definitions from specified file
	try {
	    LEX_TOKEN_STREAM MH_Lexer = 
		new CheckedSymbolLexer (new MH_Lexer (fileReader)) ;
	    TREE prog = File_Parser.parseTokenStream (MH_Lexer) ;
	    typeEnv = MH_Typechecker.compileTypeEnv (prog) ;
	    runEnv = MH_Typechecker.typecheckProg (prog, typeEnv) ;
	} catch (Exception x) {
	    System.out.println ("MH Error: " + x.getMessage()) ;
	}
	if (runEnv != null) {
	    BufferedReader consoleReader = 
		new BufferedReader (new InputStreamReader (System.in)) ;
	    // Enter interactive read-eval loop
	    while (0==0) {
                System.out.print ("\n") ;
                String inputLine ;
                do {
 		    System.out.print ("MH> ") ;
		    inputLine = consoleReader.readLine().trim() ;
                } while (inputLine.isEmpty()) ;
		MH_EXP e = null ;
		MH_TYPE t = null ;
		// lex, parse and typecheck one line of console input
		try {
		    Reader lineReader = 
			new BufferedReader (new StringReader (inputLine)) ;
		    LEX_TOKEN_STREAM lineLexer =
			new CheckedSymbolLexer (new MH_Lexer (lineReader)) ;
		    TREE exp = Expr_Parser.parseTokenStreamAs
			(lineLexer, "#Exp");
                    e = MH_Exp_Impl.convertExp (exp) ;
		    t = MH_Typechecker.computeType (e,typeEnv) ;
		} catch (Exception x) {
		    System.out.println ("MH Error: " + x.getMessage()) ;
		} ;
		if (t != null) {
		    // display type
		    System.out.println ("  it :: " + t.toString()) ;
		    // evaluate expression
		    MH_EXP e1 = evaluate (e,runEnv) ;
		    // display value
		    System.out.println ("  it  = " + printForm(e1)) ;
		}
	    }
	}
    }
}

