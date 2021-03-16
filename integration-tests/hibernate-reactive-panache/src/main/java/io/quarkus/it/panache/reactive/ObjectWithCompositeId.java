package io.quarkus.it.panache.reactive;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@IdClass(ObjectWithCompositeId.ObjectKey.class)
public class ObjectWithCompositeId extends PanacheEntityBase {
    @Id
    public String part1;
    @Id
    public String part2;
    public String description;

    static class ObjectKey implements Serializable {
        private String part1;
        private String part2;

        public ObjectKey() {
        }

        public ObjectKey(String part1, String part2) {
            this.part1 = part1;
            this.part2 = part2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ObjectKey objectKey = (ObjectKey) o;
            return part1.equals(objectKey.part1) &&
                    part2.equals(objectKey.part2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(part1, part2);
        }
    }
}
