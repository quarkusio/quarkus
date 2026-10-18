package io.quarkus.it.spatial;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.linestring;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Polygon;

import io.quarkus.it.spatial.geolatte.GeolatteLineEntity;
import io.quarkus.it.spatial.geolatte.GeolattePointEntity;
import io.quarkus.it.spatial.geolatte.GeolattePolygonEntity;
import io.quarkus.runtime.StartupEvent;

@Path("/geolatte")
public class GeolatteResource extends AbstractSpatialResource {
    private final Polygon<G2D> geolattePolygon = polygon(WGS84, ring(
            g(0, 0),
            g(10, 0),
            g(10, 10),
            g(0, 10),
            g(0, 0)));

    @Transactional
    public void startup(@Observes final StartupEvent startupEvent) {
        // persist some geolatte geometry entities
        em.persist(new GeolattePointEntity(1L, point(WGS84, g(5, 5))));
        em.persist(new GeolatteLineEntity(2L, linestring(WGS84, g(0, 0), g(5, 0), g(5, 5))));
        em.persist(new GeolattePolygonEntity(3L, geolattePolygon));
    }

    @Override
    protected Object getPolygon() {
        return geolattePolygon;
    }

    @Override
    protected String getPointEntityName() {
        return GeolattePointEntity.class.getSimpleName();
    }

    @Override
    protected String getLineEntityName() {
        return GeolatteLineEntity.class.getSimpleName();
    }

    @Override
    protected String getPolygonEntityName() {
        return GeolattePolygonEntity.class.getSimpleName();
    }
}
