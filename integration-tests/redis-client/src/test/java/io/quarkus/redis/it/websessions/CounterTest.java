package io.quarkus.redis.it.websessions;

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

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.session.SessionFilter;
import io.restassured.response.Response;

@QuarkusTest
public class CounterTest {
    @Test
    public void test() throws InterruptedException {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            users.add(new User(20));
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
                    // need to sleep longer to give the session store some time to finish
                    //
                    // the operation to store session data into Redis is fired off when response headers are written,
                    // but there's nothing waiting for that operation to complete when the response is being sent
                    //
                    // therefore, if we send a 2nd request too quickly after receiving the 1st response,
                    // the session data may still be in the process of being stored and the 2nd request
                    // would get stale session data
                    Thread.sleep(500 + ThreadLocalRandom.current().nextInt(500));
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
