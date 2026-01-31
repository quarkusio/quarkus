package io.quarkus.quartz.test.security;

import static io.quarkus.scheduler.Scheduled.QUARTZ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.RunAsUser;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

class QuartzSchedulerRunAsUserTest {

    private static final String UNAUTHENTICATED_SCHEDULER = "unauthenticated";
    private static final String AUTHENTICATED_SCHEDULER = "authenticated";
    private static final String FORBIDDEN_SCHEDULER = "forbidden";
    private static final String AUTHORIZED_SCHEDULER = "authorized";

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Scheduler.class, SecuredBean.class, StaticScheduler.class))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-security", Version.getVersion())));

    @Inject
    Scheduler scheduler;

    @Test
    void testRunAsUserAnnotationOnBeanMethods() throws InterruptedException {
        for (var e : scheduler.getLatchMap().entrySet()) {
            var latchKey = e.getKey();
            var latch = e.getValue();
            var result = latch.await(5, TimeUnit.SECONDS);
            assertTrue(result, () -> "Latch " + latchKey + " did not count down in time");
            var failure = scheduler.getLatchKeyToFailure().get(latchKey);
            assertNull(failure, () -> "Test for latch '" + latchKey + "' failed over: " + failure);
        }
    }

    @Test
    void testRunAsUserAnnotationOnStaticMethod() throws InterruptedException {
        var result = StaticScheduler.LATCH.await(5, TimeUnit.SECONDS);
        assertTrue(result, "Latch on static scheduler did not count down in time");
        if (StaticScheduler.authenticatedTestFailure != null) {
            fail("Static scheduler should succeed when calling method that requires authentication",
                    StaticScheduler.authenticatedTestFailure);
        }
        if (StaticScheduler.rolesAllowedTestFailure != null) {
            fail("Static scheduler should succeed when calling to method that requires 'user' role",
                    StaticScheduler.rolesAllowedTestFailure);
        }
        if (StaticScheduler.forbiddenTestFailure == null) {
            fail("Static scheduler should fail when calling to method that requires 'admin' role");
        }
        assertThat(StaticScheduler.forbiddenTestFailure).isInstanceOf(ForbiddenException.class);
    }

    static class StaticScheduler {

        private static final CountDownLatch LATCH = new CountDownLatch(1);
        private static volatile boolean run = false;
        private static Throwable forbiddenTestFailure = null;
        private static Throwable authenticatedTestFailure = null;
        private static Throwable rolesAllowedTestFailure = null;

        @RunAsUser(user = "Elliott", roles = "user")
        @Scheduled(every = "1s", executeWith = QUARTZ)
        static void everySecond() {
            if (!run) {
                run = true;
                try {
                    authenticated();
                } catch (Throwable throwable) {
                    authenticatedTestFailure = throwable;
                }
                try {
                    rolesAllowedUser();
                } catch (Throwable throwable) {
                    rolesAllowedTestFailure = throwable;
                }
                try {
                    rolesAllowedAdmin();
                } catch (Throwable throwable) {
                    forbiddenTestFailure = throwable;
                }
                LATCH.countDown();
            }
        }

        @Authenticated
        static void authenticated() {

        }

        @RolesAllowed("user")
        static void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        static void rolesAllowedAdmin() {

        }

    }

    @ApplicationScoped
    static class Scheduler {

        private final Map<String, CountDownLatch> latchMap;
        private final Map<String, Throwable> latchKeyToFailure;
        private final SecuredBean securedBean;

        Scheduler(SecuredBean securedBean) {
            this.latchKeyToFailure = new ConcurrentHashMap<>();
            this.latchMap = Map.of(
                    UNAUTHENTICATED_SCHEDULER, new CountDownLatch(2),
                    FORBIDDEN_SCHEDULER, new CountDownLatch(2),
                    AUTHORIZED_SCHEDULER, new CountDownLatch(2),
                    AUTHENTICATED_SCHEDULER, new CountDownLatch(2));
            this.securedBean = securedBean;
        }

        @Scheduled(every = "1s", executeWith = QUARTZ)
        void noRunAsUserAnnotation() {
            runTest(UNAUTHENTICATED_SCHEDULER, () -> {
                try {
                    securedBean.authenticated();
                } catch (UnauthorizedException ignored) {
                    return;
                }
                throw new AssertionError("Authorization should fail for scheduled method 'noRunAsUserAnnotation'");
            });
        }

        @RunAsUser(user = "Quentin")
        @Scheduled(every = "1s", executeWith = QUARTZ)
        void runAsUserAnnotationWithVoidReturnType() {
            runTest(AUTHENTICATED_SCHEDULER, () -> {
                try {
                    securedBean.authenticated();
                } catch (UnauthorizedException exception) {
                    throw new AssertionError(
                            "Authorization should not fail for scheduled method 'runAsUserAnnotationWithVoidReturnType'",
                            exception);
                }
            });
        }

        @RunAsUser(user = "Julia", roles = "user")
        @Scheduled(every = "1s", executeWith = QUARTZ)
        Uni<Void> runAsUserAnnotationWithUniReturnType() {
            return Uni.createFrom().item(() -> {
                runTest(FORBIDDEN_SCHEDULER, () -> {
                    try {
                        securedBean.rolesAllowedAdmin();
                    } catch (ForbiddenException exception) {
                        return;
                    }
                    throw new AssertionError(
                            "Authorization should fail for scheduled method 'runAsUserAnnotationWithUniReturnType'");
                });
                return null;
            });
        }

        @RunAsUser(user = "Alice", roles = "admin")
        @Scheduled(every = "1s", executeWith = QUARTZ)
        CompletionStage<Void> runAsUserAnnotationWithCompletionStageReturnType() {
            return Uni.createFrom().<Void> item(() -> {
                runTest(AUTHORIZED_SCHEDULER, () -> {
                    try {
                        securedBean.rolesAllowedAdmin();
                    } catch (ForbiddenException exception) {
                        throw new AssertionError(
                                "Authorization should not fail for scheduled method 'runAsUserAnnotationWithCompletionStageReturnType'",
                                exception);
                    }
                });
                return null;
            }).subscribeAsCompletionStage();
        }

        private void runTest(String latchKey, Runnable test) {
            try {
                test.run();
            } catch (Throwable failure) {
                latchKeyToFailure.put(latchKey, failure);
            }
            latchMap.get(latchKey).countDown();
        }

        Map<String, CountDownLatch> getLatchMap() {
            return latchMap;
        }

        Map<String, Throwable> getLatchKeyToFailure() {
            return latchKeyToFailure;
        }
    }

    @ApplicationScoped
    static class SecuredBean {

        @Authenticated
        void authenticated() {

        }

        @RolesAllowed("admin")
        void rolesAllowedAdmin() {

        }

    }

}
