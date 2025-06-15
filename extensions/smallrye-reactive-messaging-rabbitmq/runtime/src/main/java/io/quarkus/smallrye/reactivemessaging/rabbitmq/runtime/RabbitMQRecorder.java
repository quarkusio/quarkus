package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime;

import java.util.Optional;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RabbitMQRecorder {

    public RuntimeValue<CredentialsProviderLink> configureOptions(String credentialsProviderName,
            Optional<String> credentialsProviderBeanName) {

        CredentialsProvider credentialsProvider = CredentialsProviderFinder
                .find(credentialsProviderBeanName.orElse(null));

        return new RuntimeValue<>(new CredentialsProviderLink(credentialsProvider, credentialsProviderName));
    }
}
