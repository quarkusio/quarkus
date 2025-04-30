package io.quarkus.reactive.pg.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.vertx.pgclient.data.Money;

/**
 * Reproduce <a href="https://github.com/quarkusio/quarkus/issues/36144">PG Reactive Client: Cannot create Money value in Range
 * (-1.00, 0.00)</a>.
 */
public class MoneyTest {

    @Test
    void testMoney() {
        Money money = new Money(new BigDecimal("-1.11"));
        assertEquals(BigDecimal.valueOf(-1.11), money.bigDecimalValue());

        money = new Money(new BigDecimal("-0.11"));
        assertEquals(BigDecimal.valueOf(-0.11), money.bigDecimalValue());
    }

}
