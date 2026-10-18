package io.quarkus.it.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GreeterBean {

    private final MessageProducer messageProducer;

    @Autowired
    @Qualifier("noop")
    StringFunction noopStringFunction;

    StringFunction capitalizerStringFunction;

    final String suffix;

    public GreeterBean(MessageProducer messageProducer, @Value("${greeting.suffix:!}") String suffix) {
        this.messageProducer = messageProducer;
        this.suffix = suffix;
    }

    public String greet(String name) {
        final String initialValue = messageProducer.getPrefix() + " " + name + suffix;
        return noopStringFunction.andThen(capitalizerStringFunction).apply(initialValue);
    }

    @Autowired
    public void setCapitalizerStringFunction(@Qualifier("cap") StringFunction capitalizerStringFunction) {
        this.capitalizerStringFunction = capitalizerStringFunction;
    }
}
