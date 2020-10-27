package io.quarkus.rest.common.runtime.util;

public class QuarkusRestUtil {
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
