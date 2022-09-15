package io.quarkus.hibernate.orm.quoted_indentifiers;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Table with reserved name.
 * <p>
 * http://www.h2database.com/html/advanced.html section "Keywords / Reserved Words".
 */
@Entity
@Table(name = "group")
public class Group {

    private Long id;

    private String name;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "groupSeq")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
