package io.quarkus.it.spring.data.jpa;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

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
}
