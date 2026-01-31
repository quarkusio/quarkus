package io.quarkus.it.spatial.geolatte;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;

@Entity
public class GeolattePointEntity {
    @Id
    private Long id;

    private Point<G2D> point;

    public GeolattePointEntity() {
    }

    public GeolattePointEntity(Long id, Point<G2D> point) {
        this.id = id;
        this.point = point;
    }
}
