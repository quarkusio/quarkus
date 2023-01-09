package io.quarkus.it.hibernate.reactive.postgresql;

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

/**
 * We want to check that the right {@link org.hibernate.hql.spi.id.MultiTableBulkIdStrategy}
 * is set when using Hibernate Reactive.
 *
 * @see io.quarkus.hibernate.reactive.runtime.boot.FastBootReactiveEntityManagerFactoryBuilder
 * @see org.hibernate.reactive.bulk.impl.ReactiveBulkIdStrategy
 */
@Path("/hr-joinedsubclass")
public class HibernateReactiveTestEndpointJoinedSubclass {

    @Inject
    Mutiny.Session session;

    @DELETE
    @Path("/deleteBook/{bookId}")
    public Uni<Book> deleteBook(@PathParam("bookId") Integer bookId) {
        return session.withTransaction(tx -> session
                .createQuery("delete BookJS where id=:id")
                .setParameter("id", bookId)
                .executeUpdate())
                .chain(() -> session.find(SpellBook.class, bookId));
    }

    @POST
    @Path("/prepareDb")
    public Uni<Void> prepareDb() {
        final SpellBook spells = new SpellBook(6, "Necronomicon", true);

        return session.persist(spells)
                .chain(session::flush);
    }

    @Entity(name = "SpellBookJS")
    @Table(name = "SpellBookJS")
    @DiscriminatorValue("S")
    public static class SpellBook extends Book {

        private boolean forbidden;

        public SpellBook(Integer id, String title, boolean forbidden) {
            super(id, title);
            this.forbidden = forbidden;
        }

        SpellBook() {
        }

        public boolean getForbidden() {
            return forbidden;
        }
    }

    @Entity(name = "BookJS")
    @Table(name = "BookJS")
    @Inheritance(strategy = InheritanceType.JOINED)
    public static class Book {

        @Id
        private Integer id;
        private String title;

        public Book() {
        }

        public Book(Integer id, String title) {
            this.id = id;
            this.title = title;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Book book = (Book) o;
            return Objects.equals(title, book.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title);
        }
    }
}
