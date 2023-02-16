package io.quarkus.hibernate.orm.devconsole;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class TypeWithUnsupportedSqlCode implements UserType<String> {

    public static final int UNSUPPORTED_SQL_CODE = Integer.MAX_VALUE;

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, String value, int index,
            SharedSessionContractImplementor session) {
        throw new UnsupportedOperationException("Should not be called - this type is not used at runtime");
    }

    @Override
    public int getSqlType() {
        return UNSUPPORTED_SQL_CODE;
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String o, String o1) {
        return Objects.equals(o, o1);
    }

    @Override
    public int hashCode(String o) {
        return o.hashCode();
    }

    @Override
    public String nullSafeGet(ResultSet resultSet, int i, SharedSessionContractImplementor sharedSessionContractImplementor,
            Object o) {
        throw new UnsupportedOperationException("Should not be called - this type is not used at runtime");
    }

    @Override
    public String deepCopy(String o) {
        return o;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(String o) {
        return (Serializable) o;
    }

    @Override
    public String assemble(Serializable cached, Object o) {
        return (String) cached;
    }

    @Override
    public String replace(String o, String t, Object owner) {
        return o;
    }
}
