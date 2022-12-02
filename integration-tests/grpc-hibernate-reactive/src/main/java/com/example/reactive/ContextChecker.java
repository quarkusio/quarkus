package com.example.reactive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ContextChecker {
    private final Map<Integer, String> requestContexts = new ConcurrentHashMap<>();

    @Inject
    RequestScopeBean requestScopeBean;

    int newContextId(String caller) {
        String original;
        int contextId = requestScopeBean.getId();
        if ((original = requestContexts.put(contextId, caller)) != null) {
            throw new RuntimeException(
                    "request context reused from a different call, original usage: " + original + ", duplicate: " + caller);
        }
        return contextId;
    }

    public int requestContextId() {
        return requestScopeBean.getId();
    }
}
