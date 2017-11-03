
// File:   MH_Lexer.java
// Date:   October 2013, subsequently modified each year.

// Java template file for lexer component of Informatics 2A Assignment 1.
// Concerns lexical classes and lexer for the language MH (`Micro-Haskell').


import java.io.* ;

class MH_Lexer extends GenLexer implements LEX_TOKEN_STREAM {

static class VarAcceptor extends Acceptor implements DFA {
  public String lexClass() {return "VAR" ;} ;
  public int numberOfStates() {return 3 ;} ;

  int next (int state, char c) {
    switch (state) {
      case 0: if (CharTypes.isSmall(c)) return 1 ; else return 2 ;
      case 1: if (CharTypes.isSmall(c) || CharTypes.isLarge(c) || CharTypes.isDigit(c) || c == '\'') return 1 ; else return 2 ;
      default: return 2 ;
    }
  }
  boolean accepting (int state) {return (state == 1) ;}
  int dead () {return 2 ;}
}

static class NumAcceptor extends Acceptor implements DFA {
  public String lexClass() {return "NUM" ;} ;
  public int numberOfStates() {return 3 ;} ;

  int next (int state, char c) {
    switch (state) {
      case 0: if (CharTypes.isDigit(c)) return 1 ; else return 2 ;
      case 1: if (CharTypes.isDigit(c)) return 1 ; else return 2 ;
      default: return 2 ;
    }
  }
  boolean accepting (int state) {return (state == 1) ;}
  int dead () {return 2 ;}
}

static class BooleanAcceptor extends Acceptor implements DFA {
  public String lexClass() {return "BOOLEAN" ;} ;
  public int numberOfStates() {return 9 ;} ;

  int next (int state, char c) {
    switch (state) {
      case 0: if (c == 'T') return 1 ; else if (c=='F') return 4 ; else return 7;
      case 1: if (c == 'r') return 2 ; else return 7 ;
      case 2: if (c == 'u') return 3 ; else return 7 ;
      case 3: if (c == 'e') return 8 ; else return 7 ;
      case 4: if (c == 'a') return 5 ; else return 7 ;
      case 5: if (c == 'l') return 6 ; else return 7 ;
      case 6: if (c == 's') return 3 ; else return 7 ;
      default: return 7 ;
    }
  }
  boolean accepting (int state) {return (state == 8) ;}
  int dead () {return 7 ;}
}

static class SymAcceptor extends Acceptor implements DFA {
  public String lexClass() {return "SYM" ;} ;
  public int numberOfStates() {return 3 ;} ;

  int next (int state, char c) {
    switch (state) {
      case 0: if (CharTypes.isSymbolic(c)) return 1 ; else return 2 ;
      case 1: if (CharTypes.isSymbolic(c)) return 1 ; else return 2 ;
      default: return 2 ;
    }
  }
  boolean accepting (int state) {return (state == 1) ;}
  int dead () {return 2 ;}
}

static class WhitespaceAcceptor extends Acceptor implements DFA {
  public String lexClass() {return "" ;} ;
  public int numberOfStates() {return 3 ;} ;

  int next (int state, char c) {
    switch (state) {
      case 0: if (CharTypes.isWhitespace(c)) return 1 ; else return 2 ;
      case 1: if (CharTypes.isWhitespace(c)) return 1 ; else return 2 ;
      default: return 2 ;
    }
  }
  boolean accepting (int state) {return (state == 1) ;}
  int dead () {return 2 ;}
}

static class CommentAcceptor extends Acceptor implements DFA {
  public String lexClass() {return "" ;} ;
  public int numberOfStates() {return 5 ;} ;

  int next (int state, char c) {
    switch (state) {
      case 0: if (c == '-') return 1 ; else return 4 ;
      case 1: if (c == '-') return 2 ; else return 4 ;
      case 2: if (c == '-') return 2 ; else if(!CharTypes.isSymbolic(c) && !CharTypes.isNewline(c)) return 3 ; else return 4;
      case 3: if (!CharTypes.isNewline(c)) return 3; return 4;
      default: return 4 ;
    }
  }
  boolean accepting (int state) {return (state == 3 || state == 2) ;}
  int dead () {return 4 ;}

}

static class TokAcceptor extends Acceptor implements DFA {

    String tok ;
    int tokLen ;
    TokAcceptor (String tok) {this.tok = tok ; tokLen = tok.length();}

    public String lexClass() {return this.tok; };
	public int numberOfStates() {return this.tokLen + 2;};

	int next (int state, char c) {
		if (state < tokLen && tok.charAt(state) == c) {
			return state + 1;
		}
		else
		{
			return tokLen + 1;
		}
	}
	
	boolean accepting (int state) {return (state == tokLen) ;}
	int dead () {return (tokLen +1 );}
	
}
	static DFA num_acceptor = new NumAcceptor();
	static DFA boolean_acceptor = new BooleanAcceptor();
	static DFA whitespace_acceptor = new WhitespaceAcceptor();
	static DFA comment_acceptor = new CommentAcceptor();
	static DFA sym_acceptor = new SymAcceptor();
	static DFA var_acceptor = new VarAcceptor();

	static DFA bool_token = new TokAcceptor("Bool");
	static DFA if_token = new TokAcceptor("if");
	static DFA then_token = new TokAcceptor("then");
	static DFA else_token = new TokAcceptor("else");
	static DFA int_token = new TokAcceptor("Integer");
	static DFA semicolon_acceptor = new TokAcceptor(";");
	static DFA lbr_acceptor = new TokAcceptor("(");
	static DFA rbr_acceptor = new TokAcceptor(")");
	
    static DFA[] MH_acceptors = new DFA[] {
		bool_token, int_token, if_token, then_token, else_token, semicolon_acceptor, lbr_acceptor, rbr_acceptor, 
		var_acceptor, num_acceptor, boolean_acceptor, whitespace_acceptor, comment_acceptor, sym_acceptor
	};

    MH_Lexer (Reader reader) {
	     super(reader,MH_acceptors) ;
	     	        
    }
    public static void main (String[] args) 
     		throws StateOutOfRange, IOException {
     		BufferedReader consoleReader = new BufferedReader (new InputStreamReader (System.in)) ;
     	        while (0==0) {
     		    System.out.print ("Lexer> ") ;
     	            String inputLine = consoleReader.readLine() ;
     	            Reader lineReader = new BufferedReader (new StringReader (inputLine)) ;
     	            GenLexer demoLexer = new MH_Lexer (lineReader) ;
     	            try {
     		        LexToken currTok = demoLexer.pullProperToken() ;
     		        while (currTok != null) {
     		            System.out.println (currTok.value() + " \t" + 
     			     		        currTok.lexClass()) ;
     		            currTok = demoLexer.pullProperToken() ;
     	                }
     	            } catch (LexError x) {
     			System.out.println ("Error: " + x.getMessage()) ;
     	            }
     		} 
    }
    


}
