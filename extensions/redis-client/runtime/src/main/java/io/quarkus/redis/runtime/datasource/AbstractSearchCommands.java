package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.lang.reflect.Type;

import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractSearchCommands<K> extends AbstractRedisCommands {

    AbstractSearchCommands(RedisCommandExecutor redis, Type k) {
        super(redis, new Marshaller(k));
    }

    Uni<Response> _ft_list() {
        RedisCommand cmd = RedisCommand.of(Command.FT__LIST);
        return execute(cmd);
    }

    Uni<Response> _ftAggregate(String indexName, String query, AggregateArgs args) {
        notNullOrBlank(indexName, "indexName");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.FT_AGGREGATE).put(indexName).put(query).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _ftAggregate(String indexName, String query) {
        notNullOrBlank(indexName, "indexName");
        notNullOrBlank(query, "query");
        RedisCommand cmd = RedisCommand.of(Command.FT_AGGREGATE).put(indexName).put(query);
        return execute(cmd);
    }

    Uni<Response> _ftAliasAdd(String alias, String index) {
        notNullOrBlank(alias, "alias");
        notNullOrBlank(index, "index");
        RedisCommand cmd = RedisCommand.of(Command.FT_ALIASADD).put(alias).put(index);
        return execute(cmd);
    }

    Uni<Response> _ftAliasDel(String alias) {
        notNullOrBlank(alias, "alias");
        RedisCommand cmd = RedisCommand.of(Command.FT_ALIASDEL).put(alias);
        return execute(cmd);
    }

    Uni<Response> _ftAliasUpdate(String alias, String index) {
        notNullOrBlank(alias, "alias");
        notNullOrBlank(index, "index");
        RedisCommand cmd = RedisCommand.of(Command.FT_ALIASUPDATE).put(alias).put(index);
        return execute(cmd);
    }

    Uni<Response> _ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        notNullOrBlank(index, "index");
        nonNull(field, "field");
        RedisCommand cmd = RedisCommand.of(Command.FT_ALTER).put(index).putFlag(skipInitialScan, "SKIPINITIALSCAN")
                .put("SCHEMA").put("ADD").putArgs(field);
        return execute(cmd);
    }

    Uni<Response> _ftCreate(String index, CreateArgs args) {
        notNullOrBlank(index, "index");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.FT_CREATE).put(index).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _ftCursorDel(String index, long cursor) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        RedisCommand cmd = RedisCommand.of(Command.FT_CURSOR).put("DEL").put(index).put(cursor);
        return execute(cmd);
    }

    Uni<Response> _ftCursorRead(String index, long cursor) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        RedisCommand cmd = RedisCommand.of(Command.FT_CURSOR).put("READ").put(index).put(cursor);
        return execute(cmd);
    }

    Uni<Response> _ftCursorRead(String index, long cursor, int count) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.FT_CURSOR).put("READ").put(index).put(cursor).put("COUNT")
                .put(count);
        return execute(cmd);
    }

    Uni<Response> _ftDropIndex(String index) {
        notNullOrBlank(index, "index");
        RedisCommand cmd = RedisCommand.of(Command.FT_DROPINDEX).put(index);
        return execute(cmd);
    }

    Uni<Response> _ftDropIndex(String index, boolean dd) {
        notNullOrBlank(index, "index");
        RedisCommand cmd = RedisCommand.of(Command.FT_DROPINDEX).put(index).putFlag(dd, "DD");
        return execute(cmd);
    }

    Uni<Response> _ftDictAdd(String dict, String... words) {
        notNullOrBlank(dict, "dict");
        notNullOrEmpty(words, "words");
        doesNotContainNull(words, "words");
        RedisCommand cmd = RedisCommand.of(Command.FT_DICTADD).put(dict).putAll(words);
        return execute(cmd);
    }

    Uni<Response> _ftDictDel(String dict, String... words) {
        notNullOrBlank(dict, "dict");
        notNullOrEmpty(words, "words");
        doesNotContainNull(words, "words");
        RedisCommand cmd = RedisCommand.of(Command.FT_DICTDEL).put(dict).putAll(words);
        return execute(cmd);
    }

    Uni<Response> _ftDictDump(String dict) {
        notNullOrBlank(dict, "dict");
        RedisCommand cmd = RedisCommand.of(Command.FT_DICTDUMP).put(dict);
        return execute(cmd);
    }

    Uni<Response> _ftSearch(String index, String query, QueryArgs args) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.FT_SEARCH).put(index).put(query).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _ftSearch(String index, String query) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        RedisCommand cmd = RedisCommand.of(Command.FT_SEARCH).put(index).put(query);
        return execute(cmd);
    }

    Uni<Response> _ftSpellCheck(String index, String query) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        RedisCommand cmd = RedisCommand.of(Command.FT_SPELLCHECK).put(index).put(query);
        return execute(cmd);
    }

    Uni<Response> _ftSpellCheck(String index, String query, SpellCheckArgs args) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.FT_SPELLCHECK).put(index).put(query).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _ftSynDump(String index) {
        notNullOrBlank(index, "index");
        RedisCommand cmd = RedisCommand.of(Command.FT_SYNDUMP).put(index);
        return execute(cmd);
    }

    Uni<Response> _ftSynUpdate(String index, String groupId, String... words) {
        notNullOrBlank(index, "index");
        notNullOrBlank(groupId, "groupId");
        doesNotContainNull(words, "words");
        notNullOrEmpty(words, "words");
        RedisCommand cmd = RedisCommand.of(Command.FT_SYNUPDATE).put(index).put(groupId).putAll(words);
        return execute(cmd);
    }

    Uni<Response> _ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        notNullOrBlank(index, "index");
        notNullOrBlank(groupId, "groupId");
        doesNotContainNull(words, "words");
        notNullOrEmpty(words, "words");
        RedisCommand cmd = RedisCommand.of(Command.FT_SYNUPDATE).put(index).put(groupId)
                .putFlag(skipInitialScan, "SKIPINITIALSCAN").putAll(words);
        return execute(cmd);
    }

    Uni<Response> _ftTagVals(String index, String field) {
        notNullOrBlank(index, "index");
        notNullOrBlank(field, "field");
        RedisCommand cmd = RedisCommand.of(Command.FT_TAGVALS).put(index).put(field);
        return execute(cmd);
    }
}
