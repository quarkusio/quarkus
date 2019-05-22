package io.quarkus.arc;

import java.lang.reflect.Method;

/**
 * Implement this provider if you are the reference source that can tell if there is
 * a request ongoing and it has been turned asynchronous. There can only be a single
 * such service provider per application.
 */
public interface AsyncRequestStatusProvider {

    /**
     * @param method
     * @return true if there is a request ongoing and it has been turned asynchronous.
     */
    boolean isCurrentRequestAsync(Method method);

}
