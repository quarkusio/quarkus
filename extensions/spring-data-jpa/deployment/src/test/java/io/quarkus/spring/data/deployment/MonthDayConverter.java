package io.quarkus.spring.data.deployment;

import java.time.MonthDay;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MonthDayConverter implements AttributeConverter<MonthDay, String> {

    @Override
    public String convertToDatabaseColumn(MonthDay attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public MonthDay convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MonthDay.parse(dbData);
    }
}
