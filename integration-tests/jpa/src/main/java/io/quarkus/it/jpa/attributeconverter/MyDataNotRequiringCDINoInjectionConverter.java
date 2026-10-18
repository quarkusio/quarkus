package io.quarkus.it.jpa.attributeconverter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import io.quarkus.it.jpa.util.BeanInstantiator;
import io.quarkus.it.jpa.util.MyCdiContext;

// We don't add a CDI scope here, and don't use CDI at all in the class.
// This should result in Hibernate ORM being used for instantiation.
@Converter
public class MyDataNotRequiringCDINoInjectionConverter implements AttributeConverter<MyDataNotRequiringCDI, String> {

    private final BeanInstantiator beanInstantiator;

    public MyDataNotRequiringCDINoInjectionConverter() {
        this.beanInstantiator = BeanInstantiator.fromCaller();
    }

    @Override
    public String convertToDatabaseColumn(MyDataNotRequiringCDI attribute) {
        MyCdiContext.checkNotAvailable(null, beanInstantiator);
        return attribute == null ? null : attribute.getContent();
    }

    @Override
    public MyDataNotRequiringCDI convertToEntityAttribute(String dbData) {
        MyCdiContext.checkNotAvailable(null, beanInstantiator);
        return dbData == null ? null : new MyDataNotRequiringCDI(dbData);
    }
}
