package io.quarkus.it.panache.resources;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.it.panache.hibernate.HibernateBook;
import io.quarkus.it.panache.hibernate.HibernateBookRepository;
import io.quarkus.it.panache.mongodb.MongoBook;
import io.quarkus.it.panache.mongodb.MongoBookRepository;

@Path("/book")
public class BookResource {

    @Inject
    HibernateBookRepository hibernateBookRepository;
    @Inject
    MongoBookRepository mongoBookRepository;

    @Transactional
    @GET
    @Path("/hibernate/{name}/{author}")
    public List<HibernateBook> addAndListAllHibernate(@PathParam("name") String name, @PathParam("author") String author) {
        hibernateBookRepository.persist(new HibernateBook(name, author));
        return hibernateBookRepository.listAll();
    }

    @GET
    @Path("/mongo/{name}/{author}")
    public List<MongoBook> addAndListAllMongo(@PathParam("name") String name, @PathParam("author") String author) {
        mongoBookRepository.persist(new MongoBook(name, author));
        return mongoBookRepository.listAll();
    }
}
