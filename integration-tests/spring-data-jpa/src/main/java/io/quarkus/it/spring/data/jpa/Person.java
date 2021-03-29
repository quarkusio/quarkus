package io.quarkus.it.spring.data.jpa;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
public class Person {

    @Id
    @SequenceGenerator(name = "personSeqGen", sequenceName = "personSeq", initialValue = 100, allocationSize = 1)
    @GeneratedValue(generator = "personSeqGen")
    private long id;

    @JsonbProperty(nillable = true)
    private String name;

    private Integer age;

    @JsonbDateFormat("yyyy-MM-dd")
    private Date joined;

    private boolean active;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address someAddress;

    @ManyToMany
    @JoinTable(name = "liked_songs", joinColumns = @JoinColumn(name = "person_id"), inverseJoinColumns = @JoinColumn(name = "song_id"))
    private Set<Song> likedSongs = new HashSet<>();

    public Person(String name) {
        this.name = name;
        this.age = DataGenerator.randomAge();
        this.joined = DataGenerator.randomDate();
        this.active = DataGenerator.randomBoolean();
    }

    public Person() {
    }

    public long getId() {
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

    public Address getSomeAddress() {
        return someAddress;
    }

    public void setSomeAddress(Address address) {
        this.someAddress = address;
    }

    public Set<Song> getLikedSongs() {
        return likedSongs;
    }

    public void setLikedSongs(Set<Song> likedSongs) {
        this.likedSongs = likedSongs;
    }

    @MappedSuperclass
    public static class StreetEntity {

        @Column(name = "street_name")
        private String streetName;

        @Column(name = "street_number")
        private String streetNumber;

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
    }

    @Entity
    @Table(name = "address")
    public static class Address extends StreetEntity {
        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "zip_code")
        private String zipCode;

        @JsonbTransient
        @OneToMany(mappedBy = "someAddress")
        private List<Person> people;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
