package io.quarkus.it.spring.data.jpa;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
public class Person {

    private static final Random RANDOM = new Random();

    @Id
    @SequenceGenerator(name = "personSeqGen", sequenceName = "personSeq", initialValue = 100, allocationSize = 1)
    @GeneratedValue(generator = "personSeqGen")
    private Long id;

    @JsonbProperty(nillable = true)
    private String name;

    private Integer age;

    @JsonbDateFormat("yyyy-MM-dd")
    private Date joined;

    private boolean active;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    public Person(String name) {
        this.name = name;
        this.age = RANDOM.nextInt(100);
        this.joined = Date.from(LocalDate.now().minusDays(RANDOM.nextInt(3000)).atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        this.active = RANDOM.nextBoolean();
    }

    public Person() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Date getJoined() {
        return joined;
    }

    public void setJoined(Date joined) {
        this.joined = joined;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Entity
    @Table(name = "address")
    public static class Address {
        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "street_name")
        private String streetName;

        @Column(name = "street_number")
        private String streetNumber;

        @Column(name = "zip_code")
        private String zipCode;

        @JsonbTransient
        @OneToMany(mappedBy = "address")
        private List<Person> people;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStreetName() {
            return streetName;
        }

        public void setStreetName(String streetName) {
            this.streetName = streetName;
        }

        public String getStreetNumber() {
            return streetNumber;
        }

        public void setStreetNumber(String streetNumber) {
            this.streetNumber = streetNumber;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public List<Person> getPeople() {
            return people;
        }

        public void setPeople(List<Person> people) {
            this.people = people;
        }
    }
}
