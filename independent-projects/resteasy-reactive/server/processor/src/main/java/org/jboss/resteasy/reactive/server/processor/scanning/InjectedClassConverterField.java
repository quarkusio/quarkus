package org.jboss.resteasy.reactive.server.processor.scanning;

public final class InjectedClassConverterField {
    final String methodName;
    final String injectedClassName;

    public InjectedClassConverterField(String methodName, String injectedClassName) {
        this.methodName = methodName;
        this.injectedClassName = injectedClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getInjectedClassName() {
        return injectedClassName;
    }
}
