package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByBelongsToTeamOrganizationalUnitName(String orgUnitName);
}
