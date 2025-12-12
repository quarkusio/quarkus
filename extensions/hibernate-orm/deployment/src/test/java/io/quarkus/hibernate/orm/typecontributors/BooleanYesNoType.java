package io.quarkus.hibernate.orm.typecontributors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

final class BooleanYesNoType implements UserType<Boolean> {

    @Override
    public int getSqlType() {
        return SqlTypes.VARCHAR;
    }

    @Override
    public Class<Boolean> returnedClass() {
        return Boolean.class;
    }

    @Override
    public Boolean nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        String value = rs.getString(position);

        if (value == null) {
            return null;
        }

        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Boolean value, int position, WrapperOptions options) throws SQLException {
        if (value == null) {
            st.setNull(position, SqlTypes.VARCHAR);
        } else {
            st.setString(position, value ? "Y" : "N");
        }
    }

    @Override
    public Boolean deepCopy(Boolean value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
