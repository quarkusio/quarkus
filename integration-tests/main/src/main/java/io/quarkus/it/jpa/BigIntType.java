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
    public int[] sqlTypes() {
        return new int[] { Types.BIGINT };
    }

    @Override
    public BigInteger get(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        BigDecimal bigDecimal = rs.getBigDecimal(names[0]);
        return bigDecimal != null ? bigDecimal.toBigIntegerExact() : null;
    }

    @Override
    public void set(PreparedStatement st, BigInteger value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.BIGINT);
        } else {
            st.setBigDecimal(index, new BigDecimal(String.valueOf(value)));
        }
    }
}
