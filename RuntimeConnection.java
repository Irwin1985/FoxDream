package core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RuntimeConnection extends Environment {
	public RuntimeConnection() {
		super();
		// Define properties
		define("provider", "", VarType.VARIABLE);
		define("server", "", VarType.VARIABLE);
		define("database", "", VarType.VARIABLE);
		define("user", "", VarType.VARIABLE);
		define("password", "", VarType.VARIABLE);
		define("port", "", VarType.VARIABLE);
		CallableConnection cc = new CallableConnection(this);
		// Define methods
		define("connect", cc, VarType.CONSTANT);
		define("disconnect", cc, VarType.CONSTANT);
		define("open", cc, VarType.CONSTANT);		
	}
	@Override
	public String toString() {
		return "Object(Connection)";
	}
	
	static class CallableConnection implements CallableObject {
		Expr callee;
		Environment parent;
		Connection connection = null;
		Statement statement = null;
		
		@Override
		public int arity() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		public CallableConnection(Environment parent) {
			this.parent = parent;
		}

		@Override
		public void setCallee(Expr callee) {
			this.callee = callee;			
		}

		@Override
		public String[] getParamInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean validateArguments() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object call(Interpreter interpreter, ArgValue[] arguments) {
			switch (((Expr.Member)callee).property.token.lexeme.toLowerCase()) {
			case "connect":
				return connect();
			case "disconnect":
				return disconnect();
			case "open":
				return open(arguments);
			default:
				throw new RuntimeError(callee.token, "Function not defined for this data type.");			
			}
		}
		
		private Object connect() {
			try {
				if (connection != null && !connection.isClosed()) {
					return true;
				}
			} catch (SQLException e) {				
				error(e.getMessage());
			}
			
			String provider = lookUp("provider").toString();
			String server = lookUp("server").toString();
			String user = lookUp("user").toString();
			String password = lookUp("password").toString();
			String database = lookUp("database").toString();
			String port = lookUp("port").toString();
			
			String url = String.format("jdbc:mysql://%s:%s/%s", server, port, database);
			try {
				connection = DriverManager.getConnection(url, user, password);
				return true;
			} catch (Exception e) {
				throw new RuntimeError(callee.token, e.getMessage());
			}
		}
		
		private Object open(ArgValue[] arguments) {
			try {
				if (connection == null || connection.isClosed()) {
					error("The connection object is not connected.");
				}
			} catch (SQLException e) {				
				error(e.getMessage());
			}
			
			String tableName = arguments[0].value.toString();
			String query = "select * from " + tableName;
			try {
				checkArgumentsArity(arguments, 1);
				statement = connection.createStatement();
				ResultSet cursor = statement.executeQuery(query);
				return new RuntimeCursor(cursor);
			} catch (SQLException e) {
				error(e.getMessage());
			}
			return null;
		}
		
		private Object disconnect() {
			try {
				connection.close();
				statement.close();
				return true;
			} catch(Exception e) {
				error(e.getMessage());
			}
			return false;
		}
		
		private Object lookUp(String property) {
			return ((Object[])parent.record.get(property))[1];
		}
		
		private void checkArgumentsArity(ArgValue[] arguments, int numberOfArgs) {
			if (arguments == null || arguments.length != numberOfArgs) {
				error(String.format("Wrong number of parameters/arguments. Expected: %s, got: %s.", numberOfArgs, arguments.length));			
			}		
		}	

		private void error(String msg) {
			throw new RuntimeError(callee.token, msg);
		}
	}
}
