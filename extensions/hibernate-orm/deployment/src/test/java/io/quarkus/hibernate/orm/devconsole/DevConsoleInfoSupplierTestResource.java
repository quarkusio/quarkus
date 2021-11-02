package io.quarkus.hibernate.orm.devconsole;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.devconsole.HibernateOrmDevConsoleInfoSupplier;

@Path("/dev-console-info-supplier")
public class DevConsoleInfoSupplierTestResource {

    @GET
    @Path("/check-pu-info-with-successful-ddl-generation")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkPuInfoWithSuccessfulDDLGeneration() {
        HibernateOrmDevConsoleInfoSupplier supplier = new HibernateOrmDevConsoleInfoSupplier();
        HibernateOrmDevConsoleInfoSupplier.PersistenceUnitsInfo infos = supplier.get();

        assertThat(infos.getNumberOfEntities()).isEqualTo(1);
        assertThat(infos.getNumberOfNamedQueries()).isEqualTo(2);

        Collection<HibernateOrmDevConsoleInfoSupplier.PersistenceUnitInfo> pus = infos.getPersistenceUnits();
        assertThat(pus).hasSize(1);
        HibernateOrmDevConsoleInfoSupplier.PersistenceUnitInfo pu = pus.iterator().next();

        assertThat(pu.getName()).isEqualTo(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);

        assertThat(pu.getManagedEntities())
                .hasSize(1)
                .element(0)
                .returns(MyEntityWithSuccessfulDDLGeneration.class.getName(),
                        HibernateOrmDevConsoleInfoSupplier.EntityInfo::getClassName)
                .returns(MyEntityWithSuccessfulDDLGeneration.TABLE_NAME,
                        HibernateOrmDevConsoleInfoSupplier.EntityInfo::getTableName);

        assertThat(pu.getNamedQueries())
                .hasSize(1)
                .element(0)
                .returns("MyEntity.findAll", HibernateOrmDevConsoleInfoSupplier.QueryInfo::getName)
                .returns("SELECT e FROM MyEntity e ORDER BY e.name", HibernateOrmDevConsoleInfoSupplier.QueryInfo::getQuery)
                .returns(true, HibernateOrmDevConsoleInfoSupplier.QueryInfo::isCacheable);
        assertThat(pu.getNamedNativeQueries())
                .hasSize(1)
                .element(0)
                .returns("MyEntity.nativeFindAll", HibernateOrmDevConsoleInfoSupplier.QueryInfo::getName)
                .returns("SELECT e FROM MyEntityTable e ORDER BY e.name",
                        HibernateOrmDevConsoleInfoSupplier.QueryInfo::getQuery)
                .returns(false, HibernateOrmDevConsoleInfoSupplier.QueryInfo::isCacheable);

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
        HibernateOrmDevConsoleInfoSupplier supplier = new HibernateOrmDevConsoleInfoSupplier();
        HibernateOrmDevConsoleInfoSupplier.PersistenceUnitsInfo infos = supplier.get();

        Collection<HibernateOrmDevConsoleInfoSupplier.PersistenceUnitInfo> pus = infos.getPersistenceUnits();
        assertThat(pus).hasSize(1);
        HibernateOrmDevConsoleInfoSupplier.PersistenceUnitInfo pu = pus.iterator().next();

        // We have some information available
        assertThat(pu.getName()).isEqualTo(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
        assertThat(pu.getManagedEntities())
                .hasSize(1)
                .element(0)
                .returns(MyEntityWithFailingDDLGeneration.class.getName(),
                        HibernateOrmDevConsoleInfoSupplier.EntityInfo::getClassName)
                .returns(MyEntityWithFailingDDLGeneration.TABLE_NAME,
                        HibernateOrmDevConsoleInfoSupplier.EntityInfo::getTableName);

        // But some DDL scripts are replaced with whatever exception occurred
        assertThat(pu.getCreateDDL())
                .contains("Could not generate DDL")
                .contains("org.hibernate.MappingException: No Dialect mapping for JDBC type: "
                        + TypeWithUnsupportedSqlCode.UNSUPPORTED_SQL_CODE);
        // Drop script generation doesn't involve column types, so it didn't fail
        assertThat(pu.getDropDDL())
                .contains("drop table if exists MyEntityTable");

        return "OK";
    }
}
