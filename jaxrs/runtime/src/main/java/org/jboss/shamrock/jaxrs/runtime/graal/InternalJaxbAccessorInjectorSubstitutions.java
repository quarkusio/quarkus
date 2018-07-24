package org.jboss.shamrock.jaxrs.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.sun.xml.internal.bind.v2.runtime.reflect.opt.AccessorInjector", onlyWith = InternalJaxbAccessorInjectorSubstitutions.Selector.class)
@Substitute
final class InternalJaxbAccessorInjectorSubstitutions {

    /**
     * Loads the optimized class and returns it.
     *
     * @return null
     * if it fails for some reason.
     */
    @Substitute
    public static Class<?> prepare(
            Class beanClass, String templateClassName, String newClassName, String... replacements) {
        return null;
    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.internal.bind.v2.runtime.reflect.opt.AccessorInjector");
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
