package io.quarkus.hibernate.orm.panache;

import java.util.Objects;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * <p>
 * Represents an entity with a generated ID field {@link #id} of type {@link Long}. If your
 * Hibernate entities extend this class they gain the ID field and auto-generated accessors
 * to all their public fields (unless annotated with {@link Transient}), as well as all
 * the useful methods from {@link PanacheEntityBase}.
 * </p>
 * <p>
 * If you want a custom ID type or strategy, you can directly extend {@link PanacheEntityBase}
 * instead, and write your own ID field. You will still get auto-generated accessors and
 * all the useful methods.
 * </p>
 *
 * @author Stéphane Épardaud
 * @see PanacheEntityBase
 */
@MappedSuperclass
public abstract class PanacheEntity extends PanacheEntityBase {

    /**
     * The auto-generated ID field. This field is set by Hibernate ORM when this entity
     * is persisted.
     *
     * @see #persist()
     */
    @Id
    @GeneratedValue
    public Long id;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "<" + id + ">";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (Objects.isNull(object) || getClass() != object.getClass()) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }
        PanacheEntity that = (PanacheEntity) object;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    // FIXME: VERSION?
}
