package org.acme

import jakarta.interceptor.InterceptorBinding

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@InterceptorBinding
annotation class Logged
