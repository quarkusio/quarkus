package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.attributeconverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;

@ApplicationScoped
public class MyConverter implements AttributeConverter<MyData, String> {
    @Inject
    MyCdiContext context;

    @Override
    public String convertToDatabaseColumn(MyData attribute) {
        MyCdiContext.checkAvailable(context);
        return attribute == null ? null : attribute.value;
    }

    @Override
    public MyData convertToEntityAttribute(String dbData) {
        MyCdiContext.checkAvailable(context);
        return dbData == null ? null : new MyData(dbData);
    }
}
