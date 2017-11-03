
// File:   Types.java
// Date:   October 2013

// Java source file provided for Informatics 2A Assignment 1 (2013).
// Provides abstract syntax trees for types in Micro-Haskell:
// effectively, trees for the simplified grammar
//     Type  -->   Integer | Bool | Type->Type


interface MH_TYPE {
    boolean isInteger() ;
    boolean isBool() ;
    boolean isFun() ;
    MH_TYPE left() ;    // returns left constituent of arrow type
    MH_TYPE right() ;   // returns right constituent
    boolean equals (MH_TYPE other) ;
    String toString() ; // for testing/debugging
}

public class MH_Type_Impl implements MH_TYPE {
    private int kind ;
    private MH_TYPE leftChild ;
    private MH_TYPE rightChild ;
    public boolean isInteger() {return kind==0 ;}
    public boolean isBool() {return kind==1 ;}
    public boolean isFun() {return kind==2 ;}
    public MH_TYPE left() {return leftChild ;}
    public MH_TYPE right() {return rightChild ;}

    public boolean equals (MH_TYPE other) {
	return ((this.isInteger() && other.isInteger()) ||
		(this.isBool() && other.isBool()) ||
		(this.isFun() && other.isFun() && 
		 this.left().equals(other.left()) &&
		 this.right().equals(other.right()))) ;
    }

    public String toString () {
	if (this.isInteger()) return "Integer" ;
	else if (this.isBool()) return "Bool" ;
	else return ("(" + this.left().toString() + " -> "
		     + this.right().toString() + ")") ;
    }

    // Constructors
    private MH_Type_Impl (int kind, MH_TYPE leftChild, MH_TYPE rightChild) {
	this.kind = kind ;
	this.leftChild = leftChild ;
	this.rightChild = rightChild ;
    } ;

    // Constants for MH types Integer and Bool
    public static MH_TYPE IntegerType = new MH_Type_Impl (0,null,null) ;
    public static MH_TYPE BoolType = new MH_Type_Impl (1,null,null) ;
    // Constructor for arrow types
    MH_Type_Impl (MH_TYPE leftChild, MH_TYPE rightChild) {
	this (2, leftChild, rightChild) ;
    }

    // Conversion from parse trees to ASTs for MH types

    // convertType accepts any well-formed tree whose root node has label
    // #Type, and returns the corresponding abstract syntax tree 
    // (see Types.java for the definition of ASTs for types).
    // convertType1 does the same for #Type1.
    // The relevant LL(1) grammar rules are:

    // #Type    -> #Type1 #TypeOps
    // #Type1   -> Integer | Bool | ( #Type )
    // #TypeOps -> epsilon | -> #Type

    // Since trees with label #Type can have subtrees of label #Type1
    // and vice versa, these two methods are mutually recursive.
    
    static MH_Parser MH_Parser1 = new MH_Parser() ;

    static MH_TYPE convertType (TREE tree) {
	if (tree.getChildren()[1].getRhs() == MH_Parser.epsilon) 
	    { // Case if TypeOps is empty
	    return convertType1 (tree.getChildren()[0]) ;
	    } else { // Case if TypeOps is -> Type
	    MH_TYPE left = convertType1 (tree.getChildren()[0]) ;
	    MH_TYPE right = convertType 
		(tree.getChildren()[1].getChildren()[1]) ;
	    return new MH_Type_Impl (left,right) ;
	} 
    }

    static MH_TYPE convertType1 (TREE tree1) {
	if (tree1.getRhs() == MH_Parser.Integer) { 
	    return MH_Type_Impl.IntegerType ;
	} else if (tree1.getRhs() == MH_Parser.Bool) {
	    return MH_Type_Impl.BoolType ;
	} else // This covers case in which tree1 matches ( Type )
            return convertType (tree1.getChildren()[1]) ;
    }

}

// Errors that may arise during typechecking:

class TypeError extends Exception {
    TypeError (String s) {super ("Type error: " + s) ;}
}

class UnknownVariable extends Exception {
    public UnknownVariable (String var) {
	super("Variable " + var + " not in scope.") ;
    }
}

class DuplicatedVariable extends Exception {
    public DuplicatedVariable (String var) {
	super("Duplicated variable " + var) ;
    }
}

class NameMismatchError extends Exception {
    public NameMismatchError (String var1, String var2) {
	super("Name mismatch between " + var1 + " and " + var2) ;
    }
}

