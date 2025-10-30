package io.quarkus.it.spatial.jts;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.locationtech.jts.geom.Polygon;

@Entity
public class JtsPolygonEntity {
    @Id
    private Long id;

    private Polygon polygon;

    public JtsPolygonEntity() {
    }

    public JtsPolygonEntity(Long id, Polygon polygon) {
        this.id = id;
        this.polygon = polygon;
    }
}
