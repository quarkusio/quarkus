package io.quarkus.narayana.interceptor;

import io.quarkus.narayana.jta.Rollback;

// prevent a rollback to counter the default
@Rollback(false)
public class AnnotatedError extends Error {
}
