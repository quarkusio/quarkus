package io.quarkus.mongodb.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.mongodb.client.internal.CryptConnection")
@Substitute
public final class CryptConnectionSubstitutions {
}
