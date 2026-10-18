package org.acme.throws_;

/**
 * This exception is only referenced in a catch block (exception table),
 * never in a throws clause or direct bytecode reference.
 * The tree shaker must preserve it via visitTryCatchBlock.
 */
public class CaughtException extends Exception {
    public CaughtException(String message) {
        super(message);
    }
}
