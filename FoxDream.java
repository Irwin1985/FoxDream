package core;

import java.io.BufferedReader;
import java.util.List;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FoxDream {

	private static final Interpreter interpreter = new Interpreter();
	
	// Error flags
	static boolean hadError = false;
	static boolean hadRuntimeError = false;
	
	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: foxd fileName");
			System.exit(64);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}
	
	public static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		
		// Check for syntax and parsing grammar phase.
		if (hadError) 			System.exit(65);
		// Check for execution error.
		if (hadRuntimeError) 	System.exit(70);
	}
	
	public static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for (;;) { // start REPL
			System.out.print("fox-dream> ");
			String line = reader.readLine();
			if (line == null) break;
			run(line);
			// whether error or succed: we reset the error flag.
			hadError = false;
		}
	}
	
	public static void run(String source) {
		List<Stmt> statements = new Parser().parse(source);
		
		// stop execution if an error occurred.
		if (hadError || statements.isEmpty()) return;
		
		interpreter.interpret(statements);
	}
	
	static void error(int line, int col, String message) {
		report(line, col, "", message);
	}
	
	private static void report(int line, int col, String where, String message) {
		System.err.println(formatError("Parsing", line, col, where, message));
		hadError = true;
	}
	
	static void error(Token token, String message) {
		if (token.kind == Kind.EOF) {
			report(token.line, token.col, " at end", message);
		} else {
			report(token.line, token.col, token.lexeme, message);
		}
	}
	
	static void runtimeError(RuntimeError error) {
		System.err.println(formatError("Runtime", error.token.line, error.token.col, error.token.lexeme, error.getMessage()));
		hadRuntimeError = true;
	}
	
	static String formatError(String errorStr, int line, int col, String where, String msg) {
		return String.format("[%s:%s] - %s error near of `%s`: %s", line, col, errorStr, where, msg);
	}
}
