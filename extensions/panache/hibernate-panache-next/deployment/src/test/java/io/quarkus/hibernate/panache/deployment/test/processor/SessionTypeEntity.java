package io.quarkus.hibernate.panache.deployment.test.processor;

import java.util.List;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@Entity
public class SessionTypeEntity extends PanacheEntity {

    public String field;

    public interface ManagedBlockingRepo extends PanacheRepository<SessionTypeEntity> {
        @Find
        List<SessionTypeEntity> all();

        @org.hibernate.annotations.processing.Find
        List<SessionTypeEntity> allHibernate();

        @Query("from SessionTypeEntity")
        List<SessionTypeEntity> allQuery();

        @HQL("from SessionTypeEntity")
        List<SessionTypeEntity> allHQL();

        @SQL("select id from SessionTypeEntity")
        List<Long> allSQL();

        @Delete
        long myDeleteAll();

        // Requires JD 1.1
        //        @Persist
        //        void myPersist(SessionTypeEntity entity);
        //        @Remove
        //        void myRemove(SessionTypeEntity entity);
        //        @Detach
        //        void myDetach(SessionTypeEntity entity);
        //        @Merge
        //        void myMerge(SessionTypeEntity entity);
        //        @Refresh
        //        void myRefresh(SessionTypeEntity entity);
    }

    public interface StatelessBlockingRepo extends PanacheRepository.Stateless<SessionTypeEntity, Long> {
        @Find
        List<SessionTypeEntity> all();

        @org.hibernate.annotations.processing.Find
        List<SessionTypeEntity> allHibernate();

        @Query("from SessionTypeEntity")
        List<SessionTypeEntity> allQuery();

        @HQL("from SessionTypeEntity")
        List<SessionTypeEntity> allHQL();

        @SQL("select id from SessionTypeEntity")
        List<Long> allSQL();

        @Delete
        long myDeleteAll();

        @Insert
        void myInsert(SessionTypeEntity entity);

        @Update
        void myUpdate(SessionTypeEntity entity);

        @Delete
        void myDelete(SessionTypeEntity entity);

        @Save
        void mySave(SessionTypeEntity entity);
    }

    public interface ManagedReactiveRepo extends PanacheRepository.Reactive<SessionTypeEntity, Long> {
        @Find
        Uni<List<SessionTypeEntity>> all();

        @org.hibernate.annotations.processing.Find
        Uni<List<SessionTypeEntity>> allHibernate();

        @Query("from SessionTypeEntity")
        Uni<List<SessionTypeEntity>> allQuery();

        @HQL("from SessionTypeEntity")
        Uni<List<SessionTypeEntity>> allHQL();

        @SQL("select id from SessionTypeEntity")
        Uni<List<Long>> allSQL();

        @Delete
        Uni<Integer> myDeleteAll();

        // Requires JD 1.1
        //        @Persist
        //        Uni<Void> myPersist(SessionTypeEntity entity);
        //        @Remove
        //        Uni<Void> myRemove(SessionTypeEntity entity);
        //        @Detach
        //        Uni<Void> myDetach(SessionTypeEntity entity);
        //        @Merge
        //        Uni<Void> myMerge(SessionTypeEntity entity);
        //        @Refresh
        //        Uni<Void> myRefresh(SessionTypeEntity entity);
    }

    public interface StatelessReactiveRepo extends PanacheRepository.Reactive.Stateless<SessionTypeEntity, Long> {
        @Find
        Uni<List<SessionTypeEntity>> all();

        @org.hibernate.annotations.processing.Find
        Uni<List<SessionTypeEntity>> allHibernate();

        @Query("from SessionTypeEntity")
        Uni<List<SessionTypeEntity>> allQuery();

        @HQL("from SessionTypeEntity")
        Uni<List<SessionTypeEntity>> allHQL();

        @SQL("select id from SessionTypeEntity")
        Uni<List<Long>> allSQL();

        @Delete
        Uni<Integer> myDeleteAll();

        @Insert
        Uni<Void> myInsert(SessionTypeEntity entity);

        @Update
        Uni<Void> myUpdate(SessionTypeEntity entity);

        @Delete
        Uni<Void> myDelete(SessionTypeEntity entity);

        @Save
        Uni<Void> mySave(SessionTypeEntity entity);
    }
}
