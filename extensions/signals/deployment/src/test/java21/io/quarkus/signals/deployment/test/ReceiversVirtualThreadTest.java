package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Signal;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.quarkus.test.QuarkusExtensionTest;

public class ReceiversVirtualThreadTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Order.class));

    @Inject
    Signal<Order> order;

    @Inject
    Receivers receivers;

    @Test
    public void testSetExecutionModel() {
        List<Boolean> virtualFlags = new CopyOnWriteArrayList<>();

        var reg = receivers.newReceiver(Order.class)
                .setExecutionModel(ExecutionModel.VIRTUAL_THREAD)
                .notify(ctx -> {
                    virtualFlags.add(Thread.currentThread().isVirtual());
                });

        order.publish(new Order("vt"));
        Awaitility.await().until(() -> virtualFlags.size() >= 1);
        assertEquals(1, virtualFlags.size());
        assertTrue(virtualFlags.get(0), "Receiver should be executed on a virtual thread");

        reg.unregister();
    }

    record Order(String id) {
    }
}
