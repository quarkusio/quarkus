package io.quarkus.funqy.knative.events;

public @interface EventAttribute {
    /**
     * Defines the cloud event attribute name that will be used for additional filtering
     * of incoming events
     *
     * @return
     */
    String name();

    /**
     * Defines the cloud event attribute's (one defined by <code>name</code>) value that
     * will be used for additional filtering of incoming events
     *
     * @return
     */
    String value();
}
