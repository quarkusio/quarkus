package io.quarkus.cache.runtime;

public class CacheException extends RuntimeException {

    private static final long serialVersionUID = -3465350968605912384L;

    public CacheException(Throwable cause) {
        super(cause);
    }
}
