package core;

@SuppressWarnings("serial")
public class ReturnException extends RuntimeException {
	final Object value;
	
	ReturnException(Object value) {
		super(null, null, false, false);
		this.value = value;
	}
}
