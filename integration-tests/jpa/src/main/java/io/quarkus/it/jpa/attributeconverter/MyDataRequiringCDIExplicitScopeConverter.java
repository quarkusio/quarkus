package io.quarkus.it.jpa.attributeconverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
@ApplicationScoped
public class MyDataRequiringCDIExplicitScopeConverter implements AttributeConverter<MyDataRequiringCDI, String> {
    @Inject
    MyCdiContext cdiContext;

    @Override
    public String convertToDatabaseColumn(MyDataRequiringCDI attribute) {
        if (attribute == null) {
            return null;
        }
        MyCdiContext.checkAvailable(cdiContext);
        return attribute.getContent();
    }

    @Override
    public MyDataRequiringCDI convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        MyCdiContext.checkAvailable(cdiContext);
        return new MyDataRequiringCDI(dbData);
    }
}
