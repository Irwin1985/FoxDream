package core;

public class RuntimeClass implements CallableObject {
	private final Stmt.Class declaration;	
	
	public RuntimeClass(Stmt.Class declaration) {
		this.declaration = declaration;
	}
	
	@Override
	public int arity() {
		if (declaration.constructorIndex >= 0) {
			return declaration.methods.get(0).parameters.size();
		}
		return 0;
	}

	@Override
	public Object call(Interpreter interpreter, ArgValue[] arguments) {
		System.out.println("Policia call!");
		return null;
	}

	@Override
	public void setCallee(Expr callee) {
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
