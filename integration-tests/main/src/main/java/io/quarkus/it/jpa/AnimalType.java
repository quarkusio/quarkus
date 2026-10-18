package io.quarkus.it.jpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.WrapperOptions;

public class AnimalType extends AbstractCustomUserType<Animal> {

    public AnimalType() {
        super(Animal.class);
    }

    @Override
    public int getSqlType() {
        return Types.DOUBLE;
    }

    @Override
    public Animal nullSafeGet(ResultSet result, int position, WrapperOptions options)
            throws SQLException {
        double weight = result.getDouble(position);
        Animal animal = new Animal();
        animal.setWeight(weight);
        return animal;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Animal value, int index,
            WrapperOptions options)
            throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.BIGINT);
        } else {
            preparedStatement.setDouble(index, value.getWeight());
        }
    }
}
