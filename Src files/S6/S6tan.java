/*
  Names:
    Jackson Maddox
    Liangyu Tan
    Johirul Islam
*/

// Hand-written S6 compiler
import java.io.*;
import java.util.*;

//======================================================
class S6 {
	public static void main(String[] args) throws IOException {
		System.out.println("S6 compiler written by ...");
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
		outFile.println("; from S6 compiler written by ...");
		outFile.println("; Jackson Maddox");
		outFile.println("; Liangyu Tan");
		outFile.println("; Johirul Islam");

		// construct objects that make up compiler
		S6SymTab st = new S6SymTab();
		S6TokenMgr tm = new S6TokenMgr(inFile, outFile, debug);
		S6CodeGen cg = new S6CodeGen(outFile, st);
		S6Parser parser = new S6Parser(st, tm, cg);

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
} // end of S6
	// ======================================================

interface S6Constants {
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

	// tokenImage provides string for each token kind
	String[] tokenImage = { "<EOF>", "\"println\"", "<UNSIGNED>", "<ID>", "\"=\"", "\";\"", "\"(\"", "\")\"", "\"+\"",
			"\"-\"", "\"*\"", "<ERROR>", "\"/\"", "\"{\"", "\"}\"", "\"print\"", "<STRING>", "\"readint\"", "\"while\"",
			"\"if\"", "\"do\"", "\"else\"", "\"extern\"", "\"int\"", "\"void\"", "\",\"", "\"break\"", };
} // end of S6Constants
	// ======================================================

class S6SymTab implements S6Constants {
	private ArrayList<String> symbol;
	private ArrayList<Integer> relAdd;
	private ArrayList<Integer> category;

	// -----------------------------------------
	public S6SymTab() {
		symbol = new ArrayList<>();
		relAdd = new ArrayList<>();
		category = new ArrayList<>();
	}

	// -----------------------------------------
	public void enter(String s, int ra, int cat) {
		int index = symbol.indexOf(s);

		// if s is not in symbol, then add it
		if (index < 0) {
			symbol.add(s);
			relAdd.add(ra);
			category.add(cat);
		} else {
			int currCat = category.get(index);
			if (cat == FUNCTIONCALL && (currCat == FUNCTIONCALL || currCat == FUNCTIONDEFINITION)) {

				; // do nothing

			} else if (cat == FUNCTIONDEFINITION && currCat == FUNCTIONCALL) {

				category.set(index, FUNCTIONDEFINITION);

			} else if (cat == LOCALVARIABLE && (currCat == GLOBALVARIABLE || currCat == EXTERNVARIABLE)) {

				symbol.add(s);
				relAdd.add(ra);
				category.add(cat);

			} else {
				throw new RuntimeException("For symbol " + s + " got cat " + cat + " while having cat " + currCat);
			}
		}
	}

	// -----------------------------------------
	public int find(String sym) {
		for (int i = symbol.size() - 1; i >= 0; i--) {
			if (symbol.get(i).equals(sym))
				return i;
		}
		throw new RuntimeException("No such symbol " + sym);
	}

	// -----------------------------------------
	public String getSymbol(int index) {
		return symbol.get(index);
	}

	// -----------------------------------------
	public Integer getRelAdd(int index) {
		return relAdd.get(index);
	}

	// -----------------------------------------
	public Integer getCategory(int index) {
		return category.get(index);
	}

	// -----------------------------------------
	public int getSize() {
		return symbol.size();
	}

	// -----------------------------------------
	public void localRemove() {
		// Inefficient, but should work
		boolean foundLocal = true;
		while (foundLocal) {
			foundLocal = false;

			for (int i = 0; i < symbol.size(); i++) {
				if (category.get(i) != LOCALVARIABLE)
					continue;
				symbol.remove(i);
				relAdd.remove(i);
				category.remove(i);
				foundLocal = true;
			}
		}
	}
} // end of S6SymTab
	// ======================================================

class S6TokenMgr implements S6Constants {
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
	// -----------------------------------------

	public S6TokenMgr(Scanner inFile, PrintWriter outFile, boolean debug) {
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
			else // not a keyword so kind is ID
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
					buffer.setLength(buffer.length() - 1); // remove the \
															// character
					continue;
				}

				buffer.append(currentChar);
				getNextChar();
			} while (currentChar != '\n' && currentChar != '\r' && currentChar != '\"' || slashCount % 2 != 0);

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
			case ',':
				token.kind = COMMA;
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

		// in S6, test for single-line comment goes here
		if (currentChar == '/' && inputLine.charAt(currentColumnNumber) == '/' && !keepComment) {
			currentChar = '\n';
		}
	}
} // end of S6TokenMgr
	// ======================================================

class S6Parser implements S6Constants {
	private S6SymTab st;
	private S6TokenMgr tm;
	private S6CodeGen cg;
	private Token currentToken;
	private Token previousToken;
	private ArrayList<String> breakLabel = new ArrayList<>();

	// -----------------------------------------
	public S6Parser(S6SymTab st, S6TokenMgr tm, S6CodeGen cg) {
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
		programUnitList();
		cg.endCode();
		if (currentToken.kind != EOF) // garbage at end?
			throw genEx("Expecting <EOF>");
	}

	// -----------------------------------------
	private void programUnitList() {
		if (currentToken.kind == EOF)
			return;

		programUnit();
		programUnitList();
	}

	// -----------------------------------------
	private void programUnit() {
		switch (currentToken.kind) {
		case EXTERN:
			externDeclaration();
			break;
		case INT:
			globalDeclaration();
			break;
		case VOID:
			functionDefinition();
			break;
		default:
			throw genEx("Expecting extern, global, or function declaration");
		}
	}

	// -----------------------------------------
	private void externDeclaration() {
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

	// -----------------------------------------
	private void globalDeclaration() {
		consume(INT);
		global();

		while (currentToken.kind == COMMA) {
			consume(COMMA);
			global();
		}
		consume(SEMICOLON);
	}

	// -----------------------------------------
	private void global() {
		Token t1, t2;
		String initVal;

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
		}

		st.enter(t1.image, 0, GLOBALVARIABLE);
		cg.emitdw(t1.image, initVal);
	}

	// -----------------------------------------
	private void functionDefinition() {
		Token t;

		consume(VOID);
		t = currentToken;
		consume(ID);
		cg.emitString("; =============== start of function " + t.image);
		st.enter(t.image, 0, FUNCTIONDEFINITION);
		cg.emitInstruction("public", t.image);
		cg.emitLabel(t.image);
		consume(LEFTPAREN);
		if (currentToken.kind == INT)
			parameterList();
		consume(RIGHTPAREN);
		consume(LEFTBRACE);
		cg.emitInstruction("esba");
		localDeclarations();
		statementList();
		consume(RIGHTBRACE);
		cg.emitInstruction("reba");
		cg.emitInstruction("ret");
		cg.emitString("; =============== end of function " + t.image);
		st.localRemove();
	}

	// -----------------------------------------
	private void parameterList() {
		Token t;
		int p;

		t = parameter();
		p = parameterR();
		st.enter(t.image, p, LOCALVARIABLE);
	}

	// -----------------------------------------
	private Token parameter() {
		Token t;

		consume(INT);
		t = currentToken;
		consume(ID);
		return t;
	}

	// -----------------------------------------
	private int parameterR() {
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

	// -----------------------------------------
	private void localDeclarations() {
		int relativeAddress = -1;

		while (currentToken.kind == INT) {
			consume(INT);
			local(relativeAddress--); // process 1 local var
			while (currentToken.kind == COMMA) {
				consume(COMMA);
				local(relativeAddress--); // process 1 local var
			}
			consume(SEMICOLON);
		}
	}

	// -----------------------------------------
	private void local(int relativeAddress) {
		Token t;
		String sign;

		t = currentToken;
		consume(ID);
		st.enter(t.image, relativeAddress, LOCALVARIABLE);

		if (currentToken.kind == ASSIGN) {
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
			cg.emitInstruction("pwc", sign + t.image);
		} else {
			cg.emitInstruction("asp", "-1");
		}
	}

	// -----------------------------------------
	private void statementList() {
		if (currentToken.kind == RIGHTBRACE)
			return;

		statement();
		statementList();
	}

	// -----------------------------------------
	private void statement() {
		try {
			switch (currentToken.kind) {
			case ID:
				if (getToken(2).kind == ASSIGN)
					assignmentStatement();
				else if (getToken(2).kind == LEFTPAREN)
					functionCall();
				else
					throw genEx("Expecting assignment or function call");
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

	// -----------------------------------------
	private void breakStatement() {
		consume(BREAK);
		int lastInd = breakLabel.size() - 1;
		cg.emitInstruction("ja", breakLabel.get(lastInd));
		breakLabel.remove(lastInd);
		consume(SEMICOLON);
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
		int index;

		t = currentToken;
		consume(ID);
		index = st.find(t.image);
		cg.pushAddress(index);
		consume(ASSIGN);
		assignmentTail();
		cg.emitInstruction("stav");
	}

	// -----------------------------------------
	private void assignmentTail() {
		Token t;
		int index;

		t = currentToken;
		if (currentToken.kind == ID && getToken(2).kind == ASSIGN) {
			consume(ID);
			index = st.find(t.image);
			cg.pushAddress(index);
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

	// -----------------------------------------
	private void whileStatement() {
		String label1, label2;

		consume(WHILE);

		label1 = cg.getLabel();
		cg.emitLabel(label1);

		consume(LEFTPAREN);
		expr();
		consume(RIGHTPAREN);

		label2 = cg.getLabel();
		breakLabel.add(label2);// get the label for breakStatement
		cg.emitInstruction("jz", label2);

		statement();

		cg.emitInstruction("ja", label1);
		cg.emitLabel(label2);
	}

	// -----------------------------------------
	private void ifStatement() {
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

	// -----------------------------------------
	private void elsePart(String label1) {
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

	// -----------------------------------------
	private void doWhileStatement() {
		String label1;
		String label2; // for break

		consume(DO);

		label1 = cg.getLabel();
		
		label2 = cg.getLabel();
		breakLabel.add(label2);
		
		cg.emitLabel(label1);

		statement();

		consume(WHILE);
		consume(LEFTPAREN);
		expr();
		consume(RIGHTPAREN);
		consume(SEMICOLON);

		cg.emitInstruction("jnz", label1);
		cg.emitLabel(label2);
	}

	// -----------------------------------------
	private void functionCall() {
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
		if (count > 0)
			cg.emitInstruction("asp", Integer.toString(count));
		consume(RIGHTPAREN);
		consume(SEMICOLON);
	}

	// -----------------------------------------
	private int argumentList() {
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
		case COMMA:
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
		case COMMA:
			;
			break;
		default:
			throw genEx("Expecting op, \")\", or \";\"");
		}
	}

	// -----------------------------------------
	private void factor() {
		Token t;
		int index;

		switch (currentToken.kind) {
		case UNSIGNED:
			t = currentToken;
			if (t.image.length() > 5 || Integer.parseInt(t.image) > 32767) {
				throw genEx("Expecting integer (-32768 to 32767)");
			}
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
			index = st.find(t.image);
			cg.push(index);
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
		int index;

		switch (currentToken.kind) {
		case UNSIGNED:
			t = currentToken;
			if (t.image.length() > 5 || Integer.parseInt(t.image) > 32768) {
				throw genEx("Expecting integer (-32768 to 32767)");
			}
			consume(UNSIGNED);
			cg.emitInstruction("pwc", "-" + t.image);
			break;
		case ID:
			t = currentToken;
			consume(ID);
			index = st.find(t.image);
			cg.push(index);
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
} // end of S6Parser
	// ======================================================

class S6CodeGen implements S6Constants {
	private PrintWriter outFile;
	private S6SymTab st;
	private int labelNumber;

	// -----------------------------------------
	public S6CodeGen(PrintWriter outFile, S6SymTab st) {
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
		outFile.printf("%s%n", label + ":");
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
	public void emitString(String line) {
		outFile.printf("%s%n", line);
	}

	// -----------------------------------------
	public void push(int p) {
		int cat = st.getCategory(p);
		if (cat == LOCALVARIABLE)
			emitInstruction("pr", Integer.toString(st.getRelAdd(p)));
		else
			emitInstruction("p", st.getSymbol(p));
	}

	// -----------------------------------------
	public void pushAddress(int p) {
		int cat = st.getCategory(p);
		if (cat == LOCALVARIABLE)
			emitInstruction("cora", Integer.toString(st.getRelAdd(p)));
		else
			emitInstruction("pc", st.getSymbol(p));
	}

	// -----------------------------------------
	public void endCode() {
		outFile.println();

		int size = st.getSize();
		// emit extern for each FUNCTIONCALL in the symbol table
		for (int i = 0; i < size; i++)
			if (st.getCategory(i) == FUNCTIONCALL)
				emitInstruction("extern", st.getSymbol(i));
	}
} // end of S6CodeGen
