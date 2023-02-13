package io.quarkus.spring.data.deployment;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "User_")
@NamedQuery(name = "User.getUserByFullNameUsingNamedQuery", query = "select u from User u where u.fullName=:name")
@NamedQueries(@NamedQuery(name = "User.getUserByFullNameUsingNamedQueries", query = "select u from User u where u.fullName=:name"))
public class User {

    @Id
    private String userId;
    private String fullName;
    private boolean active;
    private int loginCounter;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LoginEvent> loginEvents = new ArrayList<>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getLoginCounter() {
        return loginCounter;
    }

    public void setLoginCounter(int loginCounter) {
        this.loginCounter = loginCounter;
    }

    public void addEvent(LoginEvent loginEvent) {
        this.loginEvents.add(loginEvent);
    }

    public List<LoginEvent> getLoginEvents() {
        return loginEvents;
    }

}
