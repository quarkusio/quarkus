package io.quarkus.it.jpa;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class BigIntType extends AbstractCustomUserType<BigInteger> {

    public BigIntType() {
        super(BigInteger.class);
    }

    @Override
    public int getSqlType() {
        return Types.BIGINT;
    }

    @Override
    public BigInteger nullSafeGet(ResultSet result, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        BigDecimal bigDecimal = result.getBigDecimal(position);
        return bigDecimal != null ? bigDecimal.toBigIntegerExact() : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, BigInteger value, int index,
            SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.BIGINT);
        } else {
            preparedStatement.setBigDecimal(index, new BigDecimal(String.valueOf(value)));
        }
    }
}
