/*
  Names:
    Jackson Maddox
    Liangyu Tan
    Johirul Islam
*/

// Hand-written S7 compiler
import java.io.*;
import java.util.*;
//======================================================
class S7
{
  public static void main(String[] args) throws
                                             IOException
  {
    System.out.println("S7 compiler written by ...");
    System.out.println("Jackson Maddox");
    System.out.println("Liangyu Tan");
    System.out.println("Johirul Islam");

    if (args.length != 1 && args.length != 2)
    {
      System.err.println("Wrong number cmd line args");
      System.exit(1);
    }

    // set to true to debug token manager
    boolean debug = false;

    String fileArg = null;
    if (args[0].equalsIgnoreCase("-debug_token_manager")) {
      debug = true;
      if (args.length != 2) {
        System.err.println("Missing file cmd line arg");
        System.exit(1);
      } else {
        fileArg = args[1];
      }
    } else {
      fileArg = args[0];
    }

    // build the input and output file names
    String inFileName = fileArg + ".s";
    String outFileName = fileArg + ".a";

    // construct file objects
    Scanner inFile = new Scanner(new File(inFileName));
    PrintWriter outFile = new PrintWriter(outFileName);

    // identify compiler/author in the output file
    outFile.println("; from S7 compiler written by ...");
    outFile.println("; Jackson Maddox");
    outFile.println("; Liangyu Tan");
    outFile.println("; Johirul Islam");

    // construct objects that make up compiler
    S7SymTab st = new S7SymTab();
    S7TokenMgr tm =  new S7TokenMgr(inFile, outFile, debug);
    S7CodeGen cg = new S7CodeGen(outFile, st);
    S7Parser parser = new S7Parser(st, tm, cg);

    // parse and translate
    try
    {
      parser.parse();
    }
    catch (RuntimeException e)
    {
      e.printStackTrace();
      System.err.println(e.getMessage());
      outFile.println(e.getMessage());
      outFile.close();
      System.exit(1);
    }

    outFile.close();
  }
}                                           // end of S7
//======================================================
interface S7Constants
{
  // categories for symbols
  int LOCALVARIABLE = 1;
  int GLOBALVARIABLE = 2;
  int EXTERNVARIABLE = 3;
  int FUNCTIONDEFINITION = 4;
  int FUNCTIONCALL = 5;

  // integers that identify token kinds
  int EOF = 0;
  int PRINTLN = 1;
  int UNSIGNED = 2;
  int ID = 3;
  int ASSIGN = 4;
  int SEMICOLON = 5;
  int LEFTPAREN = 6;
  int RIGHTPAREN = 7;
  int PLUS = 8;
  int MINUS = 9;
  int TIMES = 10;
  int ERROR = 11;
  int DIVIDE = 12;
  int LEFTBRACE = 13;
  int RIGHTBRACE = 14;
  int PRINT = 15;
  int STRING = 16;
  int READINT = 17;
  int WHILE = 18;
  int IF = 19;
  int DO = 20;
  int ELSE = 21;
  int EXTERN = 22;
  int INT = 23;
  int VOID = 24;
  int COMMA = 25;
  int BREAK = 26;
  int RETURN = 27;
  int REMAINDER = 28;
  int LESSTHAN = 29;
  int LESSTHANEQ = 30;
  int GREATERTHAN = 31;
  int GREATERTHANEQ = 32;
  int EQUAL = 33;
  int NOTEQUAL = 34;
  int LEFTBRACKET = 35;
  int RIGHTBRACKET = 36;
  int FOR = 37;

  // tokenImage provides string for each token kind
  String[] tokenImage =
  {
    "<EOF>",
    "\"println\"",
    "<UNSIGNED>",
    "<ID>",
    "\"=\"",
    "\";\"",
    "\"(\"",
    "\")\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "<ERROR>",
    "\"/\"",
    "\"{\"",
    "\"}\"",
    "\"print\"",
    "<STRING>",
    "\"readint\"",
    "\"while\"",
    "\"if\"",
    "\"do\"",
    "\"else\"",
    "\"extern\"",
    "\"int\"",
    "\"void\"",
    "\",\"",
    "\"break\"",
    "\"return\"",
    "\"%\"",
    "\"<\"",
    "\"<=\"",
    "\">\"",
    "\">=\"",
    "\"==\"",
    "\"!=\"",
    "\"[\"",
    "\"]\"",
    "\"for\"",
  };
}                                  // end of S7Constants
//======================================================
class S7SymTab implements S7Constants
{
  private ArrayList<String> symbol;
  private ArrayList<Integer> relAdd;
  private ArrayList<Integer> category;
  //-----------------------------------------
  public S7SymTab()
  {
    symbol = new ArrayList<>();
    relAdd = new ArrayList<>();
    category = new ArrayList<>();
  }
  //-----------------------------------------
  public void enter(String s, int ra, int cat)
  {
    int index = symbol.indexOf(s);

    // if s is not in symbol, then add it
    if (index < 0) {
      symbol.add(s);
      relAdd.add(ra);
      category.add(cat);
    } else {
      int currCat = category.get(index);
      if (cat == FUNCTIONCALL
        && (currCat == FUNCTIONCALL || currCat == FUNCTIONDEFINITION)) {

        ; // do nothing

      } else if (cat == FUNCTIONDEFINITION && currCat == FUNCTIONCALL) {

        category.set(index, FUNCTIONDEFINITION);

      } else if (cat == LOCALVARIABLE
        && (currCat == GLOBALVARIABLE || currCat == EXTERNVARIABLE)) {

        symbol.add(s);
        relAdd.add(ra);
        category.add(cat);

      } else {
        throw new RuntimeException("For symbol " + s +
          " got cat " + cat + " while having cat " + currCat);
      }
    }
  }
  //-----------------------------------------
  public int find(String sym)
  {
    for (int i = symbol.size() - 1; i >= 0; i--) {
      if (symbol.get(i).equals(sym)) return i;
    }
    throw new RuntimeException("No such symbol " + sym);
  }
  //-----------------------------------------
  public String getSymbol(int index)
  {
    return symbol.get(index);
  }
  //-----------------------------------------
  public Integer getRelAdd(int index)
  {
    return relAdd.get(index);
  }
  //-----------------------------------------
  public Integer getCategory(int index)
  {
    return category.get(index);
  }
  //-----------------------------------------
  public int getSize()
  {
    return symbol.size();
  }
  //-----------------------------------------
  public void localRemove()
  {
    // Inefficient, but should work
    boolean foundLocal = true;
    while (foundLocal) {
      foundLocal = false;

      for (int i = 0; i < symbol.size(); i++) {
        if (category.get(i) != LOCALVARIABLE) continue;
        symbol.remove(i);
        relAdd.remove(i);
        category.remove(i);
        foundLocal = true;
      }
    }
  }
}                                     // end of S7SymTab
//======================================================
class S7TokenMgr implements S7Constants
{
  private Scanner inFile;
  private PrintWriter outFile;
  private boolean debug;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;    // holds 1 line of input
  private Token token;         // holds 1 token
  private StringBuffer buffer; // token image built here
  private boolean keepComment; // keep comment characters, for string tokens
  private boolean isMultilineComment; // used for tracking when in a /* */ block
  //-----------------------------------------
  public S7TokenMgr(Scanner inFile,
                    PrintWriter outFile, boolean debug)
  {
    this.inFile = inFile;
    this.outFile = outFile;
    this.debug = debug;
    currentChar = '\n';        //  '\n' triggers read
    currentLineNumber = 0;
    buffer = new StringBuffer();
    keepComment = false;
    isMultilineComment = false;
  }
  //-----------------------------------------
  public Token getNextToken()
  {
    // skip whitespace
    while (Character.isWhitespace(currentChar)
      || (currentChar != EOF && isMultilineComment))
      getNextChar();

    // construct token to be returned to parser
    token = new Token();
    token.next = null;

    // save start-of-token position
    token.beginLine = currentLineNumber;
    token.beginColumn = currentColumnNumber;

    // check for EOF
    if (currentChar == EOF)
    {
      token.image = "<EOF>";
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;
      token.kind = EOF;
    }

    else  // check for unsigned int
    if (Character.isDigit(currentChar))
    {
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar);
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();
      token.kind = UNSIGNED;
    }

    else  // check for identifier
    if (Character.isLetter(currentChar))
    {
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar);
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isLetterOrDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();

      // check if keyword
      if (token.image.equals("println"))
        token.kind = PRINTLN;
      else if (token.image.equals("print"))
        token.kind = PRINT;
      else if (token.image.equals("readint"))
        token.kind = READINT;
      else if (token.image.equals("while"))
        token.kind = WHILE;
      else if (token.image.equals("if"))
        token.kind = IF;
      else if (token.image.equals("do"))
        token.kind = DO;
      else if (token.image.equals("else"))
        token.kind = ELSE;
      else if (token.image.equals("extern"))
        token.kind = EXTERN;
      else if (token.image.equals("int"))
        token.kind = INT;
      else if (token.image.equals("void"))
        token.kind = VOID;
      else if (token.image.equals("break"))
        token.kind = BREAK;
      else if (token.image.equals("return"))
        token.kind = RETURN;
      else if (token.image.equals("for"))
        token.kind = FOR;
      else  // not a keyword so kind is ID
        token.kind = ID;
    }

    else // check for string
    if (currentChar == '\"') {
      int slashCount = 0;
      buffer.setLength(0); // clear buffer
      keepComment = true;
      do // build token image in buffer
      {
        if (currentChar == '\\')
          slashCount++;
        else
          slashCount = 0;

        if (currentChar == '\n' || currentChar == '\r') {
          while (currentChar == '\n' || currentChar == '\r')
            getNextChar();
          buffer.setLength(buffer.length() - 1); // remove the \ character
          continue;
        }

        buffer.append(currentChar);
        getNextChar();
      } while (currentChar != '\n'
            && currentChar != '\r'
            && currentChar != '\"'
            || slashCount % 2 != 0);

      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;
      keepComment = false;

      if (currentChar == '\"') { // closing quote
        buffer.append(currentChar);
        token.kind = STRING;

        getNextChar(); // read beyond end of token
      } else { // got a \n or \r
        token.kind = ERROR;
      }

      token.image = buffer.toString();
    }

    else  // process single-character token
    {
      boolean readBeyondEnd = true;
      int savedLineNumber = currentLineNumber;
      int savedColumnNumber = currentColumnNumber;

      buffer.setLength(0);
      buffer.append(currentChar);

      switch(currentChar)
      {
        case '=':
          token.kind = ASSIGN;
          getNextChar();
          if (currentChar == '=') {
            buffer.append(currentChar);
            token.kind = EQUAL;
          } else {
            readBeyondEnd = false;
            // save end-of-token position
            token.endLine = savedLineNumber;
            token.endColumn = savedColumnNumber;
          }
          break;
        case ';':
          token.kind = SEMICOLON;
          break;
        case '(':
          token.kind = LEFTPAREN;
          break;
        case ')':
          token.kind = RIGHTPAREN;
          break;
        case '+':
          token.kind = PLUS;
          break;
        case '-':
          token.kind = MINUS;
          break;
        case '*':
          token.kind = TIMES;
          break;
        case '/':
          token.kind = DIVIDE;
          break;
        case '{':
          token.kind = LEFTBRACE;
          break;
        case '}':
          token.kind = RIGHTBRACE;
          break;
        case '[':
          token.kind = LEFTBRACKET;
          break;
        case ']':
          token.kind = RIGHTBRACKET;
          break;
        case ',':
          token.kind = COMMA;
          break;
        case '%':
          token.kind = REMAINDER;
          break;
        case '<':
          token.kind = LESSTHAN;
          getNextChar();
          if (currentChar == '=') {
            buffer.append(currentChar);
            token.kind = LESSTHANEQ;
          } else {
            readBeyondEnd = false;
            // save end-of-token position
            token.endLine = savedLineNumber;
            token.endColumn = savedColumnNumber;
          }
          break;
        case '>':
          token.kind = GREATERTHAN;
          getNextChar();
          if (currentChar == '=') {
            buffer.append(currentChar);
            token.kind = GREATERTHANEQ;
          } else {
            readBeyondEnd = false;
            // save end-of-token position
            token.endLine = savedLineNumber;
            token.endColumn = savedColumnNumber;
          }
          break;
        case '!':
          getNextChar();
          if (currentChar == '=') {
            buffer.append(currentChar);
            token.kind = NOTEQUAL;
          } else { token.kind = ERROR; }
          break;
        default:
          token.kind = ERROR;
          break;
      }

      // save the operator as String in token.image
      token.image = buffer.toString();

      if (readBeyondEnd) {
        // save end-of-token position
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;

        getNextChar();  // read beyond end of token
      }
    }

    // token trace appears as comments in output file
    if (debug)
      outFile.printf(
        "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n",
        token.kind, token.beginLine, token.beginColumn,
        token.endLine, token.endColumn, token.image);

    return token;     // return token to parser
  }
  //-----------------------------------------
  private void getNextChar()
  {
    if (currentChar == EOF)
      return;

    if (currentChar == '\n')        // need next line?
    {
      if (inFile.hasNextLine())     // any lines left?
      {
        inputLine = inFile.nextLine();  // get next line
        // output source line as comment
        outFile.println("; " + inputLine);
        inputLine = inputLine + "\n";   // mark line end
        currentColumnNumber = 0;
        currentLineNumber++;
      }
      else  // at end of file
      {
         currentChar = EOF;
         return;
      }
    }

    // get next char from inputLine
    currentChar = inputLine.charAt(currentColumnNumber++);

    // in S7, test for single-line comment goes here
    if (currentChar == '/'
    && inputLine.charAt(currentColumnNumber) == '/'
    && !keepComment) {
      currentChar = '\n';
    }

    if (currentChar == '/'
    && inputLine.charAt(currentColumnNumber) == '*'
    && !keepComment) {
      // consume characters until end of comment
      isMultilineComment = true;
    } else if (currentChar == '*'
      && inputLine.charAt(currentColumnNumber) == '/') {
      // no longer a comment
      isMultilineComment = false;

      // consume * and / characters
      currentChar = inputLine.charAt(currentColumnNumber + 1);
      currentColumnNumber += 2;
    }
  }
}                                   // end of S7TokenMgr
//======================================================
class S7Parser implements S7Constants
{
  private S7SymTab st;
  private S7TokenMgr tm;
  private S7CodeGen cg;
  private Token currentToken;
  private Token previousToken;
  private ArrayList<String> breakLabel;
  //-----------------------------------------
  public S7Parser(S7SymTab st, S7TokenMgr tm,
                                           S7CodeGen cg)
  {
    this.st = st;
    this.tm = tm;
    this.cg = cg;
    // prime currentToken with first token
    currentToken = tm.getNextToken();
    previousToken = null;
    breakLabel = new ArrayList<>();
  }
  //-----------------------------------------
  // Construct and return an exception that contains
  // a message consisting of the image of the current
  // token, its location, and the expected tokens.
  //
  private RuntimeException genEx(String errorMessage)
  {
    return new RuntimeException("Encountered \"" +
      currentToken.image + "\" on line " +
      currentToken.beginLine + ", column " +
      currentToken.beginColumn + "." +
      System.getProperty("line.separator") +
      errorMessage);
  }
  //-----------------------------------------
  // Advance currentToken to next token.
  //
  private void advance()
  {
    previousToken = currentToken;

    // If next token is on token list, advance to it.
    if (currentToken.next != null)
      currentToken = currentToken.next;

    // Otherwise, get next token from token mgr and
    // put it on the list.
    else
      currentToken =
                  currentToken.next = tm.getNextToken();
  }
  //-----------------------------------------
  // getToken(i) returns ith token without advancing
  // in token stream.  getToken(0) returns
  // previousToken.  getToken(1) returns currentToken.
  // getToken(2) returns next token, and so on.
  //
  private Token getToken(int i)
  {
    if (i <= 0)
      return previousToken;

    Token t = currentToken;
    for (int j = 1; j < i; j++)  // loop to ith token
    {
      // if next token is on token list, move t to it
      if (t.next != null)
        t = t.next;

      // Otherwise, get next token from token mgr and
      // put it on the list.
      else
        t = t.next = tm.getNextToken();
    }
    return t;
  }
  //-----------------------------------------
  // If the kind of the current token matches the
  // expected kind, then consume advances to the next
  // token. Otherwise, it throws an exception.
  //
  private void consume(int expected)
  {
    if (currentToken.kind == expected)
      advance();
    else
      throw genEx("Expecting " + tokenImage[expected]);
  }
  //-----------------------------------------
  public void parse()
  {
    program();   // program is start symbol for grammar
  }
  //-----------------------------------------
  private void program()
  {
    programUnitList();
    cg.endCode();
    if (currentToken.kind != EOF)  //garbage at end?
      throw genEx("Expecting <EOF>");
  }
  //-----------------------------------------
  private void programUnitList()
  {
    if (currentToken.kind == EOF) return;

    programUnit();
    programUnitList();
  }
  //-----------------------------------------
  private void programUnit()
  {
    switch (currentToken.kind) {
      case EXTERN:
        externDeclaration();
        break;
      case INT:
        if (getToken(2).kind == ID) {
          if (getToken(3).kind == LEFTPAREN) functionDefinition();
          else globalDeclaration();
        } else {
          throw genEx("Expecting global or function declaration");
        }
        break;
      case VOID:
        functionDefinition();
        break;
      default:
        throw genEx("Expecting extern, global, or function declaration");
    }
  }
  //-----------------------------------------
  private void externDeclaration()
  {
    Token t;

    consume(EXTERN);
    consume(INT);
    t = currentToken;
    consume(ID);
    st.enter(t.image, 0, EXTERNVARIABLE);
    cg.emitInstruction("extern", t.image);

    while (currentToken.kind == COMMA) {
      consume(COMMA);
      t = currentToken;
      consume(ID);
      st.enter(t.image, 0, EXTERNVARIABLE);
      cg.emitInstruction("extern", t.image);
    }
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void globalDeclaration()
  {
    consume(INT);
    global();

    while (currentToken.kind == COMMA) {
      consume(COMMA);
      global();
    }
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void global()
  {
    Token t1, t2;
    String initVal;
    int arrayLength, i;

    t1 = currentToken;
    consume(ID);
    cg.emitInstruction("public", t1.image);
    initVal = "0";

    if (currentToken.kind == ASSIGN) {
      consume(ASSIGN);
      initVal = "";

      if (currentToken.kind == PLUS) {
        consume(PLUS);
      } else if (currentToken.kind == MINUS) {
        consume(MINUS);
        initVal = "-";
      }

      t2 = currentToken;
      consume(UNSIGNED);
      initVal = initVal + t2.image;

      int parsed = Integer.parseInt(initVal);
      if (t2.image.length() > 5 || parsed < -32768 || parsed > 32767) {
        throw genEx("Expecting integer (-32768 to 32767)");
      }
    } else if (currentToken.kind == LEFTBRACKET) {
      // global arrays are defined crudely as a string in the asm
      consume(LEFTBRACKET);
      t2 = currentToken;
      consume(UNSIGNED);
      initVal = "\"";
      arrayLength = Integer.parseInt(t2.image);

      if (arrayLength >= 90)
        throw genEx("Global arrays cannot exceed a length of 89");

      initVal += (((char)arrayLength)=='\"' ? "\\" : "") + (char) arrayLength;
      for (i = 0; i < arrayLength - 1; i++) { // -1 b/c string null terminates
        initVal += "\\0"; // use "null" character as initializer
      }
      initVal += "\"";
      consume(RIGHTBRACKET);
    }

    st.enter(t1.image, 0, GLOBALVARIABLE);
    cg.emitdw(t1.image, initVal);
  }
  //-----------------------------------------
  private void functionDefinition()
  {
    Token t;
    int reservedLoc = 2; // if 0 params, reserved slot will be at "cora 2"
    boolean requireReturn = false;

    switch (currentToken.kind) {
      case VOID:
        consume(VOID);
        break;
      case INT:
        consume(INT);
        requireReturn = true;
        break;
      default:
        throw genEx("Expecting return type");
    }

    t = currentToken;
    consume(ID);
    cg.emitString("; =============== start of function " + t.image);
    st.enter(t.image, 0, FUNCTIONDEFINITION);
    cg.emitInstruction("public", t.image);
    cg.emitLabel(t.image);
    consume(LEFTPAREN);
    if (currentToken.kind == INT) reservedLoc = parameterList() + 1;
    consume(RIGHTPAREN);
    consume(LEFTBRACE);
    cg.emitInstruction("esba");
    localDeclarations();
    statementList();

    if (requireReturn) {
      consume(RETURN);
      cg.emitInstruction("cora", Integer.toString(reservedLoc));
      expr();
      cg.emitInstruction("stav");
      consume(SEMICOLON);
    }

    consume(RIGHTBRACE);
    cg.emitInstruction("reba");
    cg.emitInstruction("ret");
    cg.emitString("; =============== end of function " + t.image);
    st.localRemove();
  }
  //-----------------------------------------
  private int parameterList()
  {
    Token t;
    int p;

    t = parameter();
    p = parameterR();
    st.enter(t.image, p, LOCALVARIABLE);

    return p;
  }
  //-----------------------------------------
  private Token parameter()
  {
    Token t;

    consume(INT);
    t = currentToken;
    consume(ID);
    return t;
  }
  //-----------------------------------------
  private int parameterR()
  {
    Token t;
    int p;

    if (currentToken.kind == COMMA) {
      consume(COMMA);
      t = parameter();
      p = parameterR(); // p is the rel address
      st.enter(t.image, p, LOCALVARIABLE);
      return p + 1; // return next relative address
    } else {
      return 2; // end of parameter list
    }
  }
  //-----------------------------------------
  private void localDeclarations()
  {
    int relativeAddress = -1;
    int varSize = 0;

    while (currentToken.kind == INT) {
      consume(INT);
      varSize = local(relativeAddress); // process 1 local var
      relativeAddress -= varSize;
      while (currentToken.kind == COMMA) {
        consume(COMMA);
        varSize = local(relativeAddress); // process 1 local var
        relativeAddress -= varSize;
      }
      consume(SEMICOLON);
    }
  }
  //-----------------------------------------
  private int local(int relativeAddress)
  {
    Token t, t2;
    String sign, constLabel;
    int arrLength;

    t = currentToken;
    consume(ID);
    st.enter(t.image, relativeAddress, LOCALVARIABLE);

    if (currentToken.kind == ASSIGN) { // value definition
      consume(ASSIGN);
      sign = "";
      if (currentToken.kind == PLUS) {
        consume(PLUS);
      } else if (currentToken.kind == MINUS) {
        consume(MINUS);
        sign = "-";
      }
      t = currentToken;
      consume(UNSIGNED);
      constLabel = cg.getNumberLabel(sign + t.image);
      cg.emitInstruction("p", constLabel);
      return 1;
    } else if (currentToken.kind == LEFTBRACKET) { // array definition
      consume(LEFTBRACKET);
      t2 = currentToken;
      consume(UNSIGNED);
      arrLength = Integer.parseInt(t2.image) + 1; // len + space for length info
      consume(RIGHTBRACKET);
      cg.emitInstruction("asp", "-" + arrLength); // make space for the array
      // now push length to 0 location
      cg.pushAddress(st.find(t.image));
      constLabel = cg.getNumberLabel(t2.image);
      cg.emitInstruction("p", constLabel);
      cg.emitInstruction("stav");
      return arrLength;
    } else { // not an array and no value
      cg.emitInstruction("asp", "-1");
      return 1;
    }
  }
  //-----------------------------------------
  private void statementList()
  {
    if (currentToken.kind == RIGHTBRACE
      || currentToken.kind == RETURN) return;

    statement();
    statementList();
  }
  //-----------------------------------------
  private void statement()
  {
    try {
      switch(currentToken.kind)
      {
        case ID:
          if (getToken(2).kind == ASSIGN || getToken(2).kind == LEFTBRACKET)
            assignmentStatement();
          else if (getToken(2).kind == LEFTPAREN) {
            functionCall();
            consume(SEMICOLON);
          }
          else throw genEx("Expecting assignment or function call");
          break;
        case PRINTLN:
          printlnStatement();
          break;
        case PRINT:
          printStatement();
          break;
        case READINT:
          readintStatement();
          break;
        case WHILE:
          whileStatement();
          break;
        case IF:
          ifStatement();
          break;
        case DO:
          doWhileStatement();
          break;
        case LEFTBRACE:
          compoundStatement();
          break;
        case SEMICOLON:
          nullStatement();
          break;
        case BREAK:
          breakStatement();
          break;
        case FOR:
          forStatement();
          break;
        default:
          throw genEx("Expecting statement");
      }
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
      cg.emitString(e.getMessage());

      // advance to next semicolon
      while (currentToken.kind != SEMICOLON && currentToken.kind != EOF)
        advance();
      if (currentToken.kind != EOF)
        advance();
    }
  }
  //-----------------------------------------
  private void compoundStatement()
  {
    switch(currentToken.kind)
    {
      case LEFTBRACE:
        consume(LEFTBRACE);
        compoundList();
        consume(RIGHTBRACE);
        break;
      default:
        throw genEx("Expected left brace");
    }
  }
  //-----------------------------------------
  private void compoundList()
  {
    if (currentToken.kind == RIGHTBRACE)
      return;

    statement();
    compoundList();
  }
  //-----------------------------------------
  private void nullStatement()
  {
    switch (currentToken.kind)
    {
      case SEMICOLON:
        consume(SEMICOLON);
        break;
      default:
        throw genEx("Expecting semicolon");
    }
  }
  //-----------------------------------------
  private void assignmentStatement()
  {
    Token t;
    int index;

    t = currentToken;
    consume(ID);
    index = st.find(t.image);
    cg.pushAddress(index);

    // handle array assigns // TODO
    if (currentToken.kind == LEFTBRACKET) {
      arrayIndex(index);
    }

    consume(ASSIGN);
    assignmentTail();
    cg.emitInstruction("stav");
  }
  //-----------------------------------------
  private void arrayIndex(int varIndex)
  {
    consume(LEFTBRACKET);
    expr();
    if (st.getCategory(varIndex) == LOCALVARIABLE) {
      cg.emitInstruction("sub"); // stack grows down
    } else {
      cg.emitInstruction("add"); // dws grow up
    }
    consume(RIGHTBRACKET);
  }
  //-----------------------------------------
  private void assignmentTail()
  {
    Token t;
    int index;

    t = currentToken;
    if (currentToken.kind == ID && getToken(2).kind == ASSIGN)
    {
      consume(ID);
      index = st.find(t.image);
      cg.pushAddress(index);
      consume(ASSIGN);
      assignmentTail();
      cg.emitInstruction("dupe");
      cg.emitInstruction("rot");
      cg.emitInstruction("stav");
    }
    else
    {
      expr();
      consume(SEMICOLON);
    }

  }
  //-----------------------------------------
  private void printlnStatement()
  {
    consume(PRINTLN);
    consume(LEFTPAREN);

    switch (currentToken.kind) {
      case RIGHTPAREN:
        break;
      default:
        printArg();
    }

    cg.emitInstruction("pc", "'\\n'");
    cg.emitInstruction("aout");
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printStatement()
  {
    consume(PRINT);
    consume(LEFTPAREN);
    printArg();
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printArg()
  {
    Token t;

    switch (currentToken.kind) {
      case STRING: // print out string
        t = currentToken;
        String name = t.image;
        consume(STRING);
        String label = cg.getLabel();
        int i = 0;
        t = currentToken;
        int index =0;
        boolean id = false;
        while(t.kind != RIGHTPAREN)
        {
          consume(COMMA);
          t = currentToken;
          String val = "{"+i+"}";
          i++;
          if(t.kind == UNSIGNED)
          {
             name = name.replace(val,t.image);
             consume(UNSIGNED);
          }
          else
          {
            int curindex = name.indexOf(val);
            String sub = name.substring(index,curindex);
            if(!sub.equals("\""))
            {
            cg.emitInstruction("pc", "@LL"+curindex);
            cg.emitInstruction("sout");
            cg.emitdw("^@LL"+curindex,"\""+ sub+ "\"");
            }

            cg.emitInstruction("p", t.image);
            cg.emitInstruction("dout");
            index = curindex+3;
            consume(ID);
            id = true;
          }

          t = currentToken;
        }
        name = name.substring(index, name.length());
        if(!name.startsWith("\""))
        {
          name = "\""+name;
        }
        if(name.indexOf('{')!=-1)
        {
          throw new IllegalArgumentException("Error: Parameters not found");
        }
        cg.emitInstruction("pc", label);
        cg.emitInstruction("sout");
        cg.emitdw("^" + label, name);

        break;
      default: // assume it's an expression
        expr();
        cg.emitInstruction("dout");
    }
  }
  //-----------------------------------------
  private void readintStatement()
  {
    Token t;
    int index;

    consume(READINT);
    consume(LEFTPAREN);
    t = currentToken;
    consume(ID);
    index = st.find(t.image);
    cg.pushAddress(index);
    cg.emitInstruction("din");
    cg.emitInstruction("stav");
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void whileStatement()
  {
    String label1, label2;

    consume(WHILE);

    label1 = cg.getLabel();
    cg.emitLabel(label1);

    consume(LEFTPAREN);
    expr();
    consume(RIGHTPAREN);

    label2 = cg.getLabel();
    cg.emitInstruction("jz", label2);
    breakLabel.add(label2); // add this loop's break label to the end

    statement();

    breakLabel.remove(breakLabel.size() - 1);
    cg.emitInstruction("ja", label1);
    cg.emitLabel(label2);
  }
  //-----------------------------------------
  private void forStatement()
  {
    String label1, label2;
    Token t;
    int index;

    consume(FOR);

    label1 = cg.getLabel();
    cg.emitLabel(label1);

    consume(LEFTPAREN);
//evaluate the 1st expression
    localDeclarations();
	//globalDeclaration();
//evaluate the 2nd expression
    expr();
    consume(SEMICOLON);

//evaluate the 3rd expression
    t = currentToken;
    consume(ID);
    index = st.find(t.image);
    cg.pushAddress(index);
    consume(ASSIGN);
    expr();
    cg.emitInstruction("stav");

    consume(RIGHTPAREN);

    label2 = cg.getLabel();
    cg.emitInstruction("jz", label2);
    breakLabel.add(label2); // add this loop's break label to the end

    statement();

    breakLabel.remove(breakLabel.size() - 1);
    cg.emitInstruction("ja", label1);
    cg.emitLabel(label2);
  }
  //-----------------------------------------
  private void ifStatement()
  {
    String label1;

    consume(IF);
    consume(LEFTPAREN);
    expr();
    consume(RIGHTPAREN);

    label1 = cg.getLabel();
    cg.emitInstruction("jz", label1);

    statement();
    elsePart(label1);
  }
  //-----------------------------------------
  private void elsePart(String label1)
  {
    String label2;

    switch (currentToken.kind) {
      case ELSE:
        consume(ELSE);
        label2 = cg.getLabel();
        cg.emitInstruction("ja", label2);
        cg.emitLabel(label1);
        statement();
        cg.emitLabel(label2);
        break;
      default: // if there's no else part
        cg.emitLabel(label1);
    }
  }
  //-----------------------------------------
  private void doWhileStatement()
  {
    String label1, label2;

    consume(DO);

    label1 = cg.getLabel();
    cg.emitLabel(label1);

    label2 = cg.getLabel();
    breakLabel.add(label2);

    statement();

    consume(WHILE);
    consume(LEFTPAREN);
    expr();
    consume(RIGHTPAREN);
    consume(SEMICOLON);

    cg.emitInstruction("jnz", label1);

    breakLabel.remove(breakLabel.size() - 1);
    cg.emitLabel(label2);
  }
  //-----------------------------------------
  private void functionCall()
  {
    Token t;
    int count;

    t = currentToken;
    consume(ID);
    st.enter(t.image, 0, FUNCTIONCALL);
    consume(LEFTPAREN);
    count = 0;

    switch (currentToken.kind) {
      case UNSIGNED:
      case PLUS:
      case MINUS:
      case ID:
      case LEFTPAREN:
        count = argumentList();
        break;
      default:
        ; // do nothing
    }

    cg.emitInstruction("call", t.image);
    if (count > 0) cg.emitInstruction("asp", Integer.toString(count));
    consume(RIGHTPAREN);
  }
  //-----------------------------------------
  private int argumentList()
  {
    int count;

    expr();
    count = 1;
    while (currentToken.kind == COMMA) {
      consume(COMMA);
      expr();
      count++;
    }
    return count;
  }
  //-----------------------------------------
  private void breakStatement()
  {
    int index;

    consume(BREAK);
    index = breakLabel.size() - 1;
    if (index < 0) throw genEx("Must be in a loop to break");
    cg.emitInstruction("ja", breakLabel.get(index));
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void expr()
  {
    precedenceL4();
    precedenceL4List();
  }
  //-----------------------------------------
  private void precedenceL4List()
  {
    // ops: == !=
    switch (currentToken.kind)
    {
      case EQUAL:
        consume(EQUAL);
        precedenceL4();
        cg.emitInstruction("cmps", "2");
        precedenceL4List();
        break;
      case NOTEQUAL: // TODO NOT SURE
        consume(NOTEQUAL);
        precedenceL4();
        cg.emitInstruction("cmps", "2");
        cg.emitInstruction("pc", "0");
        cg.emitInstruction("cmps", "2");
        precedenceL4List();
        break;
      case RIGHTPAREN:
      case RIGHTBRACKET:
      case SEMICOLON:
      case COMMA:
        ;
        break;
      default:
        throw genEx("Expecting op, \")\", \";\", or \",\"");
    }
  }
  //-----------------------------------------
  private void precedenceL4()
  {
    precedenceL3();
    precedenceL3List();
  }
  //-----------------------------------------
  private void precedenceL3List()
  {
    // ops: < <= > >=
    switch (currentToken.kind)
    {
      case LESSTHAN:
        consume(LESSTHAN);
        precedenceL3();
        cg.emitInstruction("cmps", "4");
        precedenceL3List();
        break;
      case LESSTHANEQ:
        consume(LESSTHANEQ);
        precedenceL3();
        cg.emitInstruction("cmps", "6");
        precedenceL3List();
        break;
      case GREATERTHAN:
        consume(GREATERTHAN);
        precedenceL3();
        cg.emitInstruction("cmps", "1");
        precedenceL3List();
        break;
      case GREATERTHANEQ:
        consume(GREATERTHANEQ);
        precedenceL3();
        cg.emitInstruction("cmps", "3");
        precedenceL3List();
        break;
      case EQUAL:
      case NOTEQUAL:
      case RIGHTPAREN:
      case RIGHTBRACKET:
      case SEMICOLON:
      case COMMA:
        ;
        break;
      default:
        throw genEx("Expecting op, \")\", \";\", or \",\"");
    }
  }
  //-----------------------------------------
  private void precedenceL3()
  {
    term();
    termList();
  }
  //-----------------------------------------
  private void termList()
  {
    switch(currentToken.kind)
    {
      case PLUS:
        consume(PLUS);
        term();
        cg.emitInstruction("add");
        termList();
        break;
      case MINUS:
        consume(MINUS);
        term();
        cg.emitInstruction("sub");
        termList();
        break;
      case EQUAL:
      case NOTEQUAL:
      case LESSTHAN:
      case LESSTHANEQ:
      case GREATERTHAN:
      case GREATERTHANEQ:
      case RIGHTPAREN:
      case RIGHTBRACKET:
      case SEMICOLON:
      case COMMA:
        ;
        break;
      default:
        throw genEx("Expecting op, \")\", \";\", or \",\"");
    }
  }
  //-----------------------------------------
  private void term()
  {
    factor();
    factorList();
  }
  //-----------------------------------------
  private void factorList()
  {
    switch(currentToken.kind)
    {
      case TIMES:
        consume(TIMES);
        factor();
        cg.emitInstruction("mult");
        factorList();
        break;
      case DIVIDE:
        consume(DIVIDE);
        factor();
        cg.emitInstruction("div");
        factorList();
        break;
      case REMAINDER:
        consume(REMAINDER);
        factor();
        cg.emitInstruction("rem");
        factorList();
        break;
      case EQUAL:
      case NOTEQUAL:
      case LESSTHAN:
      case LESSTHANEQ:
      case GREATERTHAN:
      case GREATERTHANEQ:
      case PLUS:
      case MINUS:
      case RIGHTPAREN:
      case RIGHTBRACKET:
      case SEMICOLON:
      case COMMA:
        ;
        break;
      default:
        throw genEx("Expecting op, \")\", \";\", or \",\"");
    }
  }
  //-----------------------------------------
  private void factor()
  {
    Token t;
    int index;
    String constLabel;

    switch(currentToken.kind)
    {
      case UNSIGNED:
        t = currentToken;
        if (t.image.length() > 5 || Integer.parseInt(t.image) > 32767) {
          throw genEx("Expecting integer (-32768 to 32767)");
        }
        consume(UNSIGNED);
        constLabel = cg.getNumberLabel(t.image);
        cg.emitInstruction("p", constLabel);
        break;
      case PLUS:
        consume(PLUS);
        factor();
        break;
      case MINUS:
        consume(MINUS);
        negFactor();
        break;
      case ID: // TODO
        if (getToken(2).kind == LEFTPAREN) { // function call
          cg.emitInstruction("asp", "-1");
          functionCall();
        } else if (getToken(2).kind == LEFTBRACKET) { // array access
          t = currentToken;
          consume(ID);
          index = st.find(t.image);
          cg.pushAddress(index);
          arrayIndex(index);
          cg.emitInstruction("load"); // load the value at ID+expr()
        } else { // otherwise normal ID factor
          t = currentToken;
          consume(ID);
          index = st.find(t.image);
          cg.push(index);
        }
        break;
      case LEFTPAREN:
        consume(LEFTPAREN);
        expr();
        consume(RIGHTPAREN);
        break;
      default:
        throw genEx("Expecting factor");
    }
  }
  //-----------------------------------------
  private void negFactor()
  {
    Token t;
    int index;
    String constLabel;

    switch (currentToken.kind) {
      case UNSIGNED:
        t = currentToken;
        if (t.image.length() > 5 || Integer.parseInt(t.image) > 32768) {
          throw genEx("Expecting integer (-32768 to 32767)");
        }
        consume(UNSIGNED);
        constLabel = cg.getNumberLabel("-" + t.image);
        cg.emitInstruction("p", constLabel);
        break;
      case ID:
        if (getToken(2).kind == LEFTPAREN) { // function call
          cg.emitInstruction("asp", "-1");
          functionCall();
        } else if (getToken(2).kind == LEFTBRACKET) { // array access
          t = currentToken;
          consume(ID);
          index = st.find(t.image);
          cg.pushAddress(index);
          arrayIndex(index);
          cg.emitInstruction("load"); // load the value at ID+expr()
          cg.emitInstruction("neg");
          consume(RIGHTBRACKET); // TODO
        } else { // normal ID access
          t = currentToken;
          consume(ID);
          index = st.find(t.image);
          cg.push(index);
          cg.emitInstruction("neg");
        }

        break;
      case LEFTPAREN:
        consume(LEFTPAREN);
        expr();
        consume(RIGHTPAREN);
        cg.emitInstruction("neg");
        break;
      case PLUS:
        while (currentToken.kind == PLUS) consume(PLUS);

        if (currentToken.kind == MINUS) {
          consume(MINUS);
          factor();
        } else {
          factor();
          cg.emitInstruction("neg");
        }

        break;
      case MINUS:
        consume(MINUS);
        factor();
        break;
      default:
        throw genEx("Expecting factor");
    }
  }
}                                     // end of S7Parser
//======================================================
class S7CodeGen implements S7Constants
{
  private PrintWriter outFile;
  private S7SymTab st;
  private int labelNumber;
  private HashMap<String, Integer> numberSet;
  //-----------------------------------------
  public S7CodeGen(PrintWriter outFile, S7SymTab st)
  {
    this.outFile = outFile;
    this.st = st;
    this.labelNumber = 0;
    this.numberSet = new HashMap<>();
  }
  //-----------------------------------------
  public String getLabel()
  {
    return "@L" + labelNumber++;
  }
  //-----------------------------------------
  public String getNumberLabel(String numStr)
  {
    int number = Integer.parseInt(numStr);
    String label = "@";
    if (number < 0) {
      label += "_" + Integer.toString(number).substring(1);
    } else {
      label += number;
    }
    numberSet.put(label, number);
    return label;
  }
  //-----------------------------------------
  public void emitLabel(String label)
  {
    outFile.printf("%s%n", label + ":");
  }
  //-----------------------------------------
  public void emitInstruction(String op)
  {
    outFile.printf("          %-4s%n", op);
  }
  //-----------------------------------------
  public void emitInstruction(String op, String opnd)
  {
    outFile.printf("          %-4s      %s%n", op,opnd);
  }
  //-----------------------------------------
  public void emitdw(String label, String value)
  {
    outFile.printf(
             "%-9s dw        %s%n", label + ":", value);
  }
  //-----------------------------------------
  public void emitString(String line)
  {
    outFile.printf("%s%n", line);
  }
  //-----------------------------------------
  public void push(int p)
  {
    int cat = st.getCategory(p);
    if (cat == LOCALVARIABLE)
      emitInstruction("pr", Integer.toString(st.getRelAdd(p)));
    else
      emitInstruction("p", st.getSymbol(p));
  }
  //-----------------------------------------
  public void pushAddress(int p)
  {
    int cat = st.getCategory(p);
    if (cat == LOCALVARIABLE)
      emitInstruction("cora", Integer.toString(st.getRelAdd(p)));
    else
      emitInstruction("pc", st.getSymbol(p));
  }
  //-----------------------------------------
  public void endCode()
  {
    outFile.println();

    int size = st.getSize();
    // emit extern for each FUNCTIONCALL in the symbol table
    for (int i=0; i < size; i++)
      if (st.getCategory(i) == FUNCTIONCALL)
        emitInstruction("extern", st.getSymbol(i));

    // emit the constants
    for (String s : numberSet.keySet()) {
      Integer n = numberSet.get(s);
      emitdw(s, Integer.toString(n));
    }
  }
}                                    // end of S7CodeGen
