package io.quarkus.it.rest.data.hibernate;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

import io.quarkus.hibernate.panache.PanacheRepository;

@Repository
public interface FruitRepository extends PanacheRepository.Stateless<Fruit, Long> {

    @Find
    Page<Fruit> findAll(PageRequest pageRequest, Order<Fruit> order);
}
