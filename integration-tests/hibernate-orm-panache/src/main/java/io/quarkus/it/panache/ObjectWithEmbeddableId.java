package io.quarkus.it.panache;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class ObjectWithEmbeddableId extends PanacheEntityBase {
    @EmbeddedId
    public ObjectKey key;
    public String description;

    @Embeddable
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
