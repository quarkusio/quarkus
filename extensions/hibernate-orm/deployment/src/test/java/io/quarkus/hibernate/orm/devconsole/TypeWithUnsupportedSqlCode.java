package io.quarkus.hibernate.orm.devconsole;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class TypeWithUnsupportedSqlCode implements UserType {

    public static final int UNSUPPORTED_SQL_CODE = Types.ARRAY;

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index,
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
    public boolean equals(Object o, Object o1) {
        return Objects.equals(o, o1);
    }

    @Override
    public int hashCode(Object o) {
        return o.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, int i, SharedSessionContractImplementor sharedSessionContractImplementor,
            Object o) {
        throw new UnsupportedOperationException("Should not be called - this type is not used at runtime");
    }

    @Override
    public Object deepCopy(Object o) {
        return o;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object o) {
        return (Serializable) o;
    }

    @Override
    public Object assemble(Serializable cached, Object o) {
        return cached;
    }

    @Override
    public Object replace(Object o, Object t, Object owner) {
        return o;
    }
}
