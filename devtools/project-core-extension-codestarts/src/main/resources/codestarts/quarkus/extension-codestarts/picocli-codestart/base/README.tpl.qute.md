{#include readme-header /}

Also for picocli applications the dev mode is supported. When running dev mode, the picocli application is executed and on press of the Enter key, is restarted.

As picocli applications will often require arguments to be passed on the commandline, this is also possible in dev mode via:
```shell script
{buildtool.cli} {buildtool.cmd.dev} {#if input.base-codestart.buildtool == 'gradle' || input.base-codestart.buildtool == 'gradle-kotlin-dsl'}--quarkus-args{#else}-Dquarkus.args{/if}='Quarky'
```
