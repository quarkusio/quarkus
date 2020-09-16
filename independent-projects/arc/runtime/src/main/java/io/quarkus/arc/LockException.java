package io.quarkus.arc;

/**
 * 
 * @see Lock
 */
public class LockException extends RuntimeException {

    private static final long serialVersionUID = 4486284740873061615L;

    public LockException(String message) {
        super(message);
    }

}
