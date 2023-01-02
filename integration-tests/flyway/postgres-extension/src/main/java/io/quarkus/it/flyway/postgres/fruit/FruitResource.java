package io.quarkus.it.flyway.postgres.fruit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/")
public class FruitResource {

    @Inject
    EntityManager entityManager;

    @GET
    @Path("fruits")
    public Fruit[] getDefault() {
        return get();
    }

    private Fruit[] get() {
        return entityManager.createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList().toArray(new Fruit[0]);
    }




    @GET
    @Path("fruits/index")
    public List getIndexNames() {
        return entityManager.createNativeQuery("select" +
                "    t.relname as table_name, " +
                "    i.relname as index_name, " +
                "    a.attname as column_name " +
                "from " +
                "    pg_class t, " +
                "    pg_class i, " +
                "    pg_index ix, " +
                "    pg_attribute a " +
                "where " +
                "    t.oid = ix.indrelid " +
                "    and i.oid = ix.indexrelid " +
                "    and a.attrelid = t.oid " +
                "    and a.attnum = ANY(ix.indkey) " +
                "    and t.relkind = 'r' " +
                "    and t.relname like 'known_fruits' " +
                "order by " +
                "    t.relname, " +
                "    i.relname;").getResultList();
    }

}
