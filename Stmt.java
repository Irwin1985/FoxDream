package core;

import java.util.List;

import core.Expr.Visitor;

public abstract class Stmt {
	final Token token;
	
	public Stmt(Token token) {
		this.token = token;
	}
	
	interface Visitor<R> {
		R visitSimpleAssignment(SimpleAssignment stmt);
		R visitMultipleAssignmentStmt(MultipleAssignment stmt);
		R visitExpressionStmt(Expression stmt);
		R visitPrintStmt(Print stmt);
		R visitReturnStmt(Return stmt);
		R visitVarStmt(Var stmt);
		R visitBlockStmt(Block stmt);
		R visitIfStmt(If stmt);
		R visitDoCaseStmt(DoCase stmt);
		R visitDoWhileStmt(DoWhile stmt);
		R visitDoStmt(Do stmt);
		R visitExitStmt(Exit stmt);
		R visitLoopStmt(Loop stmt);
		R visitForStmt(For stmt);
		R visitFunctionStmt(Function stmt);
		R visitClassStmt(Class stmt);
		R visitConstantStmt(Const stmt);
		R visitComplexAssignment(ComplexAssignment stmt);
		R visitModuleStmt(Module stmt);
		R visitReleaseStmt(Release stmt);
		R visitDeferStmt(Defer stmt);
	}
	/**
	 * Assignment
	 */
	static class MultipleAssignment extends Stmt {	
		final List<Expr> leftElements;
		final List<Expr> values;
		
		public MultipleAssignment(Token token, List<Expr> leftElements, List<Expr> values) {
			super(token);
			this.leftElements = leftElements;
			this.values = values;				
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitMultipleAssignmentStmt(this);
		}
	}	
	
	/**
	 * SimpleAssignment (foo = bar)
	 */
	static class SimpleAssignment extends Stmt {
		final Expr left;
		final Expr value;
		
		public SimpleAssignment(Token token, Expr left, Expr value) {
			super(token);
			this.left = left;
			this.value = value;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSimpleAssignment(this);
		}
	}
	
	static class ComplexAssignment extends Stmt {
		final Expr left;
		final Expr value;
		
		public ComplexAssignment(Token token, Expr left, Expr value) {
			super(token);
			this.left = left;
			this.value = value;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitComplexAssignment(this);
		}
	}
	/**
	 * Expression Statement
	 */
	static class Expression extends Stmt {
		final Expr expression;
		public Expression(Token token, Expr expression) {
			super(token);
			this.expression = expression;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}
	}
	/**
	 * Print
	 */
	static class Print extends Stmt {
		final List<Expr> expressionList;
		
		public Print(Token token, List<Expr> expressionList) {
			super(token);
			this.expressionList = expressionList;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}
	}
	/**
	 * Return
	 */
	static class Return extends Stmt {
		final List<Expr> expressions;
		
		public Return(Token token, List<Expr> expressions) {
			super(token);
			this.expressions = expressions;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}
	}
	/**
	 * Variable Declaration: a variable internal structure
	 */
	static class VarDecl {
		final Expr.Identifier name;
		final Expr initializer;
		final Object defaultValue;
		
		public VarDecl(Expr.Identifier name, Expr initializer, Object defaultValue) {
			this.name = name;
			this.initializer = initializer;
			this.defaultValue = defaultValue;
		}
	}
	/**
	 * Variable Statement
	 */
	static class Var extends Stmt {
		final Kind scope;
		final List<VarDecl> declarations;
		final List<Expr> values;
		final boolean isMultipleAssign;
		
		public Var(Token token, Kind scope, List<VarDecl> declarations, List<Expr> values, boolean isMultipleAssign) {
			super(token);
			this.scope = scope;
			this.declarations = declarations;
			this.values = values;
			this.isMultipleAssign = isMultipleAssign;			
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}
	}
	/**
	 * Block
	 */
	static class Block extends Stmt {
		final List<Stmt> statements;
		
		public Block(Token token, List<Stmt> statements) {
			super(token);
			this.statements = statements;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}
	}
	/**
	 * If
	 */
	static class If extends Stmt {
		final Expr condition;
		final Block thenBranch;
		final Block elseBranch; 
		
		public If(Token token, Expr condition, Block thenBranch, Block elseBranch) {
			super(token);
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}
	}
	
	static class Case {
		final Token token;
		final List<Expr> conditions;
		final Block body;
		
		public Case(Token token, List<Expr> conditions, Block body) {
			this.token = token;
			this.conditions = conditions;
			this.body = body;
		}
	}
	
	/**
	 * DoCase
	 */
	static class DoCase extends Stmt {
		final List<Case> branches;
		final Block otherwise;
		
		public DoCase(Token token, List<Case> branches, Block otherwise) {
			super(token);
			this.branches = branches;
			this.otherwise = otherwise;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitDoCaseStmt(this);
		}
	}
	/**
	 * DoWhile
	 */
	static class DoWhile extends Stmt {
		final Expr condition;
		final Block block;
		
		public DoWhile(Token token, Expr condition, Block block) {
			super(token);
			this.condition = condition;
			this.block = block;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitDoWhileStmt(this);
		}
	}
	/**
	 * Do statement
	 */
	static class Do extends Stmt {
		final Expr condition;
		final Block block;
		
		public Do(Token token, Expr condition, Block block) {
			super(token);
			this.condition = condition;
			this.block = block;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitDoStmt(this);
		}
	}
	/**
	 * Exit
	 */
	static class Exit extends Stmt {
		
		public Exit(Token token) {
			super(token);
		}
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExitStmt(this);
		}
	}
	/**
	 * Loop
	 */
	static class Loop extends Stmt {
		
		public Loop(Token token) {
			super(token);
		}
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLoopStmt(this);
		}
	}
	/**
	 * For
	 */
	static class For extends Stmt {
		final Expr.Identifier identifier;
		final Expr initialValue;
		final Expr finalValue;
		final Expr increment;
		final Block block;
		
		public For(Token token, Expr.Identifier identifier, Expr initialValue, Expr finalValue, Expr increment, Block block) {
			super(token);
			this.identifier = identifier;
			this.initialValue = initialValue;
			this.finalValue = finalValue;
			this.increment = increment;
			this.block = block;
		}			
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitForStmt(this);
		}
	}
	/**
	 * Function
	 */
	static class Function extends Stmt {
		final Expr.Identifier name;
		final List<Expr.NamedExp> parameters;
		final List<Stmt> deferList;
		final List<Stmt> statements;		
		
		public Function(Token token, Expr.Identifier name, List<Expr.NamedExp> parameters, List<Stmt> statements, List<Stmt> deferList) {
			super(token);
			this.name = name;
			this.parameters = parameters;
			this.statements = statements;			
			this.deferList = deferList;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}
	}
	/**
	 * Class
	 */
	static class Class extends Stmt {
		final Expr.Identifier name;
		final Expr.Identifier superClass;
		final List<Stmt.MultipleAssignment> properties;
		final List<Function> methods;
		final int constructorIndex;
		
		public Class(Token token, 
				Expr.Identifier name, 
				Expr.Identifier superClass, 
				List<Stmt.MultipleAssignment> properties, 
				List<Function> methods, int constructorIndex) {
			super(token);
			this.name = name;
			this.superClass = superClass;
			this.properties = properties;
			this.methods = methods;
			this.constructorIndex = constructorIndex;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}
	}
	/**
	 * Const
	 */
	static class Const extends Stmt {
		final Expr.Identifier name;
		final Expr value;
		
		public Const(Token token, Expr.Identifier name, Expr value) {
			super(token);
			this.name = name;
			this.value = value;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitConstantStmt(this);
		}
	}
	
	/**
	 * Module
	 */
	static class Module extends Stmt {
		final List<Stmt> statements;
		final Token name;
		
		public Module(Token token, Token name, List<Stmt> statements) {
			super(token);
			this.name = name;
			this.statements = statements;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitModuleStmt(this);
		}
	}
	
	/**
	 * Release
	 */
	static class Release extends Stmt {
		final List<Expr> elements;
		
		public Release(Token token, List<Expr> elements) {
			super(token);
			this.elements = elements;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReleaseStmt(this);
		}
	}
	/**
	 * Defer
	 */
	static class Defer extends Stmt {
		final List<Stmt> statements;
		
		public Defer(Token token, List<Stmt> statements) {
			super(token);
			this.statements = statements;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitDeferStmt(this);
		}
	}
	
	abstract <R> R accept(Visitor<R> visitor);
}
