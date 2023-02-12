package core;

import java.util.List;
import java.util.ArrayList;

import core.Expr.IfExpr;
import core.Expr.Macro;
import core.Expr.NamedExp;
import core.Stmt.Block;
import core.Stmt.Class;
import core.Stmt.ComplexAssignment;
import core.Stmt.Const;
import core.Stmt.Defer;
import core.Stmt.Do;
import core.Stmt.DoCase;
import core.Stmt.DoWhile;
import core.Stmt.Exit;
import core.Stmt.For;
import core.Stmt.Function;
import core.Stmt.If;
import core.Stmt.Loop;
import core.Stmt.Module;
import core.Stmt.Release;
import core.Stmt.Return;
import core.Stmt.SimpleAssignment;
import core.Stmt.Var;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {		
	final Environment globals = new Environment();	
	private Environment environment = globals; // our local env starts being the global env.
	
	public Interpreter() {
		// Install the global variables
		Builtin.loadBuiltinFunctions(globals);
		
		// Start with global environment
		environment = globals;
	}
	/************************************************************
	 * FUNCTION HELPERS
	 ************************************************************/	
	void interpret(List<Stmt> statements) {
		try {
			for (Stmt stmt : statements) {
				execute(stmt);
			}
		} catch(RuntimeError error) {
			FoxDream.runtimeError(error);
		} catch(ReturnException error) {
			// return value is discarded in top level programs.
		}
	}
	
	void execute(Stmt stmt) {
		stmt.accept(this);
	}
	
	Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	public static String stringify(Object object) {
		if (object == null) return "null";
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		
		return object.toString();
	}
	
	// converts to double format e.g: true -> "1"
	private String doubleFormat(Object object) {
		if (object == null) return "0";
		if (object instanceof Double) {
			return object.toString();
		}
		if (object instanceof Boolean) {
			return (Boolean)object ? "1" : "0";
		}
		return object.toString();
	}
	
	private Object lookUpVariable(Token name) {
		return environment.lookUp(name);
	}
	
	private void multipleAssignment(Kind scope, Token token, List<Expr> listValues, List<Expr> listElements) {
		List<Object> values = new ArrayList<>();
		for (Expr value : listValues) {
			Object result = evaluate(value);
			if (result instanceof List) {
				for (Object res : (List<?>)result) {
					values.add(res);
				}
			} else {				
				values.add(result);
			}
		}
		
		int elementSize = listElements.size();
		int valueSize = values.size();
		
		if (elementSize != valueSize) {
			if (elementSize < valueSize)
				throw new RuntimeError(token, String.format("Wrong number of variables, expected: %s, got: %s", elementSize, valueSize));
			else
				throw new RuntimeError(token, String.format("Wrong number of values, expected: %s, got: %s", elementSize, valueSize));
		}
		
		for (int i=0; i<values.size(); i++) {
			Token tok = listElements.get(i).token;
			if (tok.lexeme.equals("_")) { // discard value
				continue;
			}
			if (scope == Kind.LOCAL) {				
				environment.assign(tok, values.get(i));
			} else {
				globals.assign(tok, values.get(i));
			}
		}		
	}
		
	void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			
			for (Stmt stmt : statements) {
				execute(stmt);
			}
		} 
		finally {
			this.environment = previous;
		}
	}
	
	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}
	
	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) return;
		throw new RuntimeError(operator, "Operands must be a number.");
	}
	
	private boolean isTruthy(Object object) {
		if (object == null) return false;
		if (object instanceof Boolean) return (Boolean)object;
		return true;
	}
	
	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null) return false;
		return a.equals(b);
	}
	
	private Environment resolveMemberEnvironment(Expr.Member member) {
		Object result = evaluate(member.parentObject);
				
		if (result == null) {
			throw new RuntimeError(member.parentObject.token, "Invalid member.");
		}
		
		while (!(result instanceof Environment)) {
			result = evaluate((Expr)result);
		}
		
		return (Environment)result;
	}

	/************************************************************
	 * VISITOR IMPLEMENTATION
	 ************************************************************/		
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		for (Expr e : stmt.expressionList) {			
			System.out.println(stringify(evaluate(e)));
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.token.literal;
	}

	@Override
	public Void visitMultipleAssignmentStmt(Stmt.MultipleAssignment stmt) {
		multipleAssignment(Kind.LOCAL, stmt.token, stmt.values, stmt.leftElements);
		return null;
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);		
		if (expr.operator.kind == Kind.LOGICAL_OR) {
			if (isTruthy(left)) return left;
		} else {
			if (!isTruthy(left)) return left;
		}
		return evaluate(expr.right);
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		
		switch (expr.token.category) {
		case NOT_EQ:
			return !isEqual(left, right);
		case EQUAL:
			return isEqual(left, right);
		case GREATER:
			checkNumberOperands(expr.token, left, right);
			return (double)left > (double)right;
		case GREATER_EQ:
			checkNumberOperands(expr.token, left, right);
			return (double)left >= (double)right;
		case LESS:
			checkNumberOperands(expr.token, left, right);
			return (double)left < (double)right;
		case LESS_EQ:
			checkNumberOperands(expr.token, left, right);
			return (double)left <= (double)right;
		case MINUS:
			checkNumberOperands(expr.token, left, right);
			return (double)left - (double)right;
		case MUL:
			checkNumberOperands(expr.token, left, right);
			return (double)left * (double)right;
		case DIV:
			checkNumberOperands(expr.token, left, right);
			if ((double)right == 0) {
				throw new RuntimeError(expr.token, "Division by zero.");
			}
			return (double)left / (double)right;			
		case PLUS:
			if (left instanceof String) {
				return (String)left + stringify(right);
			}
			if (left instanceof Double) {
				if (right instanceof Double)
					return (double)left + (double)right;
				try {
					Double rhs = Double.valueOf(doubleFormat(right));
					return (Double)left + rhs;
				} catch (Exception e) {
					return left;
				}
			}
		default:
			throw new RuntimeError(expr.token, "Incompatible types.");
		}		
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);
		
		switch (expr.token.category) {
		case BANG:
			return !isTruthy(right);
		case MINUS:
			checkNumberOperand(expr.token, right);
			return -(Double)right;
		case PLUS:
			checkNumberOperand(expr.token, right);
			return right;
		default:
			throw new RuntimeError(expr.token, "Incompatible types.");
		}
	}

	@Override
	public Object visitMemberExpr(Expr.Member expr) {
		Object left = evaluate(expr.parentObject);			
		if (!expr.computed) {			
			if (left instanceof Environment || left instanceof RuntimeCursor) {
				if (left instanceof Environment)
					return ((Environment)left).lookUp(expr.property.token);
				else
					try {
						return ((RuntimeCursor)left).cursor.getObject(expr.property.token.lexeme);						
					} catch(Exception e) {
						throw new RuntimeError(expr.token, e.getMessage());
					}
			}
		}
		return left;
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		final Object callee = evaluate(expr.callee);
		
		if (!(callee instanceof CallableObject)) {
			throw new RuntimeError(expr.token, "Not a function: " + expr.callee.token.lexeme);
		} 
		// pepe() <- "pepe"
		// juan.luis() <- "luis"
		
		CallableObject callable = (CallableObject)callee;		
		
		if (callable.validateArguments() && callable.arity() != expr.arguments.size()) {
			throw new RuntimeError(expr.callee.token, String.format("Wrong number of parameters/arguments. Expected: %s, got: %s.", callable.arity(), expr.arguments.size()));
		}
		
		// Evaluate arguments		
		if (expr.arguments.size() > 0) {
			int argLen = expr.arguments.size();
			final ArgValue[] arguments = new ArgValue[argLen];
			Object value = null;
			
			for (int i = 0; i < argLen; i++) {
				Expr.NamedExp arg = expr.arguments.get(i);
				if (arg.value instanceof Expr.Identifier && ((Expr.Identifier)arg.value).token.lexeme.equals("_")) {
					value = null;
				} else {					
					value = evaluate(arg.value);
				}
				arguments[i] = new ArgValue(arg.token, arg.alias, value);
			}
			callable.setCallee(expr.callee);
			return callable.call(this, arguments);
		}
		callable.setCallee(expr.callee);
		return callable.call(this, null);
	}

	@Override
	public Object visitCreateObjectExpr(Expr.CreateObject expr) {		
		Object obj = lookUpVariable(expr.name);
		
		if (obj instanceof RuntimeArray) {
			RuntimeArray array = (RuntimeArray)obj;
			if (expr.arguments != null && expr.arguments.size() > 0) {
				for (Expr exp : expr.arguments) {
					array.elements.add(evaluate(exp));
				}
			}
			return array;
		}
		return obj;
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdentifierExpr(Expr.Identifier expr) {
		return lookUpVariable(expr.token);
	}	

	@Override
	public Void visitReturnStmt(Return stmt) {
		Object value = null;
		int size = stmt.expressions.size();
		if (size == 1) {
			value = evaluate(stmt.expressions.get(0));
		} else if (size > 1) {			
			List<Object> valueList = new ArrayList<>();
			for (Expr e : stmt.expressions) {
				valueList.add(evaluate(e));				
			}
			value = valueList;
		}
		
		throw new ReturnException(value);
	}
	@Override
	public Void visitVarStmt(Var stmt) {
		if (stmt.isMultipleAssign) {
			// cast VarDecl to Identifier.
			List<Expr> listElements = new ArrayList<>();
			for (Stmt.VarDecl v : stmt.declarations) {
				listElements.add(v.name);
			}
			multipleAssignment(stmt.scope, stmt.token, stmt.values, listElements);			
		} else {
			Object initializer = null;
			for (Stmt.VarDecl v : stmt.declarations) {
				if (v.initializer != null) {
					initializer = evaluate(v.initializer);
				} else {
					initializer = v.defaultValue;
				}
				if (stmt.scope == Kind.LOCAL) {
					environment.define(v.name.token.lexeme, initializer, VarType.VARIABLE);
				} else {
					globals.define(v.name.token.lexeme, initializer, VarType.VARIABLE);
				}
			}
		}
		return null;
	}
	@Override
	public Void visitBlockStmt(Block stmt) {
		if (!stmt.statements.isEmpty()) {			
			executeBlock(stmt.statements, new Environment(environment));
		}
		return null;
	}
	@Override
	public Void visitIfStmt(If stmt) {
		Object condition = evaluate(stmt.condition);
		if (isTruthy(condition)) {
			execute(stmt.thenBranch);
		} else {
			if (stmt.elseBranch != null && stmt.elseBranch.statements.size() > 0) {
				execute(stmt.elseBranch);
			}
		}
		return null;
	}
	@Override
	public Void visitDoCaseStmt(DoCase stmt) {
		Object condition = null;
		for (Stmt.Case c : stmt.branches) {
			for (Expr e : c.conditions) {
				condition = evaluate(e);
				if (isTruthy(condition)) {
					execute(c.body);
					return null;
				}
			}
		}
		if (stmt.otherwise != null) {
			execute(stmt.otherwise);
		}
		return null;
	}
	@Override
	public Void visitDoWhileStmt(DoWhile stmt) {
		Object condition = null;
		while (true) {
			try {
				condition = evaluate(stmt.condition);				
				if (isTruthy(condition)) {
					execute(stmt.block);
				} else {
					break;
				}
			} catch(LoopException e) {
				continue;
			} catch(ExitException e) {
				break;
			}
		}
		
		return null;
	}
	@Override
	public Void visitDoStmt(Do stmt) {
		Object condition = null;
		while (true) {
			try {
				execute(stmt.block);
				condition = evaluate(stmt.condition);
				if (!isTruthy(condition)) {
					break;
				}				
			} catch(LoopException e) {
				continue;
			} catch(ExitException e) {
				break;
			}
		}
		return null;
	}
	@Override
	public Void visitExitStmt(Exit stmt) {
		throw new ExitException();
	}
	@Override
	public Void visitLoopStmt(Loop stmt) {
		throw new LoopException();
	}
	@Override
	public Void visitForStmt(For stmt) {
		Environment forEnv = new Environment(environment);
		
		Object initialValue = evaluate(stmt.initialValue);
		forEnv.define(stmt.identifier.token.lexeme, initialValue, VarType.VARIABLE);
		
		Object finalValue = evaluate(stmt.finalValue);
		Object increment = null;
		
		if (!(initialValue instanceof Double) || !(finalValue instanceof Double)) {
			throw new RuntimeError(stmt.token, "Incompatible types in 'FOR' statement.");
		}
			
		if (stmt.increment != null) {
			increment = evaluate(stmt.increment);
			if (!(increment instanceof Double)) {
				throw new RuntimeError(stmt.token, "Invalid type for incrementer.");
			}
		} else {
			increment = 1.0;
		}
		
		Double start = (Double)initialValue;
		Double end = (Double)finalValue;		
		Double inc = (Double)increment;
		
		if ((inc > 0 && start > end) || (inc < 0 && start < end)) {
			return null;
		}
		
		// execute the for statement block		
		while (true) {
			try {
				executeBlock(stmt.block.statements, forEnv);
				start = (Double)forEnv.lookUp(stmt.identifier.token) + (Double)increment;
				forEnv.assign(stmt.identifier.token, start);
				if ((inc > 0 && start > end) || (inc < 0 && start < end)) {
					break;
				}
			} catch(LoopException e) {
				continue;
			} catch(ExitException e) {
				break;
			}			
		}
		
		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Function stmt) {
		RuntimeFunction function = new RuntimeFunction(stmt, environment);
		environment.define(stmt.name.token, function, VarType.CONSTANT);
		return null;
	}
	
	@Override
	public Void visitClassStmt(Class stmt) {
		Object superclass = null;
		if (stmt.superClass != null) {
			superclass = evaluate(stmt.superClass);
			if (!(superclass instanceof RuntimeClass)) {
				throw new RuntimeError(stmt.token, "Super class must be a class.");
			}
		}
		RuntimeClass rtClass = new RuntimeClass(stmt);
		environment.define(stmt.name.token, rtClass, VarType.CONSTANT);
		return null;
	}
	
	@Override
	public Object visitNamedExpr(NamedExp expr) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object visitIfExpr(IfExpr expr) {
		Object condition = evaluate(expr.condition);
		if (isTruthy(condition)) {
			return evaluate(expr.consequence);
		}
		if (expr.alternative != null) {
			return evaluate(expr.alternative);
		}
		return null;
	}
	@Override
	public Void visitSimpleAssignment(SimpleAssignment stmt) {
		Object value = evaluate(stmt.value);
		
		if (stmt.left instanceof Expr.Identifier) {			
			environment.assign(stmt.left.token, value);
		} else if (stmt.left instanceof Expr.Member) {
			Expr.Member member = (Expr.Member)stmt.left;
			Environment env = resolveMemberEnvironment(member);
			env.assign(member.property.token, value);
		}
		return null;
	}
	@Override
	public Void visitConstantStmt(Const stmt) {
		Object value = evaluate(stmt.value);
		environment.define(stmt.name.token, value, VarType.CONSTANT);
		return null;
	}
	@Override
	public Void visitComplexAssignment(ComplexAssignment stmt) {
		Object right = evaluate(stmt.value);
		final Token token = stmt.left.token;
		Environment env = environment.resolve(token, true);				
		Object left = env.getVarPack(token);
		
		switch (stmt.token.category) {
		case MINUS:
			checkNumberOperands(token, left, right);
			env.assign(token, (double)left - (double)right);
			break;
		case MUL:
			checkNumberOperands(token, left, right);
			env.assign(token, (double)left * (double)right);
			break;
		case DIV:
			checkNumberOperands(token, left, right);
			if ((double)right == 0) {
				throw new RuntimeError(token, "Division by zero.");
			}
			env.assign(token, (double)left / (double)right);
			break;
		case PLUS:
			if (left instanceof String) {
				env.assign(token, (String)left + stringify(right));
			}
			if (left instanceof Double) {
				if (right instanceof Double)
					env.assign(token, (double)left + (double)right);
				try {
					Double rhs = Double.valueOf(doubleFormat(right));
					env.assign(token, (Double)left + rhs);
				} catch (Exception e) {
					env.assign(token, left);
				}
			}
			break;
		default:
			throw new RuntimeError(token, "Incompatible types.");
		}
		return null;
	}
	@Override
	public Object visitMacroExpr(Macro expr) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Void visitModuleStmt(Module stmt) {
		Environment moduleEnv = new Environment(environment);
		// moduleEnv.isModuleEnv = true;
		executeBlock(stmt.statements, moduleEnv);
		environment.define(stmt.name.lexeme, moduleEnv, VarType.CONSTANT);
		return null;
	}
	@Override
	public Void visitReleaseStmt(Release stmt) {
		for (Expr element : stmt.elements) {
			if (element instanceof Expr.Identifier) {
				environment.release(element.token);
			} else {
				Object result = evaluate(element);
				System.out.println(result);
			}
		}
		return null;
	}
	@Override
	public Void visitDeferStmt(Defer stmt) {
		System.out.println("policia defer?");
		// TODO Auto-generated method stub
		return null;
	}
}