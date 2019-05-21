package io.quarkus.it.kogito.jbpm;

import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CalculationService {

    private Random random = new Random();

    public Order calculateTotal(Order order) {
        order.setTotal(random.nextDouble());

        return order;
    }
}
