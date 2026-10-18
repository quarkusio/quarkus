package io.quarkus.arc.test.interceptors.inheritance;

import jakarta.interceptor.Interceptor;

@Two
@Interceptor
public class Interceptor2 extends OverridenInterceptor {

}
