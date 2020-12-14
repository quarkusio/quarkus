package io.quarkus.it.jpa.attributeconverter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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
