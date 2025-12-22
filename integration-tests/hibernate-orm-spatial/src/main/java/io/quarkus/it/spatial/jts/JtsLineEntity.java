package io.quarkus.it.spatial.jts;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.locationtech.jts.geom.LineString;

@Entity
public class JtsLineEntity {
    @Id
    private Long id;

    private LineString line;

    public JtsLineEntity() {
    }

    public JtsLineEntity(Long id, LineString line) {
        this.id = id;
        this.line = line;
    }
}
