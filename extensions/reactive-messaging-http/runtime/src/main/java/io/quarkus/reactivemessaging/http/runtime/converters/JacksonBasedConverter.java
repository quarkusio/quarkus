package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.smallrye.reactive.messaging.MessageConverter;

public abstract class JacksonBasedConverter implements MessageConverter {
    private static final Logger log = Logger.getLogger(JacksonBasedConverter.class);

    @Override
    public Message<?> convert(Message<?> in, Type target) {
        try {
            return doConvert(in, target);
        } catch (Exception any) {
            String suffix = "";
            if (any instanceof IllegalArgumentException) {
                String message = any.getMessage();
                suffix = ": " + message;
            }
            log.error("Failed to convert payload to type " + target + suffix, any);

            return in;
        }
    }

    protected abstract Message<?> doConvert(Message<?> in, Type target);
}
