package io.quarkus.cli;

import org.aesh.AeshRuntimeRunner;

import io.quarkus.cli.commands.QuarkusBaseCommand;

public class QuarkusCli {

    public static void main(String[] args) {
        AeshRuntimeRunner.builder()
                .command(QuarkusBaseCommand.class)
                .args(args)
                .interactive(true)
                .execute();
    }

}
