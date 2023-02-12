package core;

public class Builtin {
	public static void loadBuiltinFunctions(Environment global) {
		// Constants
		global.define("_VERSION", "1.0", VarType.CONSTANT);
		global.define("_AUTHOR", "Irwin Rodriguez <rodriguez.irwin@gmail.com>", VarType.CONSTANT);
		
		// Database providers global constants
		global.define("_MYSQL", 1, VarType.CONSTANT);
		global.define("_MSSQL", 2, VarType.CONSTANT);
				
		global.define("empty", new Environment(), VarType.CONSTANT);		
		global.define("array", new RuntimeArray(), VarType.CONSTANT);
		global.define("connection", new RuntimeConnection(), VarType.CONSTANT);
		/**********************************************************************
		 * Alltrim
		 **********************************************************************/
		global.define("alltrim", new BuiltinFunction() {
			
			@Override
			public int arity() {
				return 1;
			}
			
			@Override
			public String[] getParamInfo() {
				String[] info = {"String"};
				return info;
			}

			@Override
			public Object call(Interpreter interpreter, ArgValue[] arguments) {
				Builtin.argumentChecker(callee.token, arguments, getParamInfo(), true);
				
				Object value = ((ArgValue)arguments[0]).value;				
				return value != null ? value.toString().trim() : null;
			}
			
		}, VarType.CONSTANT);
		/**********************************************************************
		 * Len
		 **********************************************************************/
		global.define("len", new BuiltinFunction() {
			
			@Override
			public int arity() {
				return 1;
			}
			
			@Override
			public String[] getParamInfo() {
				String[] info = {"String"};
				return info;
			}

			@Override
			public Object call(Interpreter interpreter, ArgValue[] arguments) {
				Builtin.argumentChecker(callee.token, arguments, getParamInfo(), false);
				
				Object value = ((ArgValue)arguments[0]).value;
				
				switch (value.getClass().getSimpleName()) {
				case "String":
					return ((String)value).length();
				}
				
				return 0;
			}
			
		}, VarType.CONSTANT);		
		/**********************************************************************
		 * Tick
		 **********************************************************************/
		global.define("tick", new BuiltinFunction() {
			
			@Override
			public int arity() {
				return 0;
			}
			
			@Override
			public String[] getParamInfo() {
				String[] info = {"String"};
				return info;
			}

			@Override
			public Object call(Interpreter interpreter, ArgValue[] arguments) {
				return (double)System.currentTimeMillis() / 1000.0;
			}
			
		}, VarType.CONSTANT);	
		/**********************************************************************
		 * Tack
		 **********************************************************************/
		global.define("tack", new BuiltinFunction() {
			
			@Override
			public int arity() {
				return 1;
			}
			
			@Override
			public String[] getParamInfo() {
				String[] info = {"Double"};
				return info;
			}

			@Override
			public Object call(Interpreter interpreter, ArgValue[] arguments) {
				Double tick = (double)System.currentTimeMillis() / 1000.0;
				return tick - (Double)arguments[0].value;
			}
			
		}, VarType.CONSTANT);		
	}
	/**
	 * BuiltinFunction
	 */
	static class BuiltinFunction implements CallableObject {
		Expr callee;
		
		@Override
		public int arity() {
			return 0;
		}

		@Override
		public Object call(Interpreter interpreter, ArgValue[] arguments) {
			return null;
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
	/**
	 * Argument Checker
	 */
	public static void argumentChecker(Token token, ArgValue[] arguments, String[] paramInfo, boolean validateTypes) {
		if (arguments == null || arguments.length == 0) {
			throw new RuntimeError(token, String.format("Wrong number of arguments, expected: %s, got: 0", paramInfo.length));
		}
		if (arguments.length != paramInfo.length) {
			throw new RuntimeError(token, String.format("Wrong number of arguments or parameters, expected: %s, got: 0", paramInfo.length, arguments.length));
		}
		if (!validateTypes)
			return;
		
		// Check the type or arguments
		for (int i = 0; i < arguments.length; i++) {
			ArgValue argValue = arguments[i];
			Object val = argValue.value;
			String argType = "";
			if (val == null) {
				argType = "null";
			} else {				
				argType = val.getClass().getSimpleName(); 
			}
			if (!argType.contains(paramInfo[i])) {
				throw new RuntimeError(argValue.name, String.format("Wrong argument type, expected: %s, got: %s", paramInfo[i], argType));
			}
		}
	}
}
