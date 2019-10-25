package io.quarkus.narayana.jta;

import javax.transaction.TransactionScoped;

@TransactionScoped
public class TransactionScopedBean {
    private int value = 0;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
