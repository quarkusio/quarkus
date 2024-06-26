package io.quarkus.it.mailer;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@WithTestResource(value = MailpitTestResource.class, restrictToAnnotatedClass = false)
public class MailerIT extends MailerTest {

}
