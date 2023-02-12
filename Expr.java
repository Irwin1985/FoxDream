package core;

import java.util.List;

public abstract class Expr {
	final Token token;
	
	public Expr(Token token) {
		this.token = token;
	}
	
	interface Visitor<R> {
		R visitLiteralExpr(Literal expr);
		R visitMacroExpr(Macro expr);
		R visitLogicalExpr(Logical expr);
		R visitBinaryExpr(Binary expr);
		R visitUnaryExpr(Unary expr);
		R visitMemberExpr(Member expr);
		R visitCallExpr(Call expr);
		R visitCreateObjectExpr(CreateObject expr);
		R visitThisExpr(This expr);
		R visitIdentifierExpr(Identifier expr);
		R visitNamedExpr(NamedExp expr);
		R visitIfExpr(IfExpr expr);
	}
	/**
	 * Literal
	 */
	static class Literal extends Expr {		
		public Literal(Token token) {
			super(token);
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}
	}
	/**
	 * Macro
	 */
	static class Macro extends Expr {
		final List<Expr> macros;
		
		public Macro(Token token, List<Expr> macros) {
			super(token);
			this.macros = macros;				
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitMacroExpr(this);
		}
	}
	
	/**
	 * Logical
	 */
	static class Logical extends Expr {
		final Expr left;
		final Token operator;
		final Expr right;
		
		public Logical(Token token, Expr left, Token operator, Expr right) {
			super(token);
			this.left = left;
			this.operator = operator;
			this.right = right;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLogicalExpr(this);
		}
	}
	/**
	 * Binary
	 */
	static class Binary extends Expr {
		final Expr left;
		final Expr right;
		
		public Binary(Token token, Expr left, Expr right) {
			super(token);
			this.left = left;
			this.right = right;
		}		
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}
	}
	/**
	 * Unary
	 */
	static class Unary extends Expr {
		final Expr right;
		
		public Unary(Token token, Expr right) {
			super(token);
			this.right = right;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}
	}
	/**
	 * Member
	 */
	static class Member extends Expr {
		final boolean computed;
		final Expr parentObject;
		final Expr property;
		
		public Member(Token token, boolean computed, Expr parentObject, Expr property) {
			super(token);
			this.computed = computed;
			this.parentObject = parentObject;
			this.property = property;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitMemberExpr(this);
		}
	}
	
	/**
	 * Call
	 */
	static class Call extends Expr {
		final Expr callee;
		List<NamedExp> arguments;
		
		public Call(Token token, Expr callee, List<NamedExp> arguments) {
			super(token);
			this.callee = callee;
			this.arguments = arguments;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}		
	}

	/**
	 * CreateObject
	 */
	static class CreateObject extends Expr {
		final Token name;
		List<Expr> arguments;
		
		public CreateObject(Token token, Token name, List<Expr> arguments) {
			super(token);
			this.name = name;
			this.arguments = arguments;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitCreateObjectExpr(this);
		}		
	}	
	
	/**
	 * This
	 */
	static class This extends Expr {
		public This(Token token) {
			super(token);
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitThisExpr(this);
		}
	}	
	/**
	 * Identifier
	 */
	static class Identifier extends Expr {
		
		public Identifier(Token token) {
			super(token);
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIdentifierExpr(this);
		}
	}
	
	/**
	 * NamedExp
	 */
	static class NamedExp extends Expr {
		final String alias;
		final Expr value;
		
		public NamedExp(Token token, String alias, Expr value) {
			super(token);
			this.alias = alias;
			this.value = value;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitNamedExpr(this);
		}
	}
	
	static class IfExpr extends Expr {
		final Expr condition;
		final Expr consequence;
		final Expr alternative;
		
		public IfExpr(Token token, Expr condition, Expr consequence, Expr alternative) {
			super(token);
			this.condition = condition;
			this.consequence = consequence;
			this.alternative = alternative;
		}
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfExpr(this);
		}
	}
		
	abstract <R> R accept(Visitor<R> visitor);
}
