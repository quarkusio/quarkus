package io.quarkus.test.junit.launcher;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigUtilTest {

    @Test
    void splitArgs() {
        assertEquals(
                List.of("-Xms2G", "-Dprop=value", "-DdoubleQuoted=\"value1,    value2\"",
                        "-DsingleQuoted='   value3,\t\nvalue4'"),
                ConfigUtil.splitArgs(
                        "-Xms2G -Dprop=value -DdoubleQuoted=\"value1,    value2\" -DsingleQuoted='   value3,\t\nvalue4'"));
    }
}
