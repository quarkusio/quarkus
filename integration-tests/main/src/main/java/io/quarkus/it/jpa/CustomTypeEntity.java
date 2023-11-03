package io.quarkus.it.jpa;

import java.math.BigInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.Type;

@Entity
public class CustomTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eventSeq")
    private Long id;

    @Column(name = "big_integer")
    @Type(BigIntType.class)
    private BigInteger bigInteger;

    @Column(name = "custom_enum")
    @Type(LowerCaseCustomEnumType.class)
    private CustomEnum customEnum;

    @Column(name = "animal")
    @Type(AnimalType.class)
    private Animal animal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public CustomEnum getCustomEnum() {
        return customEnum;
    }

    public void setCustomEnum(CustomEnum customEnum) {
        this.customEnum = customEnum;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }
}
