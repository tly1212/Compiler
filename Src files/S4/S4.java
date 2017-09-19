/*
  Names:
    Jackson Maddox
    Liangyu Tan
    Johirul Islam
*/

// Hand-written S4 compiler
import java.io.*;
import java.util.*;

//======================================================
class S4 {
	public static void main(String[] args) throws IOException {
		System.out.println("S4 compiler written by ...");
		System.out.println("Jackson Maddox");
		System.out.println("Liangyu Tan");
		System.out.println("Johirul Islam");

		if (args.length != 1 && args.length != 2) {
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
		outFile.println("; from S4 compiler written by ...");
		outFile.println("; Jackson Maddox");
		outFile.println("; Liangyu Tan");
		outFile.println("; Johirul Islam");

		// construct objects that make up compiler
		S4SymTab st = new S4SymTab();
		S4TokenMgr tm = new S4TokenMgr(inFile, outFile, debug);
		S4CodeGen cg = new S4CodeGen(outFile, st);
		S4Parser parser = new S4Parser(st, tm, cg);

		// parse and translate
		try {
			parser.parse();
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			outFile.println(e.getMessage());
			outFile.close();
			System.exit(1);
		}

		outFile.close();
	}
} // end of S4
	// ======================================================

interface S4Constants {
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
	int DO = 19;
	int IF = 20;
	int ELSE = 21;

	// tokenImage provides string for each token kind
	String[] tokenImage = { "<EOF>", "\"println\"", "<UNSIGNED>", "<ID>", "\"=\"", "\";\"", "\"(\"", "\")\"", "\"+\"",
			"\"-\"", "\"*\"", "<ERROR>", "\"/\"", "\"{\"", "\"}\"", "\"print\"", "<STRING>", "\"readint\"", "\"while\"",
			"\"do\"", "\"if\"", "\"else\"", };
} // end of S4Constants
	// ======================================================

class S4SymTab {
	private ArrayList<String> symbol;

	// -----------------------------------------
	public S4SymTab() {
		symbol = new ArrayList<String>();
	}

	// -----------------------------------------
	public void enter(String s) {
		int index = symbol.indexOf(s);

		// if s is not in symbol, then add it
		if (index < 0)
			symbol.add(s);
	}

	// -----------------------------------------
	public String getSymbol(int index) {
		return symbol.get(index);
	}

	// -----------------------------------------
	public int getSize() {
		return symbol.size();
	}
} // end of S4SymTab
	// ======================================================

class S4TokenMgr implements S4Constants {
	private Scanner inFile;
	private PrintWriter outFile;
	private boolean debug;
	private char currentChar;
	private int currentColumnNumber;
	private int currentLineNumber;
	private String inputLine; // holds 1 line of input
	private Token token; // holds 1 token
	private StringBuffer buffer; // token image built here
	private boolean keepComment; // keep comment characters, for string tokens
	private int slashNum = 0; // number of consecutive slash
	private char previousChar;

	// -----------------------------------------
	public S4TokenMgr(Scanner inFile, PrintWriter outFile, boolean debug) {
		this.inFile = inFile;
		this.outFile = outFile;
		this.debug = debug;
		currentChar = '\n'; // '\n' triggers read
		currentLineNumber = 0;
		buffer = new StringBuffer();
		keepComment = false;
	}

	// -----------------------------------------
	public Token getNextToken() {
		// skip whitespace
		while (Character.isWhitespace(currentChar))
			getNextChar();

		// construct token to be returned to parser
		token = new Token();
		token.next = null;

		// save start-of-token position
		token.beginLine = currentLineNumber;
		token.beginColumn = currentColumnNumber;

		// check for EOF
		if (currentChar == EOF) {
			token.image = "<EOF>";
			token.endLine = currentLineNumber;
			token.endColumn = currentColumnNumber;
			token.kind = EOF;
		}

		else // check for unsigned int
		if (Character.isDigit(currentChar)) {
			buffer.setLength(0); // clear buffer
			do // build token image in buffer
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

		else // check for identifier
		if (Character.isLetter(currentChar)) {
			buffer.setLength(0); // clear buffer
			do // build token image in buffer
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
			else if (token.image.equals("do"))
				token.kind = DO;
			else if (token.image.equals("if"))
				token.kind = IF;
			else if (token.image.equals("else"))
				token.kind = ELSE;
			else // not a keyword so kind is ID
				token.kind = ID;
		}

		else // check for string
		if (currentChar == '\"') {
			buffer.setLength(0); // clear buffer
			keepComment = true;
			do // build token image in buffer
			{
				if (currentChar != '\\')
					slashNum = 0;
				else
					slashNum += 1;
				buffer.append(currentChar);
				previousChar = currentChar;
				getNextChar();
				if (previousChar == '\\' && currentChar == '\n') {
					buffer.setLength(buffer.length() - 1);
					getNextChar();
				}
			} while (currentChar != '\n' && currentChar != '\r'
					&& (currentChar != '\"' || (currentChar == '\"' && slashNum % 2 == 1)));

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

		else // process single-character token
		{
			switch (currentChar) {
			case '=':
				token.kind = ASSIGN;
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
			default:
				token.kind = ERROR;
				break;
			}

			// save currentChar as String in token.image
			token.image = Character.toString(currentChar);

			// save end-of-token position
			token.endLine = currentLineNumber;
			token.endColumn = currentColumnNumber;

			getNextChar(); // read beyond end of token
		}

		// token trace appears as comments in output file
		if (debug)
			outFile.printf("; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n", token.kind, token.beginLine,
					token.beginColumn, token.endLine, token.endColumn, token.image);

		return token; // return token to parser
	}

	// -----------------------------------------
	private void getNextChar() {
		if (currentChar == EOF)
			return;

		if (currentChar == '\n') // need next line?
		{
			if (inFile.hasNextLine()) // any lines left?
			{
				inputLine = inFile.nextLine(); // get next line
				// output source line as comment
				outFile.println("; " + inputLine);
				inputLine = inputLine + "\n"; // mark line end
				currentColumnNumber = 0;
				currentLineNumber++;
			} else // at end of file
			{
				currentChar = EOF;
				return;
			}
		}

		// get next char from inputLine
		currentChar = inputLine.charAt(currentColumnNumber++);

		// in S4, test for single-line comment goes here
		if (currentChar == '/' && inputLine.charAt(currentColumnNumber) == '/' && !keepComment) {
			currentChar = '\n';
		}
	}
} // end of S4TokenMgr
	// ======================================================

class S4Parser implements S4Constants {
	private S4SymTab st;
	private S4TokenMgr tm;
	private S4CodeGen cg;
	private Token currentToken;
	private Token previousToken;

	// -----------------------------------------
	public S4Parser(S4SymTab st, S4TokenMgr tm, S4CodeGen cg) {
		this.st = st;
		this.tm = tm;
		this.cg = cg;
		// prime currentToken with first token
		currentToken = tm.getNextToken();
		previousToken = null;
	}

	// -----------------------------------------
	// Construct and return an exception that contains
	// a message consisting of the image of the current
	// token, its location, and the expected tokens.
	//
	private RuntimeException genEx(String errorMessage) {
		return new RuntimeException("Encountered \"" + currentToken.image + "\" on line " + currentToken.beginLine
				+ ", column " + currentToken.beginColumn + "." + System.getProperty("line.separator") + errorMessage);
	}

	// -----------------------------------------
	// Advance currentToken to next token.
	//
	private void advance() {
		previousToken = currentToken;

		// If next token is on token list, advance to it.
		if (currentToken.next != null)
			currentToken = currentToken.next;

		// Otherwise, get next token from token mgr and
		// put it on the list.
		else
			currentToken = currentToken.next = tm.getNextToken();
	}

	// -----------------------------------------
	// getToken(i) returns ith token without advancing
	// in token stream. getToken(0) returns
	// previousToken. getToken(1) returns currentToken.
	// getToken(2) returns next token, and so on.
	//
	private Token getToken(int i) {
		if (i <= 0)
			return previousToken;

		Token t = currentToken;
		for (int j = 1; j < i; j++) // loop to ith token
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

	// -----------------------------------------
	// If the kind of the current token matches the
	// expected kind, then consume advances to the next
	// token. Otherwise, it throws an exception.
	//
	private void consume(int expected) {
		if (currentToken.kind == expected)
			advance();
		else
			throw genEx("Expecting " + tokenImage[expected]);
	}

	// -----------------------------------------
	public void parse() {
		program(); // program is start symbol for grammar
	}

	// -----------------------------------------
	private void program() {
		statementList();
		cg.endCode();
		if (currentToken.kind != EOF) // garbage at end?
			throw genEx("Expecting <EOF>");
	}

	// -----------------------------------------
	private void statementList() {
		if (currentToken.kind == EOF)
			return;
		statement();
		statementList();
	}

	// -----------------------------------------
	private void statement() {
		try {
			switch (currentToken.kind) {
			case ID:
				assignmentStatement();
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
			case LEFTBRACE:
				compoundStatement();
				break;
			case SEMICOLON:
				nullStatement();
				break;
			case WHILE:
				whileStatement();
				break;
			case DO:
				doStatement();
				break;
			case IF:
				ifStatement();
				break;
			default:
				throw genEx("Expecting statement");
			}
		} catch (RuntimeException e) {
			System.err.println(e.getMessage());
			cg.emitString("; " + e.getMessage());

			while (currentToken.kind != SEMICOLON && currentToken.kind != EOF)
				advance();
			if (currentToken.kind != EOF)
				advance();
		}
	}

	// -----------------------------------------
	private void ifStatement() {
		String iLabel1;
		switch (currentToken.kind) {
		case IF:
			consume(IF);
			consume(LEFTPAREN);
			expr();
			consume(RIGHTPAREN);
			iLabel1 = cg.getLabel();
			cg.emitInstruction("jz", iLabel1);
			statement();
			elsePart(iLabel1);
			break;
		default:
			throw genEx("Expected if");
		}
	}

	private void elsePart(String Label1) {
		String eLabel2;
		String eLabel1 = Label1;
		switch (currentToken.kind) {
		case ELSE:
			consume(ELSE);
			eLabel2 = cg.getLabel();
			cg.emitInstruction("ja", eLabel2);
			cg.emitLabel(eLabel1);
			statement();
			cg.emitLabel(eLabel2);
			break;
		default:
			cg.emitLabel(eLabel1);
			break;
		}
	}

	// -----------------------------------------
	private void doStatement() {
		String doLabel1;
		switch (currentToken.kind) {
		case DO:
			consume(DO);
			doLabel1 = cg.getLabel();
			cg.emitLabel(doLabel1);
			statement();
			consume(WHILE);
			consume(LEFTPAREN);
			expr();
			consume(RIGHTPAREN);
			consume(SEMICOLON);
			cg.emitInstruction("jnz", doLabel1);
			break;
		default:
			throw genEx("Expected do");
		}
	}

	// -----------------------------------------
	private void whileStatement() {
		String wLabel1, wLabel2;
		switch (currentToken.kind) {
		case WHILE:
			consume(WHILE);
			wLabel1 = cg.getLabel();
			cg.emitLabel(wLabel1);
			consume(LEFTPAREN);
			expr();
			consume(RIGHTPAREN);
			wLabel2 = cg.getLabel();
			cg.emitInstruction("jz", wLabel2);
			statement();
			cg.emitInstruction("ja", wLabel1);
			cg.emitLabel(wLabel2);
			break;
		default:
			throw genEx("Expected while");
		}
	}

	// -----------------------------------------
	private void compoundStatement() {
		switch (currentToken.kind) {
		case LEFTBRACE:
			consume(LEFTBRACE);
			compoundList();
			consume(RIGHTBRACE);
			break;
		default:
			throw genEx("Expected left brace");
		}
	}

	// -----------------------------------------
	private void compoundList() {
		if (currentToken.kind == RIGHTBRACE)
			return;
		statement();
		compoundList();
	}

	// -----------------------------------------
	private void nullStatement() {
		switch (currentToken.kind) {
		case SEMICOLON:
			consume(SEMICOLON);
			break;
		default:
			throw genEx("Expecting semicolon");
		}
	}

	// -----------------------------------------
	private void assignmentStatement() {
		Token t;

		t = currentToken;
		consume(ID);
		st.enter(t.image);
		cg.emitInstruction("pc", t.image);
		consume(ASSIGN);
		assignmentTail();
		cg.emitInstruction("stav");
	}

	// -----------------------------------------
	private void assignmentTail() {
		Token t;

		t = currentToken;
		if (currentToken.kind == ID && getToken(2).kind == ASSIGN) {
			consume(ID);
			st.enter(t.image);
			cg.emitInstruction("pc", t.image);
			consume(ASSIGN);
			assignmentTail();
			cg.emitInstruction("dupe");
			cg.emitInstruction("rot");
			cg.emitInstruction("stav");
		} else {
			expr();
			consume(SEMICOLON);
		}

	}

	// -----------------------------------------
	private void printlnStatement() {
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

	// -----------------------------------------
	private void printStatement() {
		consume(PRINT);
		consume(LEFTPAREN);
		printArg();
		consume(RIGHTPAREN);
		consume(SEMICOLON);
	}

	// -----------------------------------------
	private void printArg() {
		Token t;

		switch (currentToken.kind) {
		case STRING: // print out string
			t = currentToken;
			consume(STRING);
			String label = cg.getLabel();
			cg.emitInstruction("pc", label);
			cg.emitInstruction("sout");
			cg.emitdw("^" + label, t.image);
			break;
		default: // assume it's an expression
			expr();
			cg.emitInstruction("dout");
		}
	}

	// -----------------------------------------
	private void readintStatement() {
		Token t;
		consume(READINT);
		consume(LEFTPAREN);

		t = currentToken;
		consume(ID);
		cg.emitInstruction("pc", t.image);
		cg.emitInstruction("din");
		cg.emitInstruction("stav");

		consume(RIGHTPAREN);
		consume(SEMICOLON);
	}

	// -----------------------------------------
	private void expr() {
		term();
		termList();
	}

	// -----------------------------------------
	private void termList() {
		switch (currentToken.kind) {
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
		case RIGHTPAREN:
		case SEMICOLON:
			;
			break;
		default:
			throw genEx("Expecting \"+\", \")\", or \";\"");
		}
	}

	// -----------------------------------------
	private void term() {
		factor();
		factorList();
	}

	// -----------------------------------------
	private void factorList() {
		switch (currentToken.kind) {
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
		case PLUS:
		case MINUS:
		case RIGHTPAREN:
		case SEMICOLON:
			;
			break;
		default:
			throw genEx("Expecting op, \")\", or \";\"");
		}
	}

	// -----------------------------------------
	private void factor() {
		Token t;

		switch (currentToken.kind) {
		case UNSIGNED:
			t = currentToken;
			if (t.image.length() > 5 || Integer.parseInt(t.image) > 32767)
				throw genEx("Expecting integer (-32768 to 32767)");
			consume(UNSIGNED);
			cg.emitInstruction("pwc", t.image);
			break;
		case PLUS:
			consume(PLUS);
			factor();
			break;
		case MINUS:
			consume(MINUS);
			negFactor();
			break;
		case ID:
			t = currentToken;
			consume(ID);
			st.enter(t.image);
			cg.emitInstruction("p", t.image);
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

	// -----------------------------------------
	private void negFactor() {
		Token t;

		switch (currentToken.kind) {
		case UNSIGNED:
			t = currentToken;
			if (t.image.length() > 5 || Integer.parseInt(t.image) > 32768)
				throw genEx("Expecting integer (-32768 to 32767)");
			consume(UNSIGNED);
			cg.emitInstruction("pwc", "-" + t.image);
			break;
		case ID:
			t = currentToken;
			consume(ID);
			st.enter(t.image);
			cg.emitInstruction("p", t.image);
			cg.emitInstruction("neg");
			break;
		case LEFTPAREN:
			consume(LEFTPAREN);
			expr();
			consume(RIGHTPAREN);
			cg.emitInstruction("neg");
			break;
		case PLUS:
			while (currentToken.kind == PLUS)
				consume(PLUS);
			t = currentToken;

			if (t.kind == MINUS) {
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
} // end of S4Parser
	// ======================================================

class S4CodeGen {
	private PrintWriter outFile;
	private S4SymTab st;
	private int labelNumber;

	// -----------------------------------------
	public S4CodeGen(PrintWriter outFile, S4SymTab st) {
		this.outFile = outFile;
		this.st = st;
		this.labelNumber = 0;
	}

	// -----------------------------------------
	public String getLabel() {
		return "@L" + labelNumber++;
	}

	// -----------------------------------------
	public void emitLabel(String label) {
		outFile.printf("%-4s%n", label + ":");
	}

	// -----------------------------------------
	public void emitInstruction(String op) {
		outFile.printf("          %-4s%n", op);
	}

	// -----------------------------------------
	public void emitInstruction(String op, String opnd) {
		outFile.printf("          %-4s      %s%n", op, opnd);
	}

	// -----------------------------------------
	public void emitdw(String label, String value) {
		outFile.printf("%-9s dw        %s%n", label + ":", value);
	}

	// -----------------------------------------
	public void emitString(String m) {
		outFile.printf(m);
	}

	// -----------------------------------------
	public void endCode() {
		outFile.println();
		emitInstruction("halt");

		int size = st.getSize();
		// emit dw for each symbol in the symbol table
		for (int i = 0; i < size; i++)
			emitdw(st.getSymbol(i), "0");
	}
} // end of S4CodeGen
