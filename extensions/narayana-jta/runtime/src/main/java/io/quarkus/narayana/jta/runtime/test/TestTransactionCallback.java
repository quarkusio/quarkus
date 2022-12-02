package io.quarkus.narayana.jta.runtime.test;

public interface TestTransactionCallback {

    void postBegin();

    void preRollback();

}
