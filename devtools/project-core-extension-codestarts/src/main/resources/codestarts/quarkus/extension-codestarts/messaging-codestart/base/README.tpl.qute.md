{#include readme-header /}

{#each input.selected-extensions-ga}
{#switch it}
{#case 'io.quarkus:quarkus-messaging-kafka'}
[Related Apache Kafka guide section...](https://quarkus.io/guides/kafka-reactive-getting-started)

{#case 'io.quarkus:quarkus-messaging-amqp'}
[Related Apache AMQP 1.0 guide section...](https://quarkus.io/guides/amqp)

{/switch}
{/each}
