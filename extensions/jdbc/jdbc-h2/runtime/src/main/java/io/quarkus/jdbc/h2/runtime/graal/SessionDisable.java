package io.quarkus.jdbc.h2.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The org.h2.engine.Session represents the "Embedded Database" in H2.
 * We remove this explicitly as it pulls in various things we can't support;
 * rather than address them individually it's simpler to make sure this
 * Session doesn't get included by mistake: that will produce errors
 * that are easier to manage.
 */
@TargetClass(className = "org.h2.engine.Session")
@Delete
public final class SessionDisable {
}
