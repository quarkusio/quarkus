package io.quarkus.arc;

/**
 * Implement this provider if you are the reference source that can tell if there is
 * a request ongoing and it has been turned asynchronous. There can only be a single
 * such service provider per application.
 */
public interface AsyncRequestStatusProvider {

    /**
     * @return true if there is a request ongoing and it has been turned asynchronous.
     */
    boolean isCurrentRequestAsync();

}
