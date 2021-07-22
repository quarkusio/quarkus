package io.quarkus.test.kubernetes.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Use on your test resource to get a mock {@link OpenShiftServer} spawn up, and injectable with {@link OpenShiftTestServer}.
 * This annotation is only active when used on a test class, and only for this test class.
 */
@QuarkusTestResource(value = OpenShiftServerTestResource.class, restrictToAnnotatedClass = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithOpenShiftTestServer {

    /**
     * Start it with HTTPS
     */
    boolean https() default false;

    /**
     * Start it in CRUD mode
     */
    boolean crud() default true;

    /**
     * Setup class to call after the mock server is created, for custom setup.
     */
    Class<? extends Consumer<OpenShiftServer>> setup() default NO_SETUP.class;

    class NO_SETUP implements Consumer<OpenShiftServer> {
        @Override
        public void accept(OpenShiftServer t) {
        }
    }
}
