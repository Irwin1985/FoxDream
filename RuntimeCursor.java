package core;

import java.sql.*;

public class RuntimeCursor implements CallableObject {
	Expr callee;
	final ResultSet cursor;
	
	public RuntimeCursor(ResultSet cursor) {
		this.cursor = cursor;
		try {			
			this.cursor.next();
		} catch (Exception e){
			error(e.getMessage());
		}
	}
	
	@Override
	public String toString() {
		return "Cursortostring... pendiente";
	}
	
	@Override
	public int arity() {
		return 0;
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
		return false;
	}

	@Override
	public Object call(Interpreter interpreter, ArgValue[] arguments) {
		switch (((Expr.Member)callee).property.token.lexeme.toLowerCase()) {
		case "close":
			return close();
		default:
			throw new RuntimeError(callee.token, "Function not defined for this data type.");			
		}
	}
	
	private Object close() {
		try {
			cursor.close();
			return true;
		} catch(Exception e) {
			error(e.getMessage());
		}
		return false;
	}
	
	private void error(String msg) {
		throw new RuntimeError(callee.token, msg);
	}	
	
}
