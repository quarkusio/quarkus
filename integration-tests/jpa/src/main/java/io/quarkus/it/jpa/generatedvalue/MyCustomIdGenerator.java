package io.quarkus.it.jpa.generatedvalue;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class MyCustomIdGenerator implements IdentifierGenerator {
    public static String STUB_VALUE = MyCustomIdGenerator.class.getName() + "_STUB_VALUE";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return STUB_VALUE;
    }
}
