package io.quarkus.it.jpa.generatedvalue;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class MyCustomGenericGeneratorReferencedAsClass implements IdentifierGenerator {
    public static String STUB_VALUE = MyCustomGenericGeneratorReferencedAsClass.class.getName() + "_STUB_VALUE";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return STUB_VALUE;
    }
}
