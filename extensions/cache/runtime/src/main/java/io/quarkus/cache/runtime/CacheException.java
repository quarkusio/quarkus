package io.quarkus.cache.runtime;

public class CacheException extends RuntimeException {

    private static final long serialVersionUID = 228265886362860188L;

    public CacheException(Throwable cause) {
        super(cause);
    }
}
