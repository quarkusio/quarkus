package io.quarkus.hibernate.orm.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevController;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfo;

@Path("/dev-info")
public class HibernateOrmDevInfoServiceTestResource {

    @GET
    @Path("/check-pu-info-with-successful-ddl-generation")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkPuInfoWithSuccessfulDDLGeneration() {
        HibernateOrmDevInfo infos = HibernateOrmDevController.get().getInfo();

        assertThat(infos.getNumberOfEntities()).isEqualTo(1);
        assertThat(infos.getNumberOfNamedQueries()).isEqualTo(2);

        Collection<HibernateOrmDevInfo.PersistenceUnit> pus = infos.getPersistenceUnits();
        assertThat(pus).hasSize(1);
        HibernateOrmDevInfo.PersistenceUnit pu = pus.iterator().next();

        assertThat(pu.getName()).isEqualTo(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);

        assertThat(pu.getManagedEntities())
                .hasSize(1)
                .element(0)
                .returns(MyEntityWithSuccessfulDDLGeneration.class.getName(),
                        HibernateOrmDevInfo.Entity::getClassName)
                .returns(MyEntityWithSuccessfulDDLGeneration.TABLE_NAME,
                        HibernateOrmDevInfo.Entity::getTableName);

        assertThat(pu.getNamedQueries())
                .hasSize(1)
                .element(0)
                .returns("MyEntity.findAll", HibernateOrmDevInfo.Query::getName)
                .returns("SELECT e FROM MyEntity e ORDER BY e.name", HibernateOrmDevInfo.Query::getQuery)
                .returns(true, HibernateOrmDevInfo.Query::isCacheable);
        assertThat(pu.getNamedNativeQueries())
                .hasSize(1)
                .element(0)
                .returns("MyEntity.nativeFindAll", HibernateOrmDevInfo.Query::getName)
                .returns("SELECT e FROM MyEntityTable e ORDER BY e.name",
                        HibernateOrmDevInfo.Query::getQuery)
                .returns(false, HibernateOrmDevInfo.Query::isCacheable);

        assertThat(pu.getCreateDDL())
                .contains("create table MyEntityTable")
                .contains("INSERT INTO MyEntityTable(id, name) VALUES(1, 'default sql load script entity');");
        assertThat(pu.getDropDDL())
                .contains("drop table if exists MyEntityTable");

        return "OK";
    }

    @GET
    @Path("/check-pu-info-with-failing-ddl-generation")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkPuInfoWithFailingDDLGeneration() {
        HibernateOrmDevInfo infos = HibernateOrmDevController.get().getInfo();

        Collection<HibernateOrmDevInfo.PersistenceUnit> pus = infos.getPersistenceUnits();
        assertThat(pus).hasSize(1);
        HibernateOrmDevInfo.PersistenceUnit pu = pus.iterator().next();

        // We have some information available
        assertThat(pu.getName()).isEqualTo(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
        assertThat(pu.getManagedEntities())
                .hasSize(1)
                .element(0)
                .returns(MyEntityWithFailingDDLGeneration.class.getName(),
                        HibernateOrmDevInfo.Entity::getClassName)
                .returns(MyEntityWithFailingDDLGeneration.TABLE_NAME,
                        HibernateOrmDevInfo.Entity::getTableName);

        assertThat(pu.getCreateDDL())
                .contains("MyEntityTable")
                .contains(TypeWithUnsupportedSqlCode.UNSUPPORTED_SQL_CODE + " (UNKNOWN("
                        + TypeWithUnsupportedSqlCode.UNSUPPORTED_SQL_CODE + "))");
        // Drop script generation doesn't involve column types, so it didn't fail
        assertThat(pu.getDropDDL())
                .contains("drop table if exists MyEntityTable");

        return "OK";
    }
}
