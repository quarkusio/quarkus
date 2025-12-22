package io.quarkus.it.spatial.geolatte;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Polygon;

@Entity
public class GeolattePolygonEntity {
    @Id
    private Long id;

    private Polygon<G2D> polygon;

    public GeolattePolygonEntity() {
    }

    public GeolattePolygonEntity(Long id, Polygon<G2D> polygon) {
        this.id = id;
        this.polygon = polygon;
    }
}
