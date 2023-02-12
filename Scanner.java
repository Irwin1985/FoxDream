package core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
//import java.io.IOException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.util.ArrayList;

public class Scanner {
	private final String source;
	private int cursor = 0;
	private int tokenCounter = 0;
	private Kind lastToken;
	private int line = 1;
	private int col = 1;
	private String lexeme = "";
	
	private final List<Token> tokens = new ArrayList<Token>();
	
	/**
	 * Tokenizer specifications.
	 */
	static class Spec {
		final Pattern pattern;
		final Kind kind;
		final Category category;
		
		public Spec(Pattern pattern, Kind kind, Category category) {
			this.pattern = pattern;
			this.kind = kind;
			this.category = category;
		}
	}
	
	private Spec[] specs = {
		// -----------------------------------------------------------
		// Whitespace Pattern pattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
		new Spec(Pattern.compile("^[ \\t\\r\\f]+", Pattern.CASE_INSENSITIVE), Kind.IGNORE, Category.IGNORABLE),

        // -----------------------------------------------------------
        // Comments:
        // Skip single-line comments
        new Spec(Pattern.compile("^\\/\\/.*", Pattern.CASE_INSENSITIVE), Kind.IGNORE, Category.IGNORABLE),               

        // Skip multi-line comments
        new Spec(Pattern.compile("^\\/\\*[\\s\\S]*?\\*\\/", Pattern.CASE_INSENSITIVE), Kind.IGNORE, Category.IGNORABLE),	
        
        // Comma + new line: it's used to concatenate expressions.
        // -----------------------------------------------------------
        new Spec(Pattern.compile("^;[\\s]*?\\n", Pattern.CASE_INSENSITIVE), Kind.IGNORE, Category.IGNORABLE),
        
        // Single comma: comma is threated like a space, it means nothing. 
        // -----------------------------------------------------------
        new Spec(Pattern.compile("^;", Pattern.CASE_INSENSITIVE), Kind.IGNORE, Category.IGNORABLE),
        
        // -----------------------------------------------------------
        // NewLine:
        new Spec(Pattern.compile("^\\n+", Pattern.CASE_INSENSITIVE), Kind.SEMICOLON, Category.GENERIC),

        // -----------------------------------------------------------
        // Numbers:
        new Spec(Pattern.compile("^\\d+[_.\\d]*", Pattern.CASE_INSENSITIVE), Kind.NUMBER, Category.LITERAL),

        // -----------------------------------------------------------
        // Double quoted string:
        new Spec(Pattern.compile("^\"(?:[^\"\\\\^'\\\\]|\\\\.)*\"", Pattern.CASE_INSENSITIVE), Kind.STRING, Category.LITERAL),

        // -----------------------------------------------------------
        // Single quoted string:
        new Spec(Pattern.compile("^'(?:[^\"\\\\^'\\\\]|\\\\.)*'", Pattern.CASE_INSENSITIVE), Kind.STRING, Category.LITERAL),       

        // -----------------------------------------------------------
        // Backticked string:
        new Spec(Pattern.compile("^`[^`]*`", Pattern.CASE_INSENSITIVE), Kind.STRING, Category.LITERAL),       
        
        // -----------------------------------------------------------
        // Relational Operators:
        new Spec(Pattern.compile("^[<>]=?", Pattern.CASE_INSENSITIVE), Kind.RELATIONAL_OPERATOR, Category.GENERIC),
        new Spec(Pattern.compile("^[=!]=", Pattern.CASE_INSENSITIVE), Kind.EQUALITY_OPERATOR, Category.GENERIC),

        // -----------------------------------------------------------
        // Logical Operators:
        new Spec(Pattern.compile("^\\.and\\.|^\\band\\b", Pattern.CASE_INSENSITIVE), Kind.LOGICAL_AND, Category.GENERIC),
        new Spec(Pattern.compile("^\\.or\\.|^\\bor\\b", Pattern.CASE_INSENSITIVE), Kind.LOGICAL_OR, Category.GENERIC),
        new Spec(Pattern.compile("^!", Pattern.CASE_INSENSITIVE), Kind.LOGICAL_NOT, Category.UNARY),

        // -----------------------------------------------------------
        // Keywords:
        new Spec(Pattern.compile("^\\bas\\b", Pattern.CASE_INSENSITIVE), Kind.AS, Category.KEYWORD),
        new Spec(Pattern.compile("^\\blocal\\b", Pattern.CASE_INSENSITIVE), Kind.LOCAL, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bpublic\\b", Pattern.CASE_INSENSITIVE), Kind.PUBLIC, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bconst\\b", Pattern.CASE_INSENSITIVE), Kind.CONST, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bif\\b", Pattern.CASE_INSENSITIVE), Kind.IF, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bthen\\b", Pattern.CASE_INSENSITIVE), Kind.THEN, Category.KEYWORD),
        new Spec(Pattern.compile("^\\belse\\b", Pattern.CASE_INSENSITIVE), Kind.ELSE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendif\\b", Pattern.CASE_INSENSITIVE), Kind.ENDIF, Category.KEYWORD),
        new Spec(Pattern.compile("^\\.(t|true)\\.|^true", Pattern.CASE_INSENSITIVE), Kind.TRUE, Category.LITERAL),
        new Spec(Pattern.compile("^\\.(f|false)\\.|^false", Pattern.CASE_INSENSITIVE), Kind.FALSE, Category.LITERAL),
        new Spec(Pattern.compile("^\\.null\\.|^\\bnull\\b", Pattern.CASE_INSENSITIVE), Kind.NULL, Category.LITERAL),
        new Spec(Pattern.compile("^\\breturn\\b", Pattern.CASE_INSENSITIVE), Kind.RETURN, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bwhile\\b", Pattern.CASE_INSENSITIVE), Kind.WHILE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendwhile\\b", Pattern.CASE_INSENSITIVE), Kind.ENDWHILE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\benddo\\b", Pattern.CASE_INSENSITIVE), Kind.ENDDO, Category.KEYWORD),
        new Spec(Pattern.compile("^\\brepeat\\b", Pattern.CASE_INSENSITIVE), Kind.REPEAT, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bprint\\b", Pattern.CASE_INSENSITIVE), Kind.PRINT, Category.KEYWORD),
        new Spec(Pattern.compile("^\\buntil\\b", Pattern.CASE_INSENSITIVE), Kind.UNTIL, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bclass\\b", Pattern.CASE_INSENSITIVE), Kind.CLASS, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendclass\\b", Pattern.CASE_INSENSITIVE), Kind.ENDCLASS, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bthis\\b", Pattern.CASE_INSENSITIVE), Kind.THIS, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bcreateobject\\b", Pattern.CASE_INSENSITIVE), Kind.CREATEOBJECT, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bfor\\b", Pattern.CASE_INSENSITIVE), Kind.FOR, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bto\\b", Pattern.CASE_INSENSITIVE), Kind.TO, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bstep\\b", Pattern.CASE_INSENSITIVE), Kind.STEP, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendfor\\b", Pattern.CASE_INSENSITIVE), Kind.ENDFOR, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bdodefault\\b", Pattern.CASE_INSENSITIVE), Kind.DODEFAULT, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bfunction\\b", Pattern.CASE_INSENSITIVE), Kind.FUNCTION, Category.KEYWORD),
        new Spec(Pattern.compile("^\\blparameters\\b", Pattern.CASE_INSENSITIVE), Kind.LPARAMETERS, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendfunc\\b", Pattern.CASE_INSENSITIVE), Kind.ENDFUNC, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bdo\\b", Pattern.CASE_INSENSITIVE), Kind.DO, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bcase\\b", Pattern.CASE_INSENSITIVE), Kind.CASE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\botherwise\\b", Pattern.CASE_INSENSITIVE), Kind.OTHERWISE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendcase\\b", Pattern.CASE_INSENSITIVE), Kind.ENDCASE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bexit\\b", Pattern.CASE_INSENSITIVE), Kind.EXIT, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bloop\\b", Pattern.CASE_INSENSITIVE), Kind.LOOP, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bfor\\b", Pattern.CASE_INSENSITIVE), Kind.FOR, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bto\\b", Pattern.CASE_INSENSITIVE), Kind.TO, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bstep\\b", Pattern.CASE_INSENSITIVE), Kind.STEP, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bprivate\\b", Pattern.CASE_INSENSITIVE), Kind.PRIVATE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bimport\\b", Pattern.CASE_INSENSITIVE), Kind.IMPORT, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bmodule\\b", Pattern.CASE_INSENSITIVE), Kind.MODULE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bendmodule\\b", Pattern.CASE_INSENSITIVE), Kind.ENDMODULE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\brelease\\b", Pattern.CASE_INSENSITIVE), Kind.RELEASE, Category.KEYWORD),
        new Spec(Pattern.compile("^\\bdefer\\b", Pattern.CASE_INSENSITIVE), Kind.DEFER, Category.KEYWORD),
        new Spec(Pattern.compile("^\\benddefer\\b", Pattern.CASE_INSENSITIVE), Kind.ENDDEFER, Category.KEYWORD),

        // -----------------------------------------------------------
        // Assignment operators: =, +=, -=, *=, /=
        new Spec(Pattern.compile("^=", Pattern.CASE_INSENSITIVE), Kind.SIMPLE_ASSIGN, Category.ASSIGNMENT),
        new Spec(Pattern.compile("^[\\+\\-\\*\\/]=", Pattern.CASE_INSENSITIVE), Kind.COMPLEX_ASSIGN, Category.ASSIGNMENT),

        // -----------------------------------------------------------
        // Math operators: +, -, *, /
        new Spec(Pattern.compile("^[\\+\\-]", Pattern.CASE_INSENSITIVE), Kind.TERM_OPERATOR, Category.UNARY),
        new Spec(Pattern.compile("^[\\*//]", Pattern.CASE_INSENSITIVE), Kind.FACTOR_OPERATOR, Category.GENERIC),
        
        // -----------------------------------------------------------
        // Identifier
        new Spec(Pattern.compile("^\\w+", Pattern.CASE_INSENSITIVE), Kind.IDENTIFIER, Category.IDENTIFIER),
        
        // -----------------------------------------------------------
        // Symbols and delimiters:
        new Spec(Pattern.compile("^\\(", Pattern.CASE_INSENSITIVE), Kind.LPAREN, Category.GENERIC),
        new Spec(Pattern.compile("^\\)", Pattern.CASE_INSENSITIVE), Kind.RPAREN, Category.GENERIC),
        new Spec(Pattern.compile("^\\[", Pattern.CASE_INSENSITIVE), Kind.LBRACKET, Category.GENERIC),
        new Spec(Pattern.compile("^\\]", Pattern.CASE_INSENSITIVE), Kind.RBRACKET, Category.GENERIC),        
        new Spec(Pattern.compile("^,", Pattern.CASE_INSENSITIVE), Kind.COMMA, Category.GENERIC),        
        new Spec(Pattern.compile("^\\.", Pattern.CASE_INSENSITIVE), Kind.DOT, Category.GENERIC),        
        new Spec(Pattern.compile("^\\:", Pattern.CASE_INSENSITIVE), Kind.COLON, Category.GENERIC),
        new Spec(Pattern.compile("^\\?", Pattern.CASE_INSENSITIVE), Kind.QUESTION, Category.GENERIC)
	};
	
	/**
	 * Scanner
	 */
	public Scanner(String source) {
		if (!source.endsWith("\n")) {
			source += "\n";
		}
		this.source = source;
		cursor = 0;
	}
	
	/**
	 * scanTokens
	 */
	public List<Token> scanTokens() {
		for (;;) {
			Token token = getNextToken();
			if (token == null)
				break;
			tokens.add(token);
		}
		tokens.add(new Token(Kind.EOF, Category.GENERIC, "", "", line, col));
		return tokens;
	}
	
	/**
	 * Obtains next token.
	 */
	private Token getNextToken() {
		if (cursor >= source.length())
			return null;
		
		String input = source.substring(cursor);
		
		for (Spec spec : specs) {			
			Matcher matcher = spec.pattern.matcher(input);
			if (!matcher.find()) {
				continue;
			}
			// increase cursor to the length of matched string.
			cursor += matcher.end();
			lexeme = matcher.group(0);
			
			// count number of lines
			int ln = lexeme.length() - lexeme.replace("\n", "").length();
			line += ln;
			if (ln > 0) {
				col = 1;
			}
						
			// check for the IGNORE token type.
			if (spec.kind == Kind.IGNORE) {
				col += lexeme.length(); // update column number.
				return getNextToken();
			}
			
			// check for new line
			if (spec.kind == Kind.SEMICOLON) {
				if (lastToken == Kind.SEMICOLON || tokenCounter == 0) {
					return getNextToken();
				}
				lastToken = Kind.SEMICOLON;
				lexeme = "";
			} else {
				lastToken = spec.kind;
			}
			tokenCounter++;
			
			// return the token and value
			Object value = "";
			Category category = spec.category;
			
			switch (spec.kind) {
			case NUMBER:				
				lexeme = lexeme.replaceAll("_", "");
				value = Double.parseDouble(lexeme);
				break;
			case TRUE:
				value = true;
				break;
			case FALSE:
				value = false;
				break;
			case NULL:
				value = null;
				break;
			case STRING:				
				if (lexeme.startsWith("`")) { // raw string
					lexeme = lexeme.substring(1, lexeme.length()-1);
				} else {
					lexeme = lexeme.substring(1, lexeme.length()-1);
					lexeme = lexeme.replaceAll("\\\\r", "\r");
					lexeme = lexeme.replaceAll("\\\\n", "\n");
					lexeme = lexeme.replaceAll("\\\\t", "\t");
					lexeme = lexeme.replaceAll("\\\\\"", "\"");
					lexeme = lexeme.replaceAll("\\\\\'", "\'");
				}				
				value = lexeme;
				break;
			case SEMICOLON:
				value = "new line";
			case COMPLEX_ASSIGN, TERM_OPERATOR, FACTOR_OPERATOR, RELATIONAL_OPERATOR, EQUALITY_OPERATOR, LOGICAL_NOT:
				switch (lexeme) {
				case "+", "+=": category = Category.PLUS; break;				
				case "-", "-=": category = Category.MINUS; break;
				case "*", "*=": category = Category.MUL; break;
				case "/", "/=": category = Category.DIV; break;
				case "=": category = Category.ASSIGN; break;
				case "<": category = Category.LESS; break;
				case "<=": category = Category.LESS_EQ; break;
				case ">": category = Category.GREATER; break;
				case ">=": category = Category.GREATER_EQ; break;
				case "==": category = Category.EQUAL; break;
				case "!": category = Category.BANG; break;
				case "!=": category = Category.NOT_EQ; break;
				default: break;
				}
			default:
				value = lexeme;
				break;
			}
			Token tok = new Token(spec.kind, category, lexeme, value, line, col);
			col += lexeme.length();
			
			return tok;
		}
		
		FoxDream.error(line, col, "Unknown character: " + input.charAt(0));
		cursor += input.length();
		return null;
	}
	
	// Test Scanner
//	public static void main(String[] args) throws IOException {
//		byte[] bytes = Files.readAllBytes(Paths.get("C:\\Users\\irwin.SUBIFOR\\eclipse-2022-06\\FoxDream\\test.ybase"));
//		String source = new String(bytes, Charset.defaultCharset());
//		
//		Scanner sc = new Scanner(source);
//		List<Token> tokens = sc.scanTokens();
//		
//		for (Token tok : tokens) {
//			System.out.println(tok);
//		}
//		
//	}
}
