
### Repository Interface

Define repository interfaces — no implementation needed. Supported base types: `CrudRepository`, `ListCrudRepository`, `PagingAndSortingRepository`, `ListPagingAndSortingRepository`, `JpaRepository`.

```java
public interface ProductRepository extends CrudRepository<Product, Long> {

    List<Product> findByCategory(String category);

    List<Product> findByPriceLessThan(double price);

    List<Product> findByNameContainingIgnoreCase(String name);

    long countByCategory(String category);

    boolean existsByName(String name);

    @Query("SELECT p FROM Product p WHERE p.name LIKE :name")
    List<Product> searchByName(@Param("name") String name);
}
```

### Injecting Repositories

Inject directly — no `@Repository` annotation needed:

```java
@Inject ProductRepository productRepository;
```

### Derived Query Methods

Method names are parsed into queries automatically. Supported keywords:
`findBy`, `countBy`, `existsBy`, `deleteBy` + property name + operators like `LessThan`, `GreaterThan`, `Between`, `Like`, `Containing`, `IgnoreCase`, `In`, `OrderBy`, `Not`, `IsNull`, `IsNotNull`.

### Custom Queries

```java
@Query("SELECT p FROM Product p WHERE p.category = ?1 AND p.price < ?2")
List<Product> findByCategoryAndMaxPrice(String category, double maxPrice);

@Modifying
@Query("UPDATE Product p SET p.price = p.price * :factor WHERE p.category = :category")
int updatePriceByCategory(@Param("category") String category, @Param("factor") double factor);
```

`@Modifying` is required for UPDATE/DELETE queries. The method must be called within a `@Transactional` context.

### Pagination

```java
public interface ProductRepository extends PagingAndSortingRepository<Product, Long> {
    Page<Product> findByCategory(String category, Pageable pageable);
}

// Usage
Page<Product> page = repo.findByCategory("electronics", PageRequest.of(0, 10, Sort.by("price")));
```

### Configuration

For dev/test, let Dev Services manage the datasource — no configuration needed. For explicit H2:

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=sa
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

### Testing

```java
@QuarkusTest
class ProductRepositoryTest {

    @Inject ProductRepository repo;

    @Test
    @Transactional
    void testDerivedQuery() {
        Product p = new Product();
        p.setName("Laptop");
        p.setCategory("electronics");
        p.setPrice(999.99);
        repo.save(p);

        List<Product> results = repo.findByCategory("electronics");
        assertEquals(1, results.size());
    }
}
```

Repository methods that modify data (save, delete) need `@Transactional` on the test method.

### Common Pitfalls

- **No `@Repository` annotation needed** — unlike Spring Boot, Quarkus discovers repository interfaces automatically at build time.
- **H2 needs `username=sa`** — even with an empty password, H2 requires an explicit username. Omitting it causes authentication errors.
- **`database.generation` is deprecated** — use `quarkus.hibernate-orm.schema-management.strategy` instead.
- **Dev Services vs explicit config**: If you set `quarkus.datasource.jdbc.url` explicitly, you own the full config (including credentials). Omitting it lets Dev Services handle everything automatically.
- **`@Transactional` in tests**: Save/delete operations in `@QuarkusTest` methods require `@Transactional`, otherwise changes aren't committed before queries.
- **Not all Spring Data features are supported** — `Specification`, `QueryByExample`, and custom repository base classes have limited support. Check the Quarkus Spring Data JPA guide for the full compatibility matrix.
