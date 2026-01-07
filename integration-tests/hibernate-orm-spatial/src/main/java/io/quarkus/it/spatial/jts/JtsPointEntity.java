package io.quarkus.it.spatial.jts;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.locationtech.jts.geom.Point;

@Entity
public class JtsPointEntity {
    @Id
    private Long id;

    private Point point;

    public JtsPointEntity() {
    }

    public JtsPointEntity(Long id, Point point) {
        this.id = id;
        this.point = point;
    }
}
