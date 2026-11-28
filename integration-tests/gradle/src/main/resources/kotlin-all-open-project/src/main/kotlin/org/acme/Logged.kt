package org.acme

import jakarta.interceptor.InterceptorBinding

@InterceptorBinding
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Logged
