package io.quarkus.it.jpa.attributeconverter;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import io.quarkus.it.jpa.util.BeanInstantiator;
import io.quarkus.it.jpa.util.MyCdiContext;

@Converter
@Vetoed // We really don't want CDI for some reason.
public class MyDataNotRequiringCDIVetoedConverter implements AttributeConverter<MyDataNotRequiringCDI, String> {
    // This will always be null.
    // It's only here to check that one can force CDI not to be used.
    @Inject
    MyCdiContext cdiContext;

    private final BeanInstantiator beanInstantiator;

    public MyDataNotRequiringCDIVetoedConverter() {
        this.beanInstantiator = BeanInstantiator.fromCaller();
    }

    @Override
    public String convertToDatabaseColumn(MyDataNotRequiringCDI attribute) {
        MyCdiContext.checkNotAvailable(cdiContext, beanInstantiator);
        return attribute == null ? null : attribute.getContent();
    }

    @Override
    public MyDataNotRequiringCDI convertToEntityAttribute(String dbData) {
        MyCdiContext.checkNotAvailable(cdiContext, beanInstantiator);
        return dbData == null ? null : new MyDataNotRequiringCDI(dbData);
    }
}
