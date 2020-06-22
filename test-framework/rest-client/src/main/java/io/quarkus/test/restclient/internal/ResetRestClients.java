package io.quarkus.test.restclient.internal;

import java.lang.reflect.Method;
import java.util.Collection;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.restclient.RestClientTestSupport;

/**
 * This class resets every rest-client back to what is originally was before it was change from {@link RestClientTestSupport#setBaseURI}
 */
public class ResetRestClients implements QuarkusTestAfterEachCallback {

    // we call the methods with reflection because the API is currently not exposed

    private final Method activeUpdatedRestClientsMethod;
    private final Method resetURLMethod;

    public ResetRestClients() {
        try {
            Class<?> restClientTestSupportClass = Class.forName(RestClientTestSupport.class.getName(), true,
                    Thread.currentThread().getContextClassLoader());

            activeUpdatedRestClientsMethod = restClientTestSupportClass.getDeclaredMethod("activeUpdatedRestClients");
            activeUpdatedRestClientsMethod.setAccessible(true);

            resetURLMethod = restClientTestSupportClass.getDeclaredMethod("resetURL", Class.class);
            resetURLMethod.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterEach(QuarkusTestMethodContext context) {
        try {
            Collection<Class<?>> activeUpdatedRestClients = (Collection<Class<?>>) activeUpdatedRestClientsMethod.invoke(null);
            for (Class<?> activeUpdatedRestClient : activeUpdatedRestClients) {
                resetURLMethod.invoke(null, activeUpdatedRestClient);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
