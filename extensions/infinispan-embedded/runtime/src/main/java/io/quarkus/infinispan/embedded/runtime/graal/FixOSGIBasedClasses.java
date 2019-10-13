package io.quarkus.infinispan.embedded.runtime.graal;

import java.util.Map;

import org.infinispan.commons.util.OsgiClassLoader;
import org.infinispan.commons.util.ServiceFinder;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This class removes osgi based classes in embedded, which we don't support with substrate
 * 
 * @author William Burns
 */

final class FixOSGIBasedClasses {
}

@TargetClass(OsgiClassLoader.class)
@Delete
final class DeleteOsgiClassLoader {
}

@TargetClass(ServiceFinder.class)
final class SubstituteServiceFinder {
    @Substitute
    private static <T> void addOsgiServices(Class<T> contract, Map<String, T> services) {
    }
}

@TargetClass(className = "org.infinispan.commons.util.SecurityActions")
final class SubstituteSecurityActions {
    @Substitute
    private static ClassLoader getOSGiClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
