package io.quarkus.smallrye.reactivemessaging.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MessageConverter;

/**
 * A converter must see the full generic ingested payload type, so that {@code Message<Envelope<Order>>} and
 * {@code Message<Envelope<Invoice>>} can be converted differently.
 */
public class GenericConverterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Envelope.class, Order.class, Invoice.class, MyApp.class, OrderConverter.class,
                            InvoiceConverter.class));

    @Inject
    MyApp app;

    @Test
    public void testGenericConverters() {
        assertThat(app.orders()).hasSize(2).allSatisfy(order -> assertThat(order.id).startsWith("order-"));
        assertThat(app.invoices()).hasSize(2).allSatisfy(invoice -> assertThat(invoice.id).startsWith("invoice-"));
    }

    public static class Envelope<T> {
        public final T content;

        public Envelope(T content) {
            this.content = content;
        }
    }

    public static class Order {
        public final String id;

        public Order(String id) {
            this.id = id;
        }
    }

    public static class Invoice {
        public final String id;

        public Invoice(String id) {
            this.id = id;
        }
    }

    @ApplicationScoped
    public static class MyApp {
        private final List<Order> orders = new ArrayList<>();
        private final List<Invoice> invoices = new ArrayList<>();

        @Outgoing("orders")
        public Multi<String> orderIds() {
            return Multi.createFrom().items("1", "2");
        }

        @Outgoing("invoices")
        public Multi<String> invoiceIds() {
            return Multi.createFrom().items("1", "2");
        }

        @Incoming("orders")
        public CompletionStage<Void> consumeOrder(Message<Envelope<Order>> message) {
            orders.add(message.getPayload().content);
            return message.ack();
        }

        @Incoming("invoices")
        public CompletionStage<Void> consumeInvoice(Message<Envelope<Invoice>> message) {
            invoices.add(message.getPayload().content);
            return message.ack();
        }

        public List<Order> orders() {
            return orders;
        }

        public List<Invoice> invoices() {
            return invoices;
        }
    }

    static boolean isEnvelopeOf(Type target, Class<?> content) {
        return target instanceof ParameterizedType parameterized
                && parameterized.getRawType() == Envelope.class
                && parameterized.getActualTypeArguments()[0] == content;
    }

    @ApplicationScoped
    public static class OrderConverter implements MessageConverter {

        @Override
        public boolean canConvert(Message<?> in, Type target) {
            return in.getPayload() instanceof String && isEnvelopeOf(target, Order.class);
        }

        @Override
        public Message<?> convert(Message<?> in, Type target) {
            return in.withPayload(new Envelope<>(new Order("order-" + in.getPayload())));
        }
    }

    @ApplicationScoped
    public static class InvoiceConverter implements MessageConverter {

        @Override
        public boolean canConvert(Message<?> in, Type target) {
            return in.getPayload() instanceof String && isEnvelopeOf(target, Invoice.class);
        }

        @Override
        public Message<?> convert(Message<?> in, Type target) {
            return in.withPayload(new Envelope<>(new Invoice("invoice-" + in.getPayload())));
        }
    }
}
