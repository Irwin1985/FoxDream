package core;

import java.util.HashMap;
import java.util.Map;

public class RuntimeFunction implements CallableObject {
	private final Stmt.Function declaration;
	private final Environment closure;
	Expr callee = null;
	
	public RuntimeFunction(Stmt.Function declaration, Environment closure) {
		this.declaration = declaration;
		this.closure = closure;
	}	
	
	@Override
	public int arity() {
		if (declaration.parameters != null) {
			return declaration.parameters.size();			
		} else {
			return 0;
		}
	}

	@Override
	public Object call(Interpreter interpreter, ArgValue[] arguments) {
		// a new fresh enclosed environment
		Environment environment = new Environment(closure);
		Object value = null;
		if (declaration.parameters != null && declaration.parameters.size() > 0) {
			// define all parameters
			Map<String, String> relation = new HashMap<>();
			for (Expr.NamedExp param : declaration.parameters) {
				if (param.alias.length() > 0) {					
					relation.put(param.alias.toLowerCase(), param.token.lexeme);
				} else {
					// there is no alias, so we register the same name.
					relation.put(param.token.lexeme.toLowerCase(), param.token.lexeme);
				}
				if (param.value != null) {
					value = interpreter.evaluate(param.value);
				}
				// register internal name as param.name (will be accessed through it alias).
				environment.define(param.token.lexeme, value, VarType.VARIABLE);
			}
			
			// now update parameters based on args data.
			if (arguments != null && arguments.length > 0) {
				ArgValue argValue = null;
				for (int i = 0; i < declaration.parameters.size(); i++) {
					Token token = declaration.parameters.get(i).token;
					if (arguments.length > i) {
						argValue = arguments[i];
						if (argValue.alias.equals("_")) { // normal call e.g: foo("bar")
							if (argValue.value != null) {								
								environment.assign(token, argValue.value);
							}
						} else { // named argument e.g: foo(name: "bar")
							if (relation.containsKey(argValue.alias.toLowerCase())) {
								environment.assign(token, argValue.value);
							} else {
								throw new RuntimeError(argValue.name, "Alias not found: `" + argValue.alias + "`");
							}
						}						
					}
				}
			}
		}
				
		// execute
		try {
			interpreter.executeBlock(declaration.statements, environment);
		} catch(ReturnException e) {
			if (declaration.deferList != null) {
				for (int i = declaration.deferList.size()-1; i >= 0; i--) {
					Stmt.Defer defer = (Stmt.Defer)declaration.deferList.get(i);
					interpreter.executeBlock(defer.statements, environment);										
				}
			}
			return e.value;
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return "fn(" + declaration.name.token.lexeme + ")";
	}

	@Override
	public void setCallee(Expr callee) {
		this.callee = callee;		
	}

	@Override
	public String[] getParamInfo() {
		return null;
	}

	@Override
	public boolean validateArguments() {
		return true;
	}
}
