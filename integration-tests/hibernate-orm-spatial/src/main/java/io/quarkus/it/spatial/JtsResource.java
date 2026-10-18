package io.quarkus.it.spatial;

import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import io.quarkus.it.spatial.jts.JtsLineEntity;
import io.quarkus.it.spatial.jts.JtsPointEntity;
import io.quarkus.it.spatial.jts.JtsPolygonEntity;
import io.quarkus.runtime.StartupEvent;

@Path("/jts")
public class JtsResource extends AbstractSpatialResource {
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private final Polygon jtsPolygon = geometryFactory.createPolygon(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(10, 0),
            new Coordinate(10, 10),
            new Coordinate(0, 10),
            new Coordinate(0, 0)
    });

    @Transactional
    public void startup(@Observes final StartupEvent startupEvent) {
        // persist some jts geometry entities
        em.persist(new JtsPointEntity(4L, geometryFactory.createPoint(new Coordinate(5, 5))));
        em.persist(new JtsLineEntity(5L, geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(5, 0),
                new Coordinate(5, 5)
        })));
        em.persist(new JtsPolygonEntity(6L, jtsPolygon));
    }

    @Override
    protected Object getPolygon() {
        return jtsPolygon;
    }

    @Override
    protected String getPointEntityName() {
        return JtsPointEntity.class.getSimpleName();
    }

    @Override
    protected String getLineEntityName() {
        return JtsLineEntity.class.getSimpleName();
    }

    @Override
    protected String getPolygonEntityName() {
        return JtsPolygonEntity.class.getSimpleName();
    }
}
