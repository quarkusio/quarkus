package org.acme

import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
class LoggedInterceptor {

    @AroundInvoke
    fun intercept(context: InvocationContext): Any = "intercepted:" + context.proceed()
}
