package core;

import java.util.List;
import java.util.ArrayList;

public class RuntimeArray implements CallableObject {
	final List<Object> elements = new ArrayList<>();
	private static final String INVALID_ARGUMENT_NEED_INT = "Invalid argument type for this function, expecting integer.";
	Expr callee;
	
	@Override
	public String toString() {
		if (elements.size() > 0) {			
			String[] str = new String[elements.size()];											
			for (int i = 0; i < elements.size(); i++) {
				Object item = elements.get(i);
				str[i] = Interpreter.stringify(item);
			}			
			return "[" + String.join(", ", str) + "]";
		}
		return "[]";
	}

	@Override
	public int arity() {
		return elements.size();
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
	public Object call(Interpreter interpreter, ArgValue[] arguments) {		
		switch (((Expr.Member)callee).property.token.lexeme.toLowerCase()) {
		case "add":
			return addElement(arguments);
		case "remove":
			return remove(arguments);
		case "contains":
			return contains(arguments);
		case "get":
			return get(arguments);
		case "len":
			return len(arguments);
		case "indexof":
			return indexOf(arguments);
		case "set":
			return set(arguments);
		default:
			throw new RuntimeError(callee.token, "Function not defined for this data type.");			
		}
		
	}

	@Override
	public boolean validateArguments() {
		return false;
	}
	
	private Object addElement(ArgValue[] arguments) {
		checkArgumentsArity(arguments, 1);
		return elements.add(arguments[0].value);		
	}
	
	private Object remove(ArgValue[] arguments) {
		checkArgumentsArity(arguments, 1);		
		return elements.remove(arguments[0].value);			
	}

	private Object contains(ArgValue[] arguments) {
		checkArgumentsArity(arguments, 1);		
		return elements.contains(arguments[0].value);			
	}

	private Object get(ArgValue[] arguments) {
		checkArgumentsArity(arguments, 1);
		if (!(arguments[0].value instanceof Double))
			error(INVALID_ARGUMENT_NEED_INT);
		try {
			Double value = (Double)arguments[0].value;
			int i = value.intValue();			
			return elements.get(i);			
		} catch(Exception e) {
			error(e.getMessage());
		}
		return null;
	}

	private Object indexOf(ArgValue[] arguments) {
		checkArgumentsArity(arguments, 1);
		int index = elements.indexOf(arguments[0].value); 
		return (double)index;
	}
	
	private Object len(ArgValue[] arguments) {
		if (arguments != null && arguments.length > 0)
			error("Unexpected arguments.");			
		return elements.size(); 		
	}

	private Object set(ArgValue[] arguments) {
		checkArgumentsArity(arguments, 2);
		if (!(arguments[0].value instanceof Double))
			error(INVALID_ARGUMENT_NEED_INT);
		Double d = (Double)arguments[0].value;
		int i = d.intValue();
		return elements.set(i, arguments[1].value); 		
	}
	
	private void error(String msg) {
		throw new RuntimeError(callee.token, msg);
	}
	
	private void checkArgumentsArity(ArgValue[] arguments, int numberOfArgs) {
		if (arguments == null || arguments.length != numberOfArgs) {
			error(String.format("Wrong number of parameters/arguments. Expected: %s, got: %s.", numberOfArgs, arguments.length));			
		}		
	}
}
