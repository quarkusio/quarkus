package io.quarkus.it.infinispan.embedded;

import org.infinispan.interceptors.BaseCustomAsyncInterceptor;

// Here to test a custom intereceptor configured via XML
public class CustomInterceptor extends BaseCustomAsyncInterceptor {
    String foo; // configured via XML
}
