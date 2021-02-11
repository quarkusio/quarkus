package io.quarkus.it.jpa;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public abstract class AbstractCustomUserType<T> implements UserType {
    Class<T> clazz;

    public AbstractCustomUserType(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object nullSafeGet(ResultSet result, String[] names, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        return get(result, names, session, owner);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index,
            SharedSessionContractImplementor session) throws SQLException {
        set(preparedStatement, clazz.cast(value), index, session);
    }

    protected abstract T get(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws SQLException;

    protected abstract void set(PreparedStatement st, T value, int index, SharedSessionContractImplementor session)
            throws SQLException;

    @Override
    public Class returnedClass() {
        return clazz;
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
