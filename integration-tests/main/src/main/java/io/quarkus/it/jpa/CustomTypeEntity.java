package io.quarkus.it.jpa;

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@TypeDef(typeClass = LowerCaseCustomEnumType.class, name = "lowerCaseEnumType")
@TypeDefs({
        @TypeDef(typeClass = AnimalType.class, name = "animalType")
})
public class CustomTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eventSeq")
    private Long id;

    @Column(name = "big_integer")
    @Type(type = "io.quarkus.it.jpa.BigIntType")
    private BigInteger bigInteger;

    @Column(name = "custom_enum")
    @Type(type = "lowerCaseEnumType")
    private CustomEnum customEnum;

    @Column(name = "animal")
    @Type(type = "animalType")
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
