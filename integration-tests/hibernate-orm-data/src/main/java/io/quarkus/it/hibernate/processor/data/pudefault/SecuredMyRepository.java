package io.quarkus.it.hibernate.processor.data.pudefault;

import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

import org.hibernate.annotations.processing.Pattern;

import io.quarkus.it.hibernate.processor.data.security.CanWrite1;
import io.quarkus.it.hibernate.processor.data.security.CanWrite2;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;

@Repository
public interface SecuredMyRepository extends CrudRepository<MyEntity, Integer> {

    @RolesAllowed("root")
    @Insert
    void insertRootRole(MyEntity entity);

    @RolesAllowed("admin")
    @Insert
    void insertAdminRole(MyEntity entity);

    @DenyAll
    @Insert
    void insertDenyAll(MyEntity entity);

    @Authenticated
    @Insert
    void insertAuthenticated(MyEntity entity);

    @RolesAllowed("${donald}")
    @Find
    Stream<MyEntity> findAllForDonald(Order<MyEntity> order);

    @PermissionsAllowed("find-all")
    @PermissionsAllowed("find-for-donald")
    @Find
    Stream<MyEntity> findAllForDonald(@Pattern String name, Sort<MyEntity> sort);

    // public, unannotated
    @Find
    Stream<MyEntity> findAllForDonald(Sort<MyEntity> order, @Pattern String name);

    @RolesAllowed("${george}")
    @Find
    Stream<MyEntity> findAllForGeorge(Order<MyEntity> order);

    @Query("select e from MyEntity e where e.name like :name")
    List<MyEntity> findByName(String name);

    @Delete
    void delete(String name);

    @PermissionsAllowed("rename-1")
    @Update
    void rename1(MyEntity entity);

    @PermissionsAllowed("rename-2")
    @Update
    void rename2(MyEntity entity);

    @PermissionsAllowed("rename-1")
    @PermissionsAllowed("rename-2")
    @Update
    void renameAll1And2(List<MyEntity> entities);

    @PermissionsAllowed("rename-2")
    @PermissionsAllowed("rename-3")
    @Update
    void renameAll2And3(List<MyEntity> entities);

    @PermissionsAllowed("rename-overloaded")
    @Update
    void renameOverloaded(List<MyEntity> entities);

    @Update
    void renameOverloaded(MyEntity entity);

    @CanWrite1
    @Insert
    void insertAll1(List<MyEntity> entities);

    @CanWrite2
    @Insert
    void insertAll2(List<MyEntity> entities);

}
