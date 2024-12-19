package io.quarkus.devui.runtime.jsonrpc;

import java.util.Map;

public final class JsonRpcMethod {
    private Class clazz;
    private String methodName;
    private Map<String, Class> params;

    private boolean isExplicitlyBlocking;
    private boolean isExplicitlyNonBlocking;

    public JsonRpcMethod() {
    }

    public JsonRpcMethod(Class clazz, String methodName, Map<String, Class> params) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.params = params;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public Map<String, Class> getParams() {
        return params;
    }

    public boolean hasParams() {
        return this.params != null && !this.params.isEmpty();
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setParams(Map<String, Class> params) {
        this.params = params;
    }

    public boolean getExplicitlyBlocking() {
        return isExplicitlyBlocking;
    }

    public void setExplicitlyBlocking(boolean blocking) {
        isExplicitlyBlocking = blocking;
    }

    public boolean getExplicitlyNonBlocking() {
        return isExplicitlyNonBlocking;
    }

    public void setExplicitlyNonBlocking(boolean nonblocking) {
        isExplicitlyNonBlocking = nonblocking;
    }

    @Override
    public String toString() {
        return clazz.getName() + ":" + methodName + "(" + params + ")";
    }

}
