package io.quarkus.it.hibertnate.orm.graphql.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class BookRepository implements PanacheRepository<Book> {
}
