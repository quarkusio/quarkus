package io.quarkus.it.spring.data.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "employee")
public class Employee extends AbstractEntity {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team belongsToTeam;

    public String getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Team getBelongsToTeam() {
        return belongsToTeam;
    }

    @Entity
    @Table(name = "team")
    public static class Team extends AbstractEntity {

        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "unit_id", nullable = false)
        private OrgUnit organizationalUnit;

        public String getName() {
            return name;
        }

        public OrgUnit getOrganizationalUnit() {
            return organizationalUnit;
        }
    }

    @Entity
    @Table(name = "unit")
    public static class OrgUnit extends AbstractEntity {

        private String name;
    }
}
