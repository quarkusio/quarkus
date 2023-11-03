package io.quarkus.it.jpa.attributeconverter;

import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MyDataNotRequiringCDIConverter implements AttributeConverter<MyDataNotRequiringCDI, String> {
    // This will always be null.
    // It's only here to check that this class is instantiated without relying on CDI
    // (because we want to test non-CDI attribute converters too).
    @Inject
    MyCdiContext cdiContext;

    @Override
    public String convertToDatabaseColumn(MyDataNotRequiringCDI attribute) {
        MyCdiContext.checkNotAvailable(cdiContext);
        return attribute == null ? null : attribute.getContent();
    }

    @Override
    public MyDataNotRequiringCDI convertToEntityAttribute(String dbData) {
        MyCdiContext.checkNotAvailable(cdiContext);
        return dbData == null ? null : new MyDataNotRequiringCDI(dbData);
    }
}
