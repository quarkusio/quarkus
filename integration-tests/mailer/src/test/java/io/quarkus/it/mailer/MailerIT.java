package io.quarkus.it.mailer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@QuarkusTestResource(FakeMailerTestResource.class)
public class MailerIT extends MailerTest {

}
