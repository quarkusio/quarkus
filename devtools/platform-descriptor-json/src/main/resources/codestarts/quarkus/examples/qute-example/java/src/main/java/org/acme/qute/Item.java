package org.acme.qute;

import java.math.BigDecimal;

public class Item {

    public final BigDecimal price;
    public final String name;

    public Item(BigDecimal price, String name) {
        this.price = price;
        this.name = name;
    }

}