package io.quarkus.amazon.lambda.runtime;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Client;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

public class ClientContextImpl implements ClientContext {
    private ClientImpl impl;
    private Map<String, String> cust;
    private Map<String, String> env;

    @JsonGetter("client")
    public ClientImpl getImpl() {
        return impl;
    }

    @JsonSetter("client")
    public void setImpl(ClientImpl impl) {
        this.impl = impl;
    }

    @JsonGetter("custom")
    public Map<String, String> getCust() {
        return cust;
    }

    @JsonSetter("custom")
    public void setCust(Map<String, String> cust) {
        this.cust = cust;
    }

    @JsonGetter("env")
    public Map<String, String> getEnv() {
        return env;
    }

    @JsonSetter("env")
    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    @Override
    @JsonIgnore
    public Client getClient() {
        return impl;
    }

    @Override
    @JsonIgnore
    public Map<String, String> getCustom() {
        return cust;
    }

    @Override
    @JsonIgnore
    public Map<String, String> getEnvironment() {
        return env;
    }
}
