package io.quarkus.funqy.test;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(UseSyncGreetExtension.class)
public class GreetSyncTest extends GreetTestBase {
}
