package io.quarkus.it.vertx.websessions;

import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.session.SessionFilter;
import io.restassured.response.Response;

@QuarkusTest
public class CounterTest {
    @Test
    public void test() throws InterruptedException {
        when().get("/check-sessions").then().statusCode(200).body(Matchers.is("OK"));

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            users.add(new User(100));
        }

        for (User user : users) {
            user.start();
        }
        for (User user : users) {
            user.join();
        }
        for (User user : users) {
            user.verify();
        }
    }

    static class User extends Thread {
        private static final AtomicInteger counter = new AtomicInteger();

        private final Set<String> sessionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        private final Queue<String> responses = new ConcurrentLinkedQueue<>();

        private final int requests;

        User(int requests) {
            super("User" + counter.incrementAndGet());
            this.requests = requests;
        }

        @Override
        public void run() {
            SessionFilter sessions = new SessionFilter();
            for (int i = 0; i < requests; i++) {
                Response response = with().filter(sessions).get("/counter");
                if (response.sessionId() != null) {
                    sessionIds.add(response.sessionId());
                }
                responses.add(response.body().asString());

                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(50));
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void verify() {
            assertEquals(1, sessionIds.size());
            String id = sessionIds.iterator().next();

            assertEquals(requests, responses.size());
            int i = 1;
            for (String response : responses) {
                assertEquals(id + "|" + i, response);
                i++;
            }
        }
    }
}
