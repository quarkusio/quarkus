package io.quarkus.devjsonrpc.runtime.comms;

public enum MessageType {
    Void,
    Response,
    SubscriptionMessage,
    HotReload
}
