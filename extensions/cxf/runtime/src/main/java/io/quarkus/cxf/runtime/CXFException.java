package io.quarkus.cxf.runtime;

public final class CXFException extends Exception {
    private final Object faultInfo;

    public CXFException(String message, Object fault) {
        super(message);
        this.faultInfo = fault;
    }

    public Object getFaultInfo() {
        return faultInfo;
    }
}