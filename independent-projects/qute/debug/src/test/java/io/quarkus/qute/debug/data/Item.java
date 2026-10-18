package io.quarkus.qute.debug.data;

import java.math.BigDecimal;

public class Item {

    public final BigDecimal price;

    public Item(int price) {
        this(new BigDecimal(price));
    }

    public Item(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Item(" + price.intValue() + ")";
    }
}
