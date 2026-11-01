# Quarkus RP OIDC Certification Instructions

## How to test
Go to `https://www.certification.openid.net`, login and create a new `Basic Profile, ResponseMode: Query` test profile, `request_type=plain_http_request, client_registration=static_client` variant.

Start a certification endpoint in the `certification-endpoint` folder with `mvn quarkus:dev`, set `quarkus.oidc.auth-server-url`, `quarkus.oidc.client-id` and `quarkus.oidc.credentials.secret` in `application.properties` to the values shown in the created test profile.

Run tests one by one as documented. Initially, each test transitions to a `Waiting` state. Now access `http://localhost:8080/oidc` from the browser to have the current running test transitioned to a `Finished` state. Repeat for every listed test. Clearing the browser cookie cache between tests is recommended.

