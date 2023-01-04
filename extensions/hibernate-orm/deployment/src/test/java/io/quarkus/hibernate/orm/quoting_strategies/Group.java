package io.quarkus.hibernate.orm.quoting_strategies;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Table with reserved name and containing one column with reserved name and column definition.
 *
 * @see <a href="http://www.h2database.com/html/advanced.html#keywords">H2 Documentation, section
 *      "Keywords / Reserved Words"</a>
 */
@Entity
@Table(name = "group")
public class Group {

    private Long id;

    private String name;

    private String value;

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

    @Column(columnDefinition = "varchar(255)")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
