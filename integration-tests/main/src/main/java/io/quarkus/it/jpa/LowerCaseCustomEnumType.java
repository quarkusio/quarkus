package io.quarkus.it.jpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class LowerCaseCustomEnumType extends AbstractCustomUserType<CustomEnum> {

    public LowerCaseCustomEnumType() {
        super(CustomEnum.class);
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.VARCHAR };
    }

    @Override
    public CustomEnum get(ResultSet result, String[] names, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String val = result.getString(names[0]);
        return val != null ? CustomEnum.valueOf(val.toUpperCase()) : null;
    }

    @Override
    public void set(PreparedStatement preparedStatement, CustomEnum value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.BIGINT);
        } else {
            preparedStatement.setString(index, value.name().toLowerCase());
        }
    }

}
