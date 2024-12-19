package io.quarkus.devui.runtime.jsonrpc;

import java.util.Objects;

public final class JsonRpcMethodName {
    private String name;

    public JsonRpcMethodName() {
    }

    public JsonRpcMethodName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JsonRpcMethodName other = (JsonRpcMethodName) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return name;
    }

}
