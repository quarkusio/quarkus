package io.quarkus.it.jpa.attributeconverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
@ApplicationScoped
public class MyDataRequiringCDIConverter implements AttributeConverter<MyDataRequiringCDI, String> {
    @Inject
    MyCdiContext cdiContext;

    @Override
    public String convertToDatabaseColumn(MyDataRequiringCDI attribute) {
        MyCdiContext.checkAvailable(cdiContext);
        return attribute == null ? null : attribute.getContent();
    }

    @Override
    public MyDataRequiringCDI convertToEntityAttribute(String dbData) {
        MyCdiContext.checkAvailable(cdiContext);
        return dbData == null ? null : new MyDataRequiringCDI(dbData);
    }
}
