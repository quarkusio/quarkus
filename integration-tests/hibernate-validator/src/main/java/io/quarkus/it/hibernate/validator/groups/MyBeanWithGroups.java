package io.quarkus.it.hibernate.validator.groups;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

public class MyBeanWithGroups {

    @Null(groups = ValidationGroups.Post.class)
    @NotNull(groups = { ValidationGroups.Put.class, ValidationGroups.Get.class, ValidationGroups.Delete.class })
    private Long id;

    @NotNull
    private String name;

    @AssertFalse(groups = { ValidationGroups.Post.class, ValidationGroups.Put.class, ValidationGroups.Get.class })
    @AssertTrue(groups = ValidationGroups.Delete.class)
    private boolean deleted;

    public MyBeanWithGroups() {
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
