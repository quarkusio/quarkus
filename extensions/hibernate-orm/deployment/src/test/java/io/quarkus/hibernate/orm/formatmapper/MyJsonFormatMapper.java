package io.quarkus.hibernate.orm.formatmapper;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

import io.quarkus.hibernate.orm.JsonFormat;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;

@JsonFormat
@PersistenceUnitExtension
public class MyJsonFormatMapper implements FormatMapper {

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        throw new UnsupportedOperationException();
    }
}
