package io.quarkus.it.hibernate.validator.groups;

import javax.validation.groups.Default;

public interface ValidationGroups {
    interface Post extends Default {
    }

    interface Put extends Default {
    }

    interface Get extends Default {
    }

    interface Delete extends Default {
    }
}
