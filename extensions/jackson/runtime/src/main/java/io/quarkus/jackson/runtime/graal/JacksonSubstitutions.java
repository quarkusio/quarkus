package io.quarkus.jackson.runtime.graal;

import java.lang.reflect.Constructor;

import com.fasterxml.jackson.databind.util.ClassUtil;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.fasterxml.jackson.databind.util.ClassUtil")
final class Target_com_fasterxml_jackson_databind_util_ClassUtil {
    @Substitute
    public static <T> Constructor<T> findConstructor(Class<T> cls, boolean forceAccess) {
        try {
            Constructor<T> ctor = cls.getDeclaredConstructor();
            if (forceAccess) {
                ClassUtil.checkAndFixAccess(ctor, forceAccess);
            }
            return ctor;
        } catch (NoSuchMethodException e) {
            ;
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e,
                    "Failed to find default constructor of class " + cls.getName() + ", problem: " + e.getMessage());
        }
        return null;
    }
}

class JacksonSubstitutions {
}
