package io.quarkus.mongodb.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.mongodb.async.client.internal.AsyncCryptConnection")
@Substitute
public final class AsyncCryptConnectionSubstitutions {
}
