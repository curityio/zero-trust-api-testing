package se.curity.examples.exceptions;

public class AuthorizationException extends Exception {

    public AuthorizationException() {
        super("Unauthorized");
    }

    public AuthorizationException(String errorMessage) {
        super(errorMessage);
    }
}
