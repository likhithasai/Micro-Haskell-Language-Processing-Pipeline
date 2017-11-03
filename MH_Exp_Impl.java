
// File:   Expressions.java
// Date:   October 2013

// Java source file provided for Informatics 2A Assignment 1 (2013).
// Provides abstract syntax trees for expressions in Micro-Haskell:
// effectively, trees for the simplified grammar

//     Exp  -->   VAR | NUM | BOOLEAN | Exp Exp | Exp infix Exp
//                | if Exp then Exp else Exp

import java.util.* ;


interface MH_EXP {
    boolean isVAR() ;
    boolean isNUM() ;
    boolean isBOOLEAN() ;
    boolean isAPP() ;
    boolean isINFIX() ;
    boolean isIF() ;
    boolean isLAMBDA() ; 
    boolean isREF() ;
    // LAMBDA and REF only needed for evaluator: MH itself doesn't have these.
    // REF used for potentially shared subexpressions (to implement call-by-need)

    String value() ;     
    // for VAR, NUM, BOOLEAN: returns e.g. "x", "5", "True"
    String infixOp() ;     
    // for infix expressions: returns "==", "<=", "+" or "-"
    int index() ;
    // for REF expressions
    MH_EXP first() ;     
    // returns first child (for application, infix, if-expressions, ref)
    MH_EXP second() ;    
    // returns second child (for application, infix, if-expressions)
    MH_EXP third() ;     
    // returns third child (for if-expressions only)

    String toString (Vector<MH_EXP> bank) ;  // for debugging
}

// Examples illustrating how the MH_Exp operations are used:
// If e is the MH_EXP tree for "if e_1 then e_2 else e_3", then
//   - e.first() returns the tree for e_1
//   - e.second() returns the tree for e_2
//   - e.third() returns the tree for e_3
//   - e.value() and e.infixOp() won't return anything sensible.
// If e is the MH_EXP tree for "e_1 e_2" (function application), then
//   - e.first() returns the tree for e_1
//   - e.second() returns the tree for e_2
//   - e.third(), e.value() and e.infixOp() won't return anything sensible
// If e is the MH_EXP tree for "e_1 == e_2", then
//   - e.first() returns the tree for e_1
//   - e.second() returns the tree for e_2
//   - e.infixOp() returns the string "==", identifying the infix involved
//   - e.third() and e.value() won't return anything sensible
// and similarly for the other infix operations.
// If e is the MH_EXP tree for a VAR, NUM or BOOLEAN, then
//   - e.value() returns its string representation, e.g. "x" or "42" or "True"
//   - e.first(), e.second(), e.third(), e.infixOp() won't return 
//     anything sensible.

public class MH_Exp_Impl implements MH_EXP {
    private int kind ;
    private String value ;
    private String infixOp ;
    private int index;
    private MH_EXP firstChild ;
    private MH_EXP secondChild ;
    private MH_EXP thirdChild ;
    public boolean isVAR() {return kind==0 ;}
    public boolean isNUM() {return kind==1 ;}
    public boolean isBOOLEAN() {return kind==2 ;}
    public boolean isAPP() {return kind==3 ;}
    public boolean isINFIX() {return kind==4 ;}
    public boolean isIF() {return kind==5 ;}
    public boolean isLAMBDA() {return kind==6 ;}
    public boolean isREF() {return kind==7 ;}
    public String value() {return value ;}
    public String infixOp() {return infixOp ;}
    public int index() {return index ;}
    public MH_EXP first() {return firstChild ;}
    public MH_EXP second() {return secondChild ;}
    public MH_EXP third() {return thirdChild ;}

    // Various constructors: number and type of arguments determine
    // the kind of expression.

    // For atomic expressions (VAR, NUM, BOOLEAN)
    MH_Exp_Impl (String lexClass, String value) {
	this.value = value ;
	if (lexClass.equals("VAR")) kind=0 ;
	else if (lexClass.equals("NUM")) kind=1 ;
	else if (lexClass.equals("BOOLEAN")) kind=2 ;
	else {
	    System.out.println ("Warning: unknown lexClass " + lexClass) ;
	    kind=-1 ; 
	}
    }

    // For applications
    MH_Exp_Impl (MH_EXP left, MH_EXP right) {
	this.kind = 3 ;
	this.firstChild = left ;
	this.secondChild = right ;
    }

    // For infix expressions
    MH_Exp_Impl (MH_EXP left, String infixOp, MH_EXP right) {
	this.kind = 4 ;
	this.firstChild = left ;
	this.secondChild = right ;
	this.infixOp = infixOp ;
    }

    // For if-expressions
    MH_Exp_Impl (MH_EXP condition, MH_EXP branch1, MH_EXP branch2) {
	this.kind = 5 ;
	this.firstChild = condition ;
	this.secondChild = branch1 ;
	this.thirdChild = branch2 ;
    }

    // For lambda-expressions
    MH_Exp_Impl (String var, MH_EXP body) {
	this.kind = 6 ;
	this.value = var ;
	this.firstChild = body ;
    }

    // For ref expressions
    MH_Exp_Impl (int index) {
        this.kind = 7 ;
        this.index = index ;
    }

    public String toString (Vector<MH_EXP> bank) {
        switch (this.kind) {
        case 0: return this.value ;
        case 1: return this.value ;
        case 2: return this.value ;
        case 3: return "(" + this.firstChild.toString(bank) + " " + this.secondChild.toString(bank) + ")" ;
        case 4: return "(" + this.firstChild.toString(bank) + this.infixOp + 
                       this.secondChild.toString(bank) + ")" ;
        case 5: return "(if " + this.firstChild.toString(bank) + " then " + 
                       this.secondChild.toString(bank) +
                       " else " + this.thirdChild.toString(bank) + ")" ;
        case 6: return "(\\" + this.value + " -> " + this.firstChild.toString(bank) + ")" ;
        case 7: return "[" + this.index + "]" + bank.get(this.index).toString(bank) ;
        default: return null ;
        }
    }


    // Converting parse trees to ASTs for expressions

    static MH_Parser MH_Parser1 = MH_Type_Impl.MH_Parser1 ;

    static class TaggedExp {
	MH_EXP exp ;
	String tag ;
	TaggedExp (MH_EXP exp, String tag) {
	    this.exp = exp ; this.tag = tag ;
	}
    }

    static MH_EXP convertExp (TREE exp) {
	if (exp.getLabel().equals("#Exp3")) {
	    if (exp.getRhs() == MH_Parser.lbr_Exp_rbr) 
		return convertExp (exp.getChildren()[1]) ;
	    else {
		TREE terminal = exp.getChildren()[0] ;
		// build atomic expression
		return new MH_Exp_Impl 
		    (terminal.getLabel(), terminal.getValue()) ;
	    }
	} else if (exp.getLabel().equals("#Exp2")) {
	    MH_EXP head = convertExp (exp.getChildren()[0]) ;
	    Stack rest = convertOps2 (exp.getChildren()[1]) ;
	    while (! rest.isEmpty()) {
		// build application expression
		head = new MH_Exp_Impl (head, (MH_EXP)(rest.pop())) ;
	    } ;
	    return head ;
	} else if (exp.getLabel().equals("#Exp1")) {
	    MH_EXP head = convertExp (exp.getChildren()[0]) ;
	    Stack rest = convertOps1 (exp.getChildren()[1]) ;
	    while (! rest.isEmpty()) {
		// build "+" or "-" infix expression
		TaggedExp tt = (TaggedExp)rest.pop() ;
		head = new MH_Exp_Impl (head, tt.tag, tt.exp) ;
	    } ;
	    return head ;
	} else if (exp.getLabel().equals("#Exp0")) {
	    MH_EXP head = convertExp (exp.getChildren()[0]) ;
	    TREE op1 = exp.getChildren()[1] ;
	    if (op1.getRhs() == MH_Parser.epsilon) 
		return head ;
	    else {
		MH_EXP other = convertExp (op1.getChildren()[1]) ;
		String op = op1.getChildren()[0].getLabel() ;
		// build "==" or "<=" infix expression
		return new MH_Exp_Impl (head, op, other) ;
	    }
	} else if (exp.getLabel().equals("#Exp")) {
	    if (exp.getRhs() == MH_Parser.Exp0) 
		return convertExp (exp.getChildren()[0]) ;
	    else // construct if-expression
		return new MH_Exp_Impl
		    (convertExp (exp.getChildren()[1]),
		     convertExp (exp.getChildren()[3]),
		     convertExp (exp.getChildren()[5])) ;				       
	} else {
	    System.out.println ("Unexpected label " + exp.getLabel()) ;
	    return null ;
	}
    }

    static Stack<MH_EXP> convertOps2 (TREE ops2) {
	if (ops2.getRhs() == MH_Parser.epsilon) 
	    return new Stack<MH_EXP>() ;
	else {
	    MH_EXP exp = convertExp (ops2.getChildren()[0]) ;
	    Stack<MH_EXP> stack = convertOps2 (ops2.getChildren()[1]) ;
	    stack.push(exp) ;
	    return stack ;
	}
    }

    static Stack<TaggedExp> convertOps1 (TREE ops1) {
	if (ops1.getRhs() == MH_Parser.epsilon) 
	    return new Stack<TaggedExp>() ;
	else {
	    MH_EXP exp = convertExp (ops1.getChildren()[1]) ;
	    String tag = ops1.getChildren()[0].getLabel()  ;
	    Stack<TaggedExp> stack = convertOps1 (ops1.getChildren()[2]) ;
	    stack.push (new TaggedExp(exp,tag)) ;
	    return stack ;
	}
    }

}


// Expression environments, associating names with closures.
// For use by runtime system.

class MH_Exp_Env {
    private java.util.TreeMap env ;
    MH_Exp_Env (java.util.TreeMap env) {this.env = env ;}
    public MH_EXP valueOf (String var) throws UnknownVariable {
	MH_EXP e = (MH_EXP)env.get(var) ;
	if (e == null) throw new UnknownVariable(var) ;
	else return e ;
    }
}
