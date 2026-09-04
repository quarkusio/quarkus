package io.quarkus.spring.data.deployment;

import java.time.MonthDay;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Forecast {

    @Id
    @GeneratedValue
    private Integer id;

    /**
     * A JDK type Hibernate ORM does not handle natively, so it goes through an
     * AttributeConverter. Being a JDK class it is on the classpath but never part of the
     * application's Jandex index, which is what reproduces #51750.
     */
    @Convert(converter = MonthDayConverter.class)
    private MonthDay renewal;

    private Integer amount;

    public Forecast() {
    }

    public Forecast(MonthDay renewal, Integer amount) {
        this.renewal = renewal;
        this.amount = amount;
    }

    public Integer getId() {
        return id;
    }

    public MonthDay getRenewal() {
        return renewal;
    }

    public void setRenewal(MonthDay renewal) {
        this.renewal = renewal;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
