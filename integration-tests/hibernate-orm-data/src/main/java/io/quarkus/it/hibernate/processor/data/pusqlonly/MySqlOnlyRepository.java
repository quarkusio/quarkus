package io.quarkus.it.hibernate.processor.data.pusqlonly;

import java.util.Optional;

import jakarta.data.repository.Repository;

import org.hibernate.annotations.processing.SQL;

@Repository(dataStore = "sqlonly")
public interface MySqlOnlyRepository {

    @SQL("insert into myuser (id, username, role) VALUES (:id, :username, :role)")
    void insert(int id, String username, String role);

    @SQL("select id, username, role from myuser where username = :username")
    Optional<MyUserDto> findByUsername(String username);

    record MyUserDto(Integer id, String username, String role) {
    }
}
