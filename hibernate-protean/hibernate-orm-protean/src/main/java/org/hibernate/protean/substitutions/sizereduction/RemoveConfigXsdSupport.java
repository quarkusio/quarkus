package org.hibernate.protean.substitutions.sizereduction;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Class org.hibernate.boot.xsd.ConfigXsdSupport is used only to validate configuration schema,
 * which should never happen at runtime.
 * It also keeps hold of references to parsed schemas in static fields, which is good for bootstrap
 * performance when running in the JVM - so we need to remove this cache.
 *
 * WARNING: This single removal is worth almost 5MB of size in the final image.
 */
@TargetClass(className = "org.hibernate.boot.xsd.ConfigXsdSupport")
@Delete
public final class RemoveConfigXsdSupport {
}
