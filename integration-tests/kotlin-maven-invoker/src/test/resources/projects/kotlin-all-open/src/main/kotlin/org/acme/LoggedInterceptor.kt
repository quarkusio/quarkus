package org.acme

import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@Logged
@Priority(1)
@Interceptor
class LoggedInterceptor {

    @AroundInvoke
    fun intercept(ctx: InvocationContext): Any? = ctx.proceed()
}
