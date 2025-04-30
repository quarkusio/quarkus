package io.quarkus.it.hibertnate.orm.graphql.panache;

import java.io.Serial;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A "correct" local date JDBC type.
 */
public final class LocalDateJdbcType implements JdbcType {
    @Serial
    private static final long serialVersionUID = -3375371178728213643L;

    public static final LocalDateJdbcType INSTANCE = new LocalDateJdbcType();

    private LocalDateJdbcType() {
    }

    @Override
    public int getJdbcTypeCode() {
        return Types.DATE;
    }

    @Override
    public String getFriendlyName() {
        return "DATE";
    }

    @Override
    public String toString() {
        return "LocalDateTypeDescriptor";
    }

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
            Integer length,
            Integer scale,
            TypeConfiguration typeConfiguration) {
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(LocalDate.class);
    }

    @Override
    public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
        return new JdbcLiteralFormatterTemporal<>(javaType, TemporalType.DATE);
    }

    @Override
    public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
        return LocalDate.class;
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setObject(index, javaType.unwrap(value, LocalDate.class, options));
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                st.setObject(name, javaType.unwrap(value, LocalDate.class, options));
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
        return new BasicExtractor<>(javaType, this) {
            @Override
            protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                return javaType.wrap(rs.getObject(paramIndex, LocalDate.class), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                return javaType.wrap(statement.getObject(index, LocalDate.class), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                return javaType.wrap(statement.getObject(name, LocalDate.class), options);
            }
        };
    }
}
