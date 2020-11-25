package org.jboss.resteasy.reactive.common.util;

public class QuarkusRestUtil {
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
