package io.quarkus.spring.data.deployment;

import java.util.Locale;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.LocaleJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Workaround for https://hibernate.atlassian.net/browse/HHH-17466
 */
public class FixedLocaleJavaType extends LocaleJavaType {
    public FixedLocaleJavaType() {
        super();
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return indicators.getJdbcType(SqlTypes.VARCHAR);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrap(Locale value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (Locale.class.isAssignableFrom(type)) {
            return (X) value;
        }
        return super.unwrap(value, type, options);
    }

    @Override
    public <X> Locale wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (value instanceof Locale) {
            return (Locale) value;
        }
        return super.wrap(value, options);
    }

}
