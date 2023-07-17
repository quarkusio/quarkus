package io.quarkus.narayana.interceptor;

import io.quarkus.transaction.annotations.Rollback;

// force a rollback to counter the default
@Rollback
public class AnnotatedTestException extends Exception {
}
