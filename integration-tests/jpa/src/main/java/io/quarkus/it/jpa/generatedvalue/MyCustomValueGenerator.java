package io.quarkus.it.jpa.generatedvalue;

import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

public class MyCustomValueGenerator implements BeforeExecutionGenerator {
    public static String STUB_VALUE = MyCustomValueGenerator.class.getName() + "_STUB_VALUE";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return STUB_VALUE;
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT, EventType.UPDATE);
    }
}
