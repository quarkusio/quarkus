package io.quarkus.it.spatial.geolatte;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.geolatte.geom.G2D;
import org.geolatte.geom.LineString;

@Entity
public class GeolatteLineEntity {
    @Id
    private Long id;

    private LineString<G2D> line;

    public GeolatteLineEntity() {
    }

    public GeolatteLineEntity(Long id, LineString<G2D> line) {
        this.id = id;
        this.line = line;
    }
}
