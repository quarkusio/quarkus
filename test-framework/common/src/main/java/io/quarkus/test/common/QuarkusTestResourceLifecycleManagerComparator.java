package io.quarkus.test.common;

import java.util.Comparator;

public final class QuarkusTestResourceLifecycleManagerComparator implements Comparator<QuarkusTestResourceLifecycleManager> {
    @Override
    public int compare(QuarkusTestResourceLifecycleManager o1, QuarkusTestResourceLifecycleManager o2) {
        return o1.order() - o2.order();
    }
}
