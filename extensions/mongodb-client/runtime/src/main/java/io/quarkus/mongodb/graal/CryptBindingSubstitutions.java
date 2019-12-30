package io.quarkus.mongodb.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.mongodb.client.internal.CryptBinding")
@Substitute
public final class CryptBindingSubstitutions {
}
