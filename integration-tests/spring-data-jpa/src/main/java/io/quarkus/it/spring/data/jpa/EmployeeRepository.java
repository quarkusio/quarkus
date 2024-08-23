package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByBelongsToTeamOrganizationalUnitName(String orgUnitName);

    List<Employee> findByLastNameContainingAndFirstNameContainingAndEmailContainingAllIgnoreCase(String lastName,
            String firstName, String emailPart);

    List<Employee> findFirst2ByFirstNameContainingIgnoreCaseOrderById(String firstName);
}
