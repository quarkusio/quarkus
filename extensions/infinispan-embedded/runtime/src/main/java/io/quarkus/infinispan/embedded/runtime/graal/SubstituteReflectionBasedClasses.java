package io.quarkus.infinispan.embedded.runtime.graal;

import org.infinispan.commons.util.Util;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class SubstituteReflectionBasedClasses {
}

@TargetClass(Util.class)
final class SubstituteUtil {
    @Substitute
    public static <T> T getInstance(String classname, ClassLoader cl) {
        if (classname == null)
            throw new IllegalArgumentException("Cannot load null class!");
        switch (classname) {
            case "org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup":
                return (T) new JBossStandaloneJTAManagerLookup();
            default:
                Class<T> clazz = loadClass(classname, cl);
                return getInstance(clazz);
        }
    }

    @Alias
    public static <T> Class<T> loadClass(String classname, ClassLoader cl) {
        return null;
    }

    @Alias
    public static <T> T getInstance(Class<T> clazz) {
        return null;
    }
}
