package core;
import java.util.List;

@SuppressWarnings("serial")
public class DeferException extends RuntimeException {
	final Token token;
	final List<Stmt> statements;
	
	DeferException(Token token, List<Stmt> statements) {
		super(null, null, false, false);
		this.token = token;
		this.statements = statements;
	}
}
