package core;

public interface CallableObject {
	int arity();
	void setCallee(Expr callee);
	String[] getParamInfo();
	boolean validateArguments();
	Object call(Interpreter interpreter, ArgValue[] arguments);
}
