package io.quarkus.mongodb.panache.transaction.interceptor;

import com.mongodb.client.ClientSession;

public class Transaction {
    private ClientSession clientSession;

    public Transaction(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public ClientSession getClientSession() {
        return clientSession;
    }
}
