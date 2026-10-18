package io.quarkus.it.validator;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/validator")
public class TestValidatorEndpoint {

    @Inject
    Validator validator;

    @POST
    @Path("/manual")
    @Consumes("application/json")
    public String manualValidation(MyData data) {
        Set<ConstraintViolation<MyData>> result = validator.validate(data);
        if (result.isEmpty()) {
            return "passed";
        }
        return "failed:" + result.iterator().next().getPropertyPath().toString();
    }

    public static class MyData {
        private String name;
        @Size(min = 3)
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

}
