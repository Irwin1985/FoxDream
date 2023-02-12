package core;

public class ArgValue {
	final Token name;
	final String alias;
	final Object value;
	
	public ArgValue(Token name, String alias, Object value) {
		this.name = name;
		this.alias = alias;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.format("(name: %s, alias: '%s', value: %s)", name.lexeme, alias, value);
	}
}
