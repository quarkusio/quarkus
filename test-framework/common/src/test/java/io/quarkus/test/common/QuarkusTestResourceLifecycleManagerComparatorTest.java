package io.quarkus.test.common;

import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.TreeSet;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class QuarkusTestResourceLifecycleManagerComparatorTest {

    private final QuarkusTestResourceLifecycleManager q1 = new QuarkusTestResourceLifecycleManager() {
        @Override
        public Map<String, String> start() {
            return null;
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 1;
        }

    };

    private final QuarkusTestResourceLifecycleManager q2 = new QuarkusTestResourceLifecycleManager() {
        @Override
        public Map<String, String> start() {
            return null;
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return -1;
        }

    };

    final QuarkusTestResourceLifecycleManager q3 = new QuarkusTestResourceLifecycleManager() {
        @Override
        public Map<String, String> start() {
            return null;
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 25;
        }

    };

    @Test
    public void testPriorityOfQuarkusTestResourceLifecycle() {

        final TreeSet<QuarkusTestResourceLifecycleManager> quarkusTestResourceLifecycleManagers = new TreeSet<>(
                new QuarkusTestResourceLifecycleManagerComparator());

        quarkusTestResourceLifecycleManagers.add(q1);
        quarkusTestResourceLifecycleManagers.add(q2);
        quarkusTestResourceLifecycleManagers.add(q3);

        assertThat(quarkusTestResourceLifecycleManagers.pollFirst(), CoreMatchers.is(q2));
        assertThat(quarkusTestResourceLifecycleManagers.pollFirst(), CoreMatchers.is(q1));
        assertThat(quarkusTestResourceLifecycleManagers.pollFirst(), CoreMatchers.is(q3));

    }

}
