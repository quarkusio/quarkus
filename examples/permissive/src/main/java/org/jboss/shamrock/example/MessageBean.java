package org.jboss.shamrock.example;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class MessageBean {

    @Inject
    Config config;

    public String getMessage() {
        Optional<String> message = config.getOptionalValue("message", String.class);
        if (message.isPresent()) {
            return message.get();
        }
        return "A message";
    }

}
