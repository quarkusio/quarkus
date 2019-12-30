package io.quarkus.mongodb.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(com.mongodb.async.client.internal.AsyncCryptBinding.class)
@Substitute
public final class AsyncCryptBindingSubstitutions {
}
