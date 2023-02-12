package core;

public class Token {
	Kind kind;
	String lexeme;
	Category category;	
	Object literal;
	int line;
	int col;
	
	public Token(Kind k) {
		this.kind = k;
	}
	
	public Token(Kind kind, Category category, String lexeme, Object literal, int line, int col) {
		this.kind = kind;
		this.lexeme = lexeme;
		this.category = category;
		this.literal = literal;
		this.line = line;
		this.col = col;
	}
	
	@Override
	public String toString() {
		return String.format("%s[%d:%d]<lexeme: '%s'>", kind, line, col, lexeme);
	}
}
