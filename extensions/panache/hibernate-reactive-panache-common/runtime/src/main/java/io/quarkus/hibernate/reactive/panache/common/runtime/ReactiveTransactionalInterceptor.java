package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;

@Interceptor
@ReactiveTransactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class ReactiveTransactionalInterceptor extends ReactiveTransactionalInterceptorBase {

}
