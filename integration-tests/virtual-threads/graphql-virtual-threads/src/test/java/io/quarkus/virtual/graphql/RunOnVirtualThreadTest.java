package io.quarkus.virtual.graphql;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.quarkus.virtual.threads.VirtualThreads;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest extends AbstractGraphQLTest {

    //todo how to make sure vt executor is not removed?
    @Inject
    @VirtualThreads
    ExecutorService vt;

    @Test
    public void testAnnotatedBlockingObject() {

        String fooRequest = getPayload("{\n" +
                "  annotatedRunOnVirtualThreadObject {\n" +
                "    name\n" +
                "    priority\n" +
                "    state\n" +
                "    group\n" +
                "    vertxContextClassName\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .log().body().and()
                .body("data.annotatedRunOnVirtualThreadObject.name", Matchers.startsWith("quarkus-virtual-thread"))
                .and()
                .body("data.annotatedRunOnVirtualThreadObject.vertxContextClassName",
                        Matchers.equalTo("io.vertx.core.impl.DuplicatedContext"));
    }

    @Test
    public void testAnnotatedBlockingMutationObject() {

        String fooRequest = getPayload("mutation{\n" +
                "  annotatedRunOnVirtualThreadMutationObject(test:\"test\") {\n" +
                "    name\n" +
                "    priority\n" +
                "    state\n" +
                "    group\n" +
                "    vertxContextClassName\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .log().body().and()
                .body("data.annotatedRunOnVirtualThreadMutationObject.name", Matchers.startsWith("quarkus-virtual-thread"))
                .and()
                .body("data.annotatedRunOnVirtualThreadMutationObject.vertxContextClassName",
                        Matchers.equalTo("io.vertx.core.impl.DuplicatedContext"));
    }

    @Test
    @Disabled
    public void testPiningThread() {

        String fooRequest = getPayload("{\n" +
                "  pinThread {\n" +
                "    name\n" +
                "    priority\n" +
                "    state\n" +
                "    group\n" +
                "    vertxContextClassName\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .log().body().and()
                .body("data.pinThread.name", Matchers.startsWith("quarkus-virtual-thread"))
                .and()
                .body("data.pinThread.vertxContextClassName",
                        Matchers.equalTo("io.vertx.core.impl.DuplicatedContext"));
    }

    @GraphQLApi
    public static class RunOnVirtualThreadObjectTestThreadResource {

        @Inject
        Vertx vertx;

        // Return type Object with @RunOnVirtualThread
        @Query
        @RunOnVirtualThread
        public TestThread annotatedRunOnVirtualThreadObject() {
            sleep();
            return getTestThread();
        }

        // Return type Object with @RunOnVirtualThread
        @Mutation
        @RunOnVirtualThread
        public TestThread annotatedRunOnVirtualThreadMutationObject(String test) {
            sleep();
            return getTestThread();
        }

        @Query
        @RunOnVirtualThread
        public TestThread pinThread() {
            // Synchronize on an object to cause thread pinning
            Object lock = new Object();
            synchronized (lock) {
                sleep();
            }
            return getTestThread();
        }

        private void sleep() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        private TestThread getTestThread() {
            Thread t = Thread.currentThread();
            long id = t.getId();
            String name = t.getName();
            int priority = t.getPriority();
            String state = t.getState().name();
            String group = t.getThreadGroup().getName();
            return new TestThread(id, name, priority, state, group);
        }
    }
}
