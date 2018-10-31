package org.jboss.shamrock.camel.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.impl.StaticLoggerBinder;

class Slf4jSubstitutions {
}


@Substitute
@TargetClass(className = "org.slf4j.LoggerFactory")
final class Target_org_slf4j_LoggerFactory {
    @Substitute
    public static Logger getLogger(String name) {
        ILoggerFactory iLoggerFactory = getILoggerFactory();
        return iLoggerFactory.getLogger(name);
    }

    @Substitute
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    @Substitute
    public static ILoggerFactory getILoggerFactory() {
        return StaticLoggerBinder.getSingleton().getLoggerFactory();
    }
}
