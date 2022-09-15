package io.quarkus.it.jpa.elementcollection;

import static org.hibernate.annotations.CascadeType.ALL;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;

@Entity
public class OpeningTimes {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @NotNull
    private String name;
    private String description;
    private LocalTime timeFrom;
    private LocalTime timeTo;

    @ElementCollection
    @Cascade(value = { ALL })
    private Collection<DayOfWeek> daysOfWeek;

    OpeningTimes() {
        name = "";
    }

    public OpeningTimes(
            String name,
            LocalTime timeFrom,
            LocalTime timeTo,
            Collection<DayOfWeek> daysOfWeek) {
        this.name = name;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.daysOfWeek = new HashSet<>(daysOfWeek);
    }

    public OpeningTimes(String name, LocalTime from, LocalTime to, DayOfWeek... daysOfWeek) {
        this(name, from, to, new ArrayList<>(Arrays.asList(daysOfWeek)));
    }

    public LocalTime getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(LocalTime timeFrom) {
        this.timeFrom = timeFrom;
    }

    public LocalTime getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(LocalTime timeTo) {
        this.timeTo = timeTo;
    }

    public Collection<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OpeningTimes))
            return false;
        OpeningTimes that = (OpeningTimes) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getTimeFrom(), that.getTimeFrom())
                && Objects.equals(getTimeTo(), that.getTimeTo())
                && Objects.equals(getDaysOfWeek(), that.getDaysOfWeek());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getId(), getName(), getDescription(), getTimeFrom(), getTimeTo(), getDaysOfWeek());
    }
}
