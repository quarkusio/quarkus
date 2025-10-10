package io.quarkus.it.jpa.attributeconverter;

import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import io.quarkus.it.jpa.util.BeanInstantiator;
import io.quarkus.it.jpa.util.MyCdiContext;

@Converter
// No CDI scope here: it's implicit, which is allowed by the JPA spec
public class MyDataRequiringCDIImplicitScopeConverter implements AttributeConverter<MyDataRequiringCDI, String> {
    @Inject
    MyCdiContext cdiContext;

    private final BeanInstantiator beanInstantiator;

    public MyDataRequiringCDIImplicitScopeConverter() {
        this.beanInstantiator = BeanInstantiator.fromCaller();
    }

    @Override
    public String convertToDatabaseColumn(MyDataRequiringCDI attribute) {
        if (attribute == null) {
            return null;
        }
        MyCdiContext.checkAvailable(cdiContext, beanInstantiator);
        return attribute.getContent();
    }

    @Override
    public MyDataRequiringCDI convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        MyCdiContext.checkAvailable(cdiContext, beanInstantiator);
        return new MyDataRequiringCDI(dbData);
    }
}
