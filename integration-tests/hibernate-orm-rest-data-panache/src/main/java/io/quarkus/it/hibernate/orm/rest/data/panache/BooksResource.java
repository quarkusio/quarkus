package io.quarkus.it.hibernate.orm.rest.data.panache;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface BooksResource extends PanacheRepositoryResource<BookRepository, Book, Long> {
}
