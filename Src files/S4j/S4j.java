/* Generated By:JavaCC: Do not edit this line. S4j.java */
import java.io.*;
import java.util.ArrayList;
class S4j implements S4jConstants {
  private PrintWriter outFile;
  private S4jSymTab st;
  private S4jCodeGen cg;
  //-----------------------------------------
  public static void main(String[] args) throws
                                             IOException
  {
    System.out.println("S4j compiler written by ...");
        System.out.println("Jackson Maddox");
    System.out.println("Liangyu Tan");
    System.out.println("Johirul Islam");

    if (args.length != 1)
    {
      System.err.println("Wrong number cmd line args");
      System.exit(1);
    }

    // build input and output file names
    String inFileName = args[0] + ".s";
    String outFileName = args[0] + ".a";

    // construct file objects
    FileInputStream inFile =
                       new FileInputStream(inFileName);
    PrintWriter outFile = new PrintWriter(outFileName);

    // identify compiler/author in output file
    outFile.println("; from S4j compiler written by ...");
        outFile.println("; Jackson Maddox");
        outFile.println("; Liangyu Tan");
        outFile.println("; Johirul Islam");

    // construct objects that make up compiler
    S4jSymTab st = new S4jSymTab();
    S4jCodeGen cg = new S4jCodeGen(outFile, st);
    S4j parser = new S4j(inFile);

    // initialize parser's instance variables
    parser.outFile = outFile;
    parser.st = st;
    parser.cg = cg;

    try
    {
      parser.program();
    }
    catch(ParseException e)
    {
      System.err.println(e.getMessage());
      outFile.println(e.getMessage());
      outFile.close();
      System.exit(1);
    }

    outFile.close();
  }

  private RuntimeException genEx(String errorMessage)
  {
        Token currentToken = getToken(1);
    return new RuntimeException("Encountered \u005c"" +
      currentToken.image + "\u005c" on line " +
      currentToken.beginLine + ", column " +
      currentToken.beginColumn + "." +
      System.getProperty("line.separator") +
      errorMessage);
  }
  //-----------------------------------------
  // If COMMON_TOKEN_ACTION is true, the token manager 
  // calls makeComment for each token to create the
  // token trace.
  //
  public void makeComment(Token t)
  {
    outFile.printf(
      "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im= %s%n",
       t.kind, t.beginLine, t.beginColumn, t.endLine,
       t.endColumn, t.image);
  }

  public void makeComment(String s)
  {
    outFile.println("; " + s);
  }

  public void makeComment(Token t1, Token t2)
  {
    outFile.print(";");
        while(t1 != t2){
                outFile.print(t1.image + " ");
                t1 = t1.next;
        }
        outFile.println();
  }

// Translation grammar for S4
  final public void program() throws ParseException {
    statementList();
    cg.endCode();
    jj_consume_token(0);
  }

//------------------------------
  final public void statementList() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case DO:
    case IF:
    case WHILE:
    case READINT:
    case PRINT:
    case PRINTLN:
    case ID:
    case SEMICOLON:
    case LEFTBRACE:
      statement();
      statementList();
      break;
    default:
      jj_la1[0] = jj_gen;

    }
  }

//------------------------------
  final public void statement() throws ParseException {
                   Token t; boolean outComment;
   t = getToken(1); outComment = true;
    try {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case ID:
        assignmentStatement();
        break;
      case PRINTLN:
        printlnStatement();
        break;
      case PRINT:
        printStatement();
        break;
      case SEMICOLON:
        nullStatement();
        break;
      case LEFTBRACE:
        compoundStatement();
    outComment = false;
        break;
      case READINT:
        readintStatement();
        break;
      case WHILE:
        whileStatement();
        break;
      case DO:
        doWhileStatement();
        break;
      case IF:
        ifStatement();
        break;
      default:
        jj_la1[1] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
  if (outComment) makeComment(t, getToken(1));
    } catch (ParseException e) {
        System.err.println(e.getMessage());
        cg.emitString(e.getMessage());

        while(getToken(1).kind != SEMICOLON && getToken(1).kind != EOF)
                getNextToken();
        if(getToken(1).kind != EOF)
                getNextToken();
    }
  }

//------------------------------
  final public void whileStatement() throws ParseException {
                        String label1, label2;
    jj_consume_token(WHILE);
    label1 = cg.getLabel();
    cg.emitLabel(label1);
    jj_consume_token(LEFTPAREN);
    expr();
    jj_consume_token(RIGHTPAREN);
    label2 = cg.getLabel();
    cg.emitInstruction("jz", label2);
    statement();
    cg.emitInstruction("ja", label1);
    cg.emitLabel(label2);
  }

//------------------------------
  final public void doWhileStatement() throws ParseException {
                         String label1;
    jj_consume_token(DO);
         label1 = cg.getLabel();
     cg.emitLabel(label1);
    statement();
    jj_consume_token(WHILE);
    jj_consume_token(LEFTPAREN);
    expr();
    jj_consume_token(RIGHTPAREN);
    jj_consume_token(SEMICOLON);
     cg.emitInstruction("jnz", label1);
  }

//------------------------------
  final public void ifStatement() throws ParseException {
                    String label1;
    jj_consume_token(IF);
    jj_consume_token(LEFTPAREN);
    expr();
    jj_consume_token(RIGHTPAREN);
     label1 = cg.getLabel();
     cg.emitInstruction("jz", label1);
    statement();
    elsePart(label1);
  }

//------------------------------
  final public void elsePart(String label1) throws ParseException {
                              String label2;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case ELSE:
      jj_consume_token(ELSE);
       label2 = cg.getLabel();
       cg.emitInstruction("ja", label2);
       cg.emitLabel(label1);
      statement();
       cg.emitLabel(label2);
      break;
    default:
      jj_la1[2] = jj_gen;
       cg.emitLabel(label1);
    }
  }

//------------------------------
  final public void assignmentStatement() throws ParseException {
                             Token t;
    t = jj_consume_token(ID);
    st.enter(t.image);
    cg.emitInstruction("pc", t.image);
    jj_consume_token(ASSIGN);
    assignmentTail();
    cg.emitInstruction("stav");
  }

//------------------------------
  final public void assignmentTail() throws ParseException {
                         Token t;
    if (jj_2_1(2)) {
      t = jj_consume_token(ID);
         st.enter(t.image);
         cg.emitInstruction("pc", t.image);
      jj_consume_token(ASSIGN);
      assignmentTail();
         cg.emitInstruction("dupe");
         cg.emitInstruction("rot");
         cg.emitInstruction("stav");
    } else {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case UNSIGNED:
      case ID:
      case LEFTPAREN:
      case PLUS:
      case MINUS:
        expr();
        jj_consume_token(SEMICOLON);
        break;
      default:
        jj_la1[3] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
  }

//------------------------------
  final public void printlnStatement() throws ParseException {
    jj_consume_token(PRINTLN);
    jj_consume_token(LEFTPAREN);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case UNSIGNED:
    case ID:
    case LEFTPAREN:
    case PLUS:
    case MINUS:
    case STRING:
      printArg();
      break;
    default:
      jj_la1[4] = jj_gen;

    }
    cg.emitInstruction("pc", "'\u005c\u005cn'");
    cg.emitInstruction("aout");
    jj_consume_token(RIGHTPAREN);
    jj_consume_token(SEMICOLON);
  }

//------------------------------
  final public void printStatement() throws ParseException {
    jj_consume_token(PRINT);
    jj_consume_token(LEFTPAREN);
    printArg();
    jj_consume_token(RIGHTPAREN);
    jj_consume_token(SEMICOLON);
  }

//------------------------------
  final public void printArg() throws ParseException {
                  Token t; String label;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case STRING:
      t = jj_consume_token(STRING);
    label = cg.getLabel();
    cg.emitInstruction("pc", label);
    cg.emitInstruction("sout");
    cg.emitdw("^" + label, t.image);
      break;
    case UNSIGNED:
    case ID:
    case LEFTPAREN:
    case PLUS:
    case MINUS:
      expr();
    cg.emitInstruction("dout");
      break;
    default:
      jj_la1[5] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

//------------------------------
  final public void nullStatement() throws ParseException {
    jj_consume_token(SEMICOLON);
  }

//------------------------------
  final public void compoundStatement() throws ParseException {
    jj_consume_token(LEFTBRACE);
    makeComment("{");
    statementList();
    jj_consume_token(RIGHTBRACE);
    makeComment("}");
  }

//------------------------------
  final public void expr() throws ParseException {
    term();
    termList();
  }

//------------------------------
  final public void termList() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case PLUS:
      jj_consume_token(PLUS);
      term();
    cg.emitInstruction("add");
      termList();
      break;
    case MINUS:
      jj_consume_token(MINUS);
      term();
    cg.emitInstruction("sub");
      termList();
      break;
    default:
      jj_la1[6] = jj_gen;

    }
  }

//------------------------------
  final public void term() throws ParseException {
    factor();
    factorList();
  }

//------------------------------
  final public void factorList() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case TIMES:
      jj_consume_token(TIMES);
      factor();
    cg.emitInstruction("mult");
      factorList();
      break;
    case FORWARDSLASH:
      jj_consume_token(FORWARDSLASH);
      factor();
    cg.emitInstruction("div");
      factorList();
      break;
    default:
      jj_la1[7] = jj_gen;

    }
  }

//------------------------------
  final public void factor() throws ParseException {
                Token t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case UNSIGNED:
      t = jj_consume_token(UNSIGNED);
    if (t.image.length() > 5 || Integer.parseInt(t.image) > 32767) {
      {if (true) throw genEx("Expecting integer (-32768 to 32767)");}}
    cg.emitInstruction("pwc", t.image);
      break;
    case PLUS:
      jj_consume_token(PLUS);
      factor();
      break;
    case MINUS:
      jj_consume_token(MINUS);
      negfactor();
      break;
    case ID:
      t = jj_consume_token(ID);
    st.enter(t.image);
    cg.emitInstruction("p", t.image);
      break;
    case LEFTPAREN:
      jj_consume_token(LEFTPAREN);
      expr();
      jj_consume_token(RIGHTPAREN);
      break;
    default:
      jj_la1[8] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

  final public void negfactor() throws ParseException {
                    Token t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case UNSIGNED:
      t = jj_consume_token(UNSIGNED);
    cg.emitInstruction("pwc", "-" + t.image);
      break;
    case ID:
      t = jj_consume_token(ID);
         st.enter(t.image);
         cg.emitInstruction("p", t.image);
         cg.emitInstruction("neg");
      break;
    case LEFTPAREN:
      jj_consume_token(LEFTPAREN);
      expr();
      jj_consume_token(RIGHTPAREN);
         cg.emitInstruction("neg");
      break;
    case PLUS:
      label_1:
      while (true) {
        jj_consume_token(PLUS);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case PLUS:
          ;
          break;
        default:
          jj_la1[9] = jj_gen;
          break label_1;
        }
      }
         t = getToken(1);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case MINUS:
        jj_consume_token(MINUS);
        factor();
        break;
      case UNSIGNED:
      case ID:
      case LEFTPAREN:
      case PLUS:
        factor();
           cg.emitInstruction("neg");
        break;
      default:
        jj_la1[10] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    case MINUS:
      jj_consume_token(MINUS);
      factor();
      break;
    default:
      jj_la1[11] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

//------------------------------
  final public void readintStatement() throws ParseException {
                          Token t;
    jj_consume_token(READINT);
    jj_consume_token(LEFTPAREN);
    t = jj_consume_token(ID);
         cg.emitInstruction("pc", t.image);
         cg.emitInstruction("din");
         cg.emitInstruction("stav");
    jj_consume_token(RIGHTPAREN);
    jj_consume_token(SEMICOLON);
  }

  private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_3_1() {
    if (jj_scan_token(ID)) return true;
    if (jj_scan_token(ASSIGN)) return true;
    return false;
  }

  /** Generated Token Manager. */
  public S4jTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[12];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static {
      jj_la1_init_0();
      jj_la1_init_1();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x102bd80,0x102bd80,0x200,0x34c000,0x34c000,0x34c000,0x300000,0xc00000,0x34c000,0x100000,0x34c000,0x34c000,};
   }
   private static void jj_la1_init_1() {
      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x2,0x2,0x0,0x0,0x0,0x0,0x0,0x0,};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[1];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public S4j(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public S4j(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new S4jTokenManager(this, jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 12; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 12; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public S4j(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new S4jTokenManager(this, jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 12; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 12; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public S4j(S4jTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 12; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(S4jTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 12; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[35];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 12; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
          if ((jj_la1_1[i] & (1<<j)) != 0) {
            la1tokens[32+j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 35; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 1; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}                                          // end of S4j
//======================================================
class S4jSymTab
{
  private ArrayList<String> symbol;
  //-----------------------------------------
  public S4jSymTab()
  {
    symbol = new ArrayList<String>();
  }
  //-----------------------------------------
  public void enter(String s)
  {
    int index = symbol.indexOf(s);

    // if s is not in symbol, then add it
    if (index < 0)
      symbol.add(s);
  }
  //-----------------------------------------
  public String getSymbol(int i)
  {
    return symbol.get(i);
  }
  //-----------------------------------------
  public int getSize()
  {
    return symbol.size();
  }
}                                    // end of S4jSymTab
//======================================================
class S4jCodeGen
{
  private PrintWriter outFile;
  private S4jSymTab st;
  private int labelNumber = 0;
  //-----------------------------------------
  public S4jCodeGen(PrintWriter outFile, S4jSymTab st)
  {
    this.outFile = outFile;
    this.st = st;
  }
  //-----------------------------------------
  public void emitInstruction(String op)
  {
    outFile.printf("          %-4s%n", op);
  }
  //-----------------------------------------
  public void emitInstruction(String op, String opnd)
  {
    outFile.printf(
                  "          %-4s      %s%n", op, opnd);
  }
  //-----------------------------------------
  public void emitdw(String label, String value)
  {
    outFile.printf(
             "%-9s dw        %s%n", label + ":", value);
  }
  //-----------------------------------------
  public void emitLabel(String label)
  {
    outFile.printf("%s%n", label + ":");
  }
  //-----------------------------------------
  public void endCode()
  {
    outFile.println();
    emitInstruction("halt");

    int size = st.getSize();
    // emit dw for each symbol in the symbol table
    for (int i=0; i < size; i++)
      emitdw(st.getSymbol(i), "0");
  }
  //-----------------------------------------
  public String getLabel(){
          return "@L" + labelNumber++;
  }
   //-----------------------------------------
  public void emitString(String line)
  {
    outFile.printf("%s%n", line);
  }
}
