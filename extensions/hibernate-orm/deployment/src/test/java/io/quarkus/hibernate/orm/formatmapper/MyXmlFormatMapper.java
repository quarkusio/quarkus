package io.quarkus.hibernate.orm.formatmapper;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.XmlFormat;

@XmlFormat
@PersistenceUnitExtension
public class MyXmlFormatMapper implements FormatMapper {

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        throw new UnsupportedOperationException();
    }
}
