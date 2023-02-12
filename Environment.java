package core;

import java.util.Map;
import java.util.HashMap;

public class Environment {
	Map<String, Object> record;
	Environment parent;	
		
	public Environment() {
		this.record = new HashMap<String, Object>();
		this.parent = null;
	}	
	
	public Environment(Map<String, Object> record, Environment parent) {
		if (record == null) {
			this.record = new HashMap<String, Object>();			
		}
		this.parent = parent;
	}
	
	public Environment(Environment parent) {
		this.record = new HashMap<String, Object>();
		this.parent = parent;
	}
	
	// Define variable by name.
	public Object define(String name, Object value, VarType varType) {
		Object[] varPack = new Object[2];
		varPack[0] = varType;
		varPack[1] = value;
		record.put(name.toLowerCase(), varPack);
		return value;		
	}
	
	// Define variable by Token
	public Object define(Token name, Object value, VarType varType) {
		if (record.containsKey(name.lexeme.toLowerCase())) {
			Object[] varPack = (Object[])record.get(name.lexeme.toLowerCase());
			if (varPack[0] == VarType.CONSTANT) {
				throw new RuntimeError(name, "Constants cannot be redefined `" + name.lexeme + "`");
			}
		}
		return define(name.lexeme, value, varType);
	}
	
	public Object lookUp(Token name) {
		Environment env = resolve(name, true);
		Object[] varPack = (Object[])env.record.get(name.lexeme.toLowerCase());
		
		return varPack[1]; // return the value
	}
	
	public Object assign(Token name, Object value) {
		Environment env = resolve(name, false);
		if (env == null) {
			// define the variable in local environment.
			return define(name, value, VarType.VARIABLE);
		}
		
		// get the type and validate constant cannot be modified
		Object[] varPack = (Object[])env.record.get(name.lexeme.toLowerCase());
		if (varPack[0] == VarType.CONSTANT) {
			throw new RuntimeError(name, "Invalid constant assignment `" + name.lexeme + "`");
		}
		// Update the value
		varPack[1] = value;
		
		// Assign the new value
		env.record.put(name.lexeme.toLowerCase(), varPack);
		return value;
	}
	
	public Object localAssign(String name, Object value) {
		return record.put(name, value);
	}
	
	public Environment resolve(Token name, boolean throwException) {
		if (record.containsKey(name.lexeme.toLowerCase())) {
			return this;
		}
		if (parent != null) {
			return parent.resolve(name, throwException);
		}
		if (throwException)
			throw new RuntimeError(name, "Undefined variable `" + name.lexeme + "`.");
		return null;
	}
	
	public Object getVarPack(Token name) {
		Object[] varPack = (Object[])record.get(name.lexeme.toLowerCase());
		if (varPack[0] == VarType.CONSTANT) {
			throw new RuntimeError(name, "Invalid constant assignment `" + name.lexeme + "`");
		}		
		return varPack[1];
	}
	
	public void release(Token name) {
		Environment env = resolve(name, false);
		if (env != null) {
			env.record.remove(name.lexeme.toLowerCase());
		}		
	}
	
	@Override
	public String toString() {
		if (record.size() > 0) {
			String[] str = new String[record.size()];
			int i = 0;
			for (Map.Entry<String, Object> entry : record.entrySet()) {				
				Object value = ((Object[])entry.getValue())[1];
				str[i++] = entry.getKey() + ":" + Interpreter.stringify(value);
			}
			return "{" + String.join(", ", str) + "}";
		}
		return "{}";
	}
}
