package io.quarkus.arc.test.interceptors.inheritance;

import javax.interceptor.Interceptor;

@Two
@Interceptor
public class Interceptor2 extends OverridenInterceptor {

}
