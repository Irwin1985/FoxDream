package core;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Parser {
	private Scanner tokenizer;
	private List<Token> tokens;
	private int current = 0;
	
	@SuppressWarnings("serial")
	private static class ParseError extends RuntimeException{}

	// this is the main entry point for all the productions.
	public List<Stmt> parse(String source) {
		tokenizer = new Scanner(source);		
		tokens = tokenizer.scanTokens();			
		
		// Parse recursively stating from the main
		// entry point, the Program.
		return statementList();
	}
	
	// statementList ::= declaration* EOF;
	private List<Stmt> statementList() {
		List<Stmt> statements = new ArrayList<Stmt>();
		
		while (!isAtEnd()) {
			statements.add(declaration());
		}	
		
		return statements;
	}
	
	// declaration ::= statement;
	private Stmt declaration() {
		try {
			if (match(Kind.LOCAL)) {
				return parseVariableDeclaration(Kind.LOCAL);
			}
			if (match(Kind.PUBLIC)) {
				return parseVariableDeclaration(Kind.PUBLIC);
			}
			if (match(Kind.CONST)) {
				return parseConstantDeclaration();
			}
			if (match(Kind.FUNCTION)) {
				return parseFunctionDeclaration();
			}
			if (match(Kind.MODULE)) {
				return parseModuleDeclaration();
			}
			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}
	
	// statement ::= printStmt | expressionStmt;
	private Stmt statement() {
		if (match(Kind.PRINT) || match(Kind.QUESTION)) {
			return printStatement();
		}
		if (match(Kind.RETURN)) {
			return returnStatement();
		}
		if (match(Kind.IF)) {
			return ifStatement();
		}
		if (match(Kind.DO)) {
			return parseDoStmt();
		}
		if (match(Kind.EXIT)) {
			return exitStatement();
		}
		if (match(Kind.LOOP)) {
			return loopStatement();
		}
		if (match(Kind.FOR)) {
			return forStatement();
		}
		if (match(Kind.LPAREN)) {
			return multipleAssignment();
		}
		if (match(Kind.IMPORT)) {
			return importStatement();
		}
		if (match(Kind.RELEASE)) {
			return releaseStatement();
		}
		if (match(Kind.DEFER)) {
			return deferStatement();
		}
		
		return expressionStatement();
	}
	
	// printStatement ::= 'PRINT' expression?
	private Stmt printStatement() {
		final Token token = previous();		
		final List<Expr> expr = new ArrayList<Expr>();
		boolean eatRightParen = false;
		boolean canParseExp = true;
		
		if (match(Kind.LPAREN)) {
			eatRightParen = true;
			if (check(Kind.RPAREN)) {
				canParseExp = false;				
			}
		}
		
		if (canParseExp) {			
			do {
				expr.add(expression());
			} while (!isAtEnd() && match(Kind.COMMA));
		}
		if (eatRightParen) {			
			consume(Kind.RPAREN, "Expect ')' after expressions.");
		}
		
		consume(Kind.SEMICOLON, "Expect new line after `print()` statement.");
		
		return new Stmt.Print(token, expr);
	}
	
	// returnStatement ::= 'RETURN' expression
	private Stmt returnStatement() {
		final Token token = previous();
		Expr value = null;
		final List<Expr> expressionList = new ArrayList<>();
		if (!check(Kind.SEMICOLON)) {			
			do {
				value = expression();
				// check for if expression e.g: RETURN 15 if true else 20
				if (match(Kind.IF)) {
					value = parseIfExpression(value);
				}				
				expressionList.add(value);
			} while (!isAtEnd() && match(Kind.COMMA));
		}
		consume(Kind.SEMICOLON, "Expect new line after `return` statement.");
		
		return new Stmt.Return(token, expressionList);
	}
	
	private Stmt parseConstantDeclaration() {
		final Token token = previous();
		final Expr.Identifier name = new Expr.Identifier(consume(Kind.IDENTIFIER, "Expect constant name."));
		consume(Kind.SIMPLE_ASSIGN, "Expect `=` after constant name.");
		Expr value = expression();
		if (match(Kind.IF)) {
			value = parseIfExpression(value);
		}
		consume(Kind.SEMICOLON, "Expect new line after constant declaration.");
		return new Stmt.Const(token, name, value);
	}
	
	// variableStatement ::= 'LOCAL'|'PUBLIC' ('(' variableList ')' | assignment)
	private Stmt parseVariableDeclaration(Kind scope) {
		final Token token = previous();
		boolean allowInitializer = true;
		if (match(Kind.LPAREN)) {
			allowInitializer = false;			
		}
		
		final List<Stmt.VarDecl> declarations = variableDeclarationList(allowInitializer);
		final List<Expr> values = new ArrayList<>();
		
		if (!allowInitializer) {
			consume(Kind.RPAREN, "Expect `)` after variable declarations.");
			consume(Kind.SIMPLE_ASSIGN, "Expect `=` after variable declarations.");
			do {
				values.add(expression());
			} while(!isAtEnd() && match(Kind.COMMA));
		}
		
		consume(Kind.SEMICOLON, "Expect new line after variable declarations.");
		
		return new Stmt.Var(token, scope, declarations, values, (allowInitializer == false));
	}		
	
	// variableDeclarationList
	private List<Stmt.VarDecl> variableDeclarationList(boolean allowInitializer) {
		final List<Stmt.VarDecl> declarations = new ArrayList<Stmt.VarDecl>();
		do {
			declarations.add(variableDeclaration(allowInitializer));			
		} while (!isAtEnd() && match(Kind.COMMA));
		
		return declarations;
	}
	
	// variableDeclaration
	private Stmt.VarDecl variableDeclaration(boolean allowInitializer) {
		consume(Kind.IDENTIFIER, "Expect variable name.");		
		Expr.Identifier name = new Expr.Identifier(previous()); 
		Token bindType = null;
		Object defaultValue = null;
		Expr initializer = null;
		
		if (match(Kind.AS)) {
			bindType = consume(Kind.IDENTIFIER, "Expect variable type name.");
		}
		
		if (allowInitializer) {			
			if (match(Kind.SIMPLE_ASSIGN)) {
				initializer = expression();			
				// check for if expression e.g: LOCAL a = 10 if true else 20
				if (match(Kind.IF)) {
					initializer = parseIfExpression(initializer);
				}		
			} else if (bindType != null) {
				if (bindType.lexeme.toLowerCase().equals("string")) {					
					defaultValue = "";
				} else if (bindType.lexeme.toLowerCase().equals("number")) {
					defaultValue = 0.0;
				} else if (bindType.lexeme.toLowerCase().equals("boolean")) {
					defaultValue = false;
				} else {
					defaultValue = null;
				}
			}	
		}
		
		return new Stmt.VarDecl(name, initializer, defaultValue);
	}
	
	private Stmt ifStatement() {
		final Token token = previous();
		Expr condition = null;
		if (match(Kind.LPAREN)) {
			condition = expression();
			consume(Kind.RPAREN, "Expect ')' after condition.");
		} else {
			condition = expression();
		}
		match(Kind.THEN); // THEN token is optional.
		consume(Kind.SEMICOLON, "Expect new line before block.");
		
		// IfBranch
		final List<Stmt> consequence = new ArrayList<Stmt>();
		while (!isAtEnd() && !match(Kind.ENDIF, Kind.ELSE, Kind.EOF)) {
			consequence.add(declaration());			
		}
		
		// elseBranch
		final List<Stmt> alternative = new ArrayList<Stmt>();
		if (previous().kind == Kind.ELSE) {
			consume(Kind.SEMICOLON, "Expect new line in else branch.");
			while (!isAtEnd() && !match(Kind.ENDIF)) {
				alternative.add(declaration());
			}
		}
		consume(Kind.SEMICOLON, "Expect new line after if statement.");
		
		return new Stmt.If(token, condition, new Stmt.Block(token, consequence), new Stmt.Block(token, alternative));
	}
	
	// parseDoStmt
	private Stmt parseDoStmt() {
		if (match(Kind.CASE)) {
			return doCaseStatement();
		} else if (match(Kind.WHILE)) {
			return doWhileStatement();
		}
		return doStatement();
	}
	
	// doCaseStatement
	private Stmt doCaseStatement() {
		final Token token = previous();
		consume(Kind.SEMICOLON, "Expect new line after 'CASE'.");
		final List<Stmt.Case> branches = new ArrayList<>();
		Stmt.Block otherwise = null;
		
		while (!isAtEnd() && match(Kind.CASE)) {
			final Token caseToken = previous();
			final List<Expr> conditions = new ArrayList<>();
			do {
				conditions.add(expression());
			} while(!isAtEnd() && match(Kind.COMMA));
			consume(Kind.SEMICOLON, "Expect new line after case conditions.");

			// Statements
			final List<Stmt> statements = new ArrayList<>();
			final Token bodyToken = peek();
			while (!isAtEnd() && !check(Kind.CASE) && !check(Kind.OTHERWISE)) {
				statements.add(declaration());
			}
			Stmt.Block caseBlock = new Stmt.Block(bodyToken, statements);
			
			// Create and add the Case branch
			branches.add(new Stmt.Case(caseToken, conditions, caseBlock));
		}
		
		if (match(Kind.OTHERWISE)) {
			final Token otherwiseToken = previous();
			consume(Kind.SEMICOLON, "Expect new line after otherwise.");
			// Statements
			final List<Stmt> statements = new ArrayList<>();
			while (!isAtEnd() && !check(Kind.ENDCASE)) {
				statements.add(declaration());
			}
			otherwise = new Stmt.Block(otherwiseToken, statements);
		}
		
		consume(Kind.ENDCASE, "Expect keyword 'OTHERWISE'");
		consume(Kind.SEMICOLON, "Expect new line at the end of `DO CASE` statement.");
		
		return new Stmt.DoCase(token, branches, otherwise);
	}
	
	// doWhileStatement
	private Stmt doWhileStatement() {
		final Token token = previous();
		Expr condition = null;
		if (match(Kind.LPAREN)) {
			condition = expression();
			consume(Kind.RPAREN, "Expect ')' after 'WHILE' condition.");			
		} else {
			condition = expression();
		}
		consume(Kind.SEMICOLON, "Expect new line after 'WHILE' condition.");
		
		final List<Stmt> statements = new ArrayList<>();		
		while (!isAtEnd() && !match(Kind.ENDDO)) {
			statements.add(declaration());
		}
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return new Stmt.DoWhile(token, condition, new Stmt.Block(token, statements));
	}
	
	private Stmt doStatement() {
		final Token token = previous();
		Expr condition = null;
		
		consume(Kind.SEMICOLON, "Expect new line after 'DO' keyword.");
		
		final List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd() && !match(Kind.WHILE)) {
			statements.add(statement());
		}
		
		if (match(Kind.LPAREN)) {
			condition = expression();
			consume(Kind.RPAREN, "Expect ')' after 'WHILE' condition.");			
		} else {
			condition = expression();
		}
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return new Stmt.Do(token, condition, new Stmt.Block(token, statements));
	}
	
	// exitStatement
	private Stmt exitStatement() {
		Stmt.Exit exit = new Stmt.Exit(previous());
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return exit;
	}
	
	// loopStatement
	private Stmt loopStatement() {
		Stmt.Loop loop = new Stmt.Loop(previous());
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return loop;
	}
	
	// forStatement
	private Stmt forStatement() {
		final Token token = previous();
		final Expr.Identifier identifier = new Expr.Identifier(consume(Kind.IDENTIFIER, "Expect variable name."));
		consume(Kind.SIMPLE_ASSIGN, "Expect '=' after variable name.");
		
		final Expr initialValue = expression();
		consume(Kind.TO, "Expect keyword 'TO'");
		
		final Expr finalValue = expression();
		Expr increment = null;
		
		if (match(Kind.STEP)) {
			increment = expression();
		} //else {
			//increment = new Expr.Literal(new Token(TokenType.NUMBER, "1"), 1.0);
		//}
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		final List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd() && !match(Kind.ENDFOR)) {
			statements.add(declaration());
		}		
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return new Stmt.For(token, identifier, initialValue, finalValue, increment, new Stmt.Block(token, statements));
	}
	
	// functionStatement
	private Stmt parseFunctionDeclaration() {
		final Token token = previous();
		final Expr.Identifier name = new Expr.Identifier(consume(Kind.IDENTIFIER, "Expect function name."));		
		List<Expr.NamedExp> parameters = null;
		final List<Stmt> deferList = new ArrayList<>();
		
		if (match(Kind.LPAREN)) {
			if (!check(Kind.RPAREN))
				parameters = parseFunctionParameters();
			consume(Kind.RPAREN, "Expect ')' after parameters.");			
		}
		
		consume(Kind.SEMICOLON, "Expect new line.");		
		final List<Stmt> statements = new ArrayList<>();	
		while (!isAtEnd() && !match(Kind.ENDFUNC)) {
			Stmt declaration = declaration();
			if (declaration instanceof Stmt.Defer) {
				deferList.add(declaration);
			} else {				
				statements.add(declaration);
			}
		}
		
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return new Stmt.Function(token, name, parameters, statements, deferList);
	}
	
	private Stmt parseModuleDeclaration() {
		final Token token = previous();
		final Token name = consume(Kind.IDENTIFIER, "Expect module name.");
		consume(Kind.SEMICOLON, "Expect new line after module name.");
		
		final List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd() && !match(Kind.ENDMODULE)) {
			statements.add(declaration());
		}
		consume(Kind.SEMICOLON, "Expect new line.");
		
		return new Stmt.Module(token, name, statements);
	}
	
	private List<Expr.NamedExp> parseFunctionParameters() {
		final List <Expr.NamedExp> namedParameterList = new ArrayList<>();		
		do {
			namedParameterList.add(parseNamedExp(true));
		} while(!isAtEnd() && match(Kind.COMMA));
		
		return namedParameterList;
	}
	
	private Expr.NamedExp parseNamedExp(boolean parseParameters) {
		Token token = null;
		String alias = "";
		Expr value = null;

		if (parseParameters) {
			token = consume(Kind.IDENTIFIER, "Expect parameter name.");
			
			if (match(Kind.AS)) {
				alias = consume(Kind.IDENTIFIER, "Expect alias name for parameter: " + token.lexeme).lexeme;
			}
			
			if (match(Kind.SIMPLE_ASSIGN)) {
				value = expression();
			}			
		} else { // parse expression for call node. e.g foo(name: "John"), foo("John")
			if (peek().kind == Kind.IDENTIFIER && peekNext().kind == Kind.COLON) { // NamedExp
				token = consume(Kind.IDENTIFIER, "Expect argument name.");
				alias = token.lexeme;
				consume(Kind.COLON, "Expect `:` after argument name.");
				value = expression();
			} else { // normal expression
				value = expression();				
				alias = "_";
				token = value.token;
			}
		}
		
		return new Expr.NamedExp(token, alias, value);
	}
		
	// expressionStatement ::= expression
	private Stmt expressionStatement() {
		Expr expr = expression();
		
		if (match(Kind.SIMPLE_ASSIGN)) {
			final Token token = previous();
			final Expr value = expression();
			consume(Kind.SEMICOLON, "Expect new line after expression.");
			return new Stmt.SimpleAssignment(token, checkValidAssignment(expr, "Invalid left-hand side in assignment expression."), value);
		} else if (match(Kind.COMPLEX_ASSIGN)) {
			final Token token = previous();
			final Expr value = expression();
			consume(Kind.SEMICOLON, "Expect new line after expression.");
			
			return new Stmt.ComplexAssignment(token, checkValidAssignment(expr, "Invalid left-hand side in assignment expression."), value);
		}
		consume(Kind.SEMICOLON, "Expect new line after expression.");
		return new Stmt.Expression(expr.token, expr);
	}
	
	private Stmt importStatement() {
		final Token token = previous();
		final Token name = consume(Kind.IDENTIFIER, "Expect import name.");
		consume(Kind.SEMICOLON, "Expect new line after name.");
		
		// parse the file
		String fileName = "C:\\Users\\irwin.SUBIFOR\\eclipse-2022-06\\FoxDream\\" + name.lexeme + ".ybase";
		File fileHandle = new File(fileName);
		if (!fileHandle.exists()) {
			error(token, "Invalid module path or file name.");
		}
		
		// Extract source code from file.
		byte[] bytes;
		String sourceCode = null;
		try {
			bytes = Files.readAllBytes(Paths.get(fileName));
			sourceCode = new String(bytes, Charset.defaultCharset());
		} catch (IOException e) {
			error(token, e.getMessage());
		}
		sourceCode = "module " + name.lexeme + "\n" + sourceCode + "\n" + " endmodule"; 
		
		List<Stmt> statements = new Parser().parse(sourceCode);
		if (statements == null)
			error(token, "Something went wrong in the parsing proccess.");
		
		return statements.get(0);
	}
	
	private Stmt releaseStatement() {
		final Token token = previous();
		final List<Expr> elements = new ArrayList<>();
		
		do {			
			Expr exp = expression();
			if (!(exp instanceof Expr.Identifier)) {				
				error(previous(), "Invalid RELEASE expression.");
			}
			if (match(Kind.IF)) {
				exp = parseIfExpression(exp);
			}

			elements.add(exp);
		} while (!isAtEnd() && match(Kind.COMMA));
		
		consume(Kind.SEMICOLON, "Expect new line after 'RELEASE' expression.");
		
		return new Stmt.Release(token, elements);
	}
	
	private Stmt deferStatement() {
		final Token token = previous();
		consume(Kind.SEMICOLON, "Expect new line after 'DEFER'");
		final List<Stmt> statements = new ArrayList<>();
		
		while (!isAtEnd() && !match(Kind.ENDDEFER)) {
			statements.add(declaration());
		}		
		consume(Kind.SEMICOLON, "Expect new line");
		
		return new Stmt.Defer(token, statements);
	}
	
	// multipleAssignment. e.g: (foo, bar) = "Baz", "Faz"
	private Stmt multipleAssignment() {
		final Token token = previous();
		
		final List<Expr> leftElements = new ArrayList<>();
		final List<Expr> values = new ArrayList<>();
		
		do {
			leftElements.add(checkValidAssignment(expression(), "Invalid left-hand side in assignment expression."));
		} while (!isAtEnd() && match(Kind.COMMA));
		
		consume(Kind.RPAREN, "Expect `)` after variable name.");		
		consume(Kind.SIMPLE_ASSIGN, "Expect `=` after variable name.");
		
		do {
			values.add(expression());
		} while (!isAtEnd() && match(Kind.COMMA));
		
		consume(Kind.SEMICOLON, "Expect new line after expression.");
		
		return new Stmt.MultipleAssignment(token, leftElements, values);		
	}
	
	// expression ::= logicalOr
	private Expr expression() {
		return logicalOr();
	}
		
	private Expr checkValidAssignment(Expr node, String msg) {
		if (node instanceof Expr.Identifier || node instanceof Expr.Member) {
			return node;
		}
		throw error(node.token, msg);
	}
	
	private Expr parseIfExpression(Expr consequence) {
		final Token token = previous();
		final Expr condition = expression();
		Expr alternative = null;
		if (match(Kind.ELSE)) {
			alternative = expression();
		}
		return new Expr.IfExpr(token, condition, consequence, alternative);		
	}
	
	// logicalOr ::= logicalAnd ('or' logicalAnd)*
	private Expr logicalOr() {
		Expr left = logicalAnd();
		
		while (!isAtEnd() && match(Kind.LOGICAL_OR)) {
			Token operator = previous();
			Expr right = logicalAnd();
			left = new Expr.Logical(left.token, left, operator, right);
		}
		
		return left;
	}
	
	// logicalAnd ::= equality ('and' equality);
	private Expr logicalAnd() {
		Expr left = equality();
		
		while (!isAtEnd() && match(Kind.LOGICAL_AND)) {
			Token operator = previous();
			Expr right = equality();
			left = new Expr.Logical(left.token, left, operator, right);
		}
		
		return left;
	}
	
	// equality ::= comparison ('=='|'!=' comparison)* ;
	private Expr equality() {
		Expr left = comparison();
		
		while (!isAtEnd() && match(Kind.EQUALITY_OPERATOR)) {
			final Token token = previous();
			final Expr right = comparison();			
			left = new Expr.Binary(token, left, right);
		}
		return left;
	}
	
	// comparison ::= term ('<'|'<='|'>'|'>=' term)*
	private Expr comparison() {
		Expr left = term();
		
		while (!isAtEnd() && match(Kind.RELATIONAL_OPERATOR)) {
			final Token token = previous();
			final Expr right = term();
			left = new Expr.Binary(token, left, right);
		}
		
		return left;
	}
	
	// term ::= factor ('+'|'-' factor)*
	private Expr term() {
		Expr left = factor();
		
		while (!isAtEnd() && match(Kind.TERM_OPERATOR)) {
			final Token token = previous();
			final Expr right = factor();
			left = new Expr.Binary(token, left, right);
		}
		
		return left;
	}
	
	// factor ::= unary ('*'|'/' unary)*
	private Expr factor() {
		Expr left = unary();
		
		while (!isAtEnd() && match(Kind.FACTOR_OPERATOR)) {
			final Token token = previous();
			final Expr right = unary();
			left = new Expr.Binary(token, left, right);
		}
		
		return left;
	}
	
	// unary ::= ('+'|'-'|'!' unary)* | leftHandSide
	private Expr unary() {
		if (match(Kind.TERM_OPERATOR, Kind.LOGICAL_NOT)) {
			return new Expr.Unary(previous(), unary());
		}
		return callMemberExpression();
	}
	
	// callMemberExpression ::= memberExpression | callExpression
	private Expr callMemberExpression() {
		final Expr member = memberExpression();
		
		if (check(Kind.LPAREN)) {
			return callExpression(member);
		}
		
		return member;
	}
	
	// memberExpression ::= primary | ('.' identifier)* | ('[' expression ']')*
	// pepe.juan = 10
	//  
	private Expr memberExpression() {
		Expr parentObject = primary();
		
		while (!isAtEnd() && match(Kind.DOT, Kind.LBRACKET)) {
			// final Token token = peek();
			if (previous().kind == Kind.DOT) {
				final Expr property = new Expr.Identifier(consume(Kind.IDENTIFIER, "Expect property name."));
				parentObject = new Expr.Member(previous(), false, parentObject, property);
			} else { // LBRACKET
				final Expr property = expression();
				parentObject = new Expr.Member(previous(), true, parentObject, property);
			}
		}		
		return parentObject;
	}
	
	// callExpression ::= callExpression ('(' callExpression ')')*
	private Expr callExpression(Expr callee) {
		Expr callExpression = new Expr.Call(callee.token, callee, namedExpList());
		
		if (match(Kind.LPAREN)) {
			callExpression = callExpression(callExpression);
		}
		
		return callExpression;		
	}
	
	// primary ::= literal | thisExp | createObject | identifier | grouped
	private Expr primary() {
		if (peek().category == Category.LITERAL) {
			return new Expr.Literal(advance());
		}
		if (peek().category == Category.IDENTIFIER) {
			return new Expr.Identifier(advance());
		}
		if (match(Kind.CREATEOBJECT)) {
			return createObject();
		}
		if (match(Kind.LPAREN)) {
			return groupedExpression();
		}
		throw error(peek(), "Unexpected primary expression " + peek());
	}
	
	private Expr groupedExpression() {
		Expr expression = null;
		if (!match(Kind.RPAREN)) {			
			expression = expression();
			consume(Kind.RPAREN, "Expected ')' after expression.");
		}
		return expression;
	}

	// namedExpList
	private List<Expr.NamedExp> namedExpList() {
		consume(Kind.LPAREN, "Expect '(' before expression.");		
		final List<Expr.NamedExp> namedExpList = new ArrayList<Expr.NamedExp>();
		if (!check(Kind.RPAREN)) {
			do {
				namedExpList.add(parseNamedExp(false));
			} while (!isAtEnd() && match(Kind.COMMA));			
		}
		consume(Kind.RPAREN, "Expect ')' after expression.");
		
		return namedExpList;		
	}	
	
	private Expr createObject() {
		final Token token = previous();
		final List<Expr> arguments = new ArrayList<>();
		consume(Kind.LPAREN, "Expect '(' after 'CREATEOBJECT'");
		final Token name = consume(Kind.STRING, "Expect object name.");
		
		while (!isAtEnd() && match(Kind.COMMA)) {
			arguments.add(expression());
		}
		consume(Kind.RPAREN, "Expect ')'");		
		
		return new Expr.CreateObject(token, name, arguments);
	}
	/************************************************************
	 * HELPER FUNCTIONS
	 ************************************************************/
	
	private boolean match(Kind... types) {
		for (Kind type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}
	
	private Token consume(Kind expected, String msg) {
		if (check(expected)) return advance();
		
		throw error(peek(), msg);
	}	

	private boolean check(Kind k) {
		if (isAtEnd()) return false;
		return peek().kind == k;
	}

	// Get the next token and returns the previous one.
	private Token advance() {
		if (!isAtEnd()) current++;
		return previous();
	}
	
	// Whether we still have more tokens?
	private boolean isAtEnd() {
		return peek().kind == Kind.EOF;
	}	
	
	private Token peek() {
		return tokens.get(current);
	}
	
	private Token peekNext() {
		if (current+1 > tokens.size()) {
			return new Token(Kind.EOF);
		}
		return tokens.get(current+1);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}
	
	// throw a ParseError exception and stops the parsing proccess.
	private ParseError error(Token token, String message) {
		FoxDream.error(token, message);
		return new ParseError();
	}
	
	// Stabilizes the parser when a bad token is found.
	private void synchronize() {
		advance();
				
		while (!isAtEnd()) {
			if (previous().kind == Kind.SEMICOLON) return;
			
			switch (peek().kind) {
			case CLASS:
			case FUNCTION:
			case LOCAL:
			case PUBLIC:
			case FOR:
			case IF:
			case WHILE:
			case PRINT:
			case RETURN:			
				return;
			default:
				break;
			}
			advance();
		}
	}
}
