package io.quarkus.it.jpa.h2.generics;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@IdClass(IntermediateAbstractMapped.PK.class)
public abstract class IntermediateAbstractMapped<T> extends AbstractGenericMappedSuperType<T> {

    @Id
    private String keyOne;
    @Id
    private String keyTwo;
    @Id
    private String keyThree;

    @SuppressWarnings("UnusedDeclaration")
    public static final class PK implements Serializable {

        private String keyOne;
        private String keyTwo;
        private String keyThree;

        public PK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PK pk = (PK) o;
            return Objects.equals(keyOne, pk.keyOne) &&
                    Objects.equals(keyTwo, pk.keyTwo) &&
                    Objects.equals(keyThree, pk.keyThree);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyOne, keyTwo, keyThree);
        }

    }

}
