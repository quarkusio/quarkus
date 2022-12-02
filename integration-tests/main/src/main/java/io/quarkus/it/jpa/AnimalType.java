package io.quarkus.it.jpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class AnimalType extends AbstractCustomUserType<Animal> {

    public AnimalType() {
        super(Animal.class);
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.DOUBLE };
    }

    @Override
    public Animal get(ResultSet result, String[] names, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        double weight = result.getDouble(names[0]);
        Animal animal = new Animal();
        animal.setWeight(weight);
        return animal;
    }

    @Override
    public void set(PreparedStatement preparedStatement, Animal value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.BIGINT);
        } else {
            preparedStatement.setDouble(index, value.getWeight());
        }
    }

}
