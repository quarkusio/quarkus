package io.quarkus.hibernate.reactive.panache.common.runtime;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;

@Interceptor
@ReactiveTransactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class ReactiveTransactionalInterceptor extends ReactiveTransactionalInterceptorBase {

}
