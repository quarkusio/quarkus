package io.quarkus.it.rest.data.hibernate;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

import io.quarkus.data.hibernate.RecordRepository;

@Repository
public interface FruitRepository extends RecordRepository<Fruit> {

    @Find
    Page<Fruit> findAll(PageRequest pageRequest, Order<Fruit> order);
}
