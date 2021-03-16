package io.quarkus.credentials.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.credentials.CredentialsProvider;

public class CredentialsProviderFinder {

    public static CredentialsProvider find(String type) {
        ArcContainer container = Arc.container();
        CredentialsProvider credentialsProvider = type != null
                ? (CredentialsProvider) container.instance(type).get()
                : container.instance(CredentialsProvider.class).get();

        if (credentialsProvider == null) {
            throw new RuntimeException("unable to find credentials provider of type " + (type == null ? "default" : type));
        }

        return credentialsProvider;
    }

}
