package io.quarkus.redis.datasource.search;

import io.quarkus.redis.datasource.TransactionalRedisCommands;
import io.smallrye.common.annotation.Experimental;

/**
 * Allows executing commands from the {@code search} group (requires the Redis Search module from Redis stack). See
 * <a href="https://redis.io/commands/?group=search">the search command list</a> for further information about these
 * commands.
 * <p>
 * <a href="https://redis.io/docs/stack/search/"Redis Search</a> allows querying, secondary indexing, and full-text
 * search for Redis.
 * <p>
 * This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return
 * {@code void}.
 */
@Experimental("The commands from the search group are experimental")
public interface TransactionalSearchCommands extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/ft._list/">FT._LIST</a>. Summary: Returns a list of all
     * existing indexes. Group: search
     * <p>
     * <strong>This is a temporary command.</strong>
     */
    void ft_list();

    /**
     * Execute the command <a href="https://redis.io/commands/ft.aggregate/">FT.AGGREGATE</a>. Summary: Run a search
     * query on an index, and perform aggregate transformations on the results, extracting statistics from them Group:
     * search
     *
     * @param indexName
     *        the index against which the query is executed.
     * @param query
     *        the filtering query that retrieves the documents. It follows the exact same syntax as the search
     *        query, including filters, unions, not, optional, and so on.
     * @param args
     *        the extra parameters
     */
    void ftAggregate(String indexName, String query, AggregateArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.aggregate/">FT.AGGREGATE</a>. Summary: Run a search
     * query on an index, and perform aggregate transformations on the results, extracting statistics from them Group:
     * search
     *
     * @param indexName
     *        the index against which the query is executed.
     * @param query
     *        the filtering query that retrieves the documents. It follows the exact same syntax as the search
     *        query, including filters, unions, not, optional, and so on.
     */
    void ftAggregate(String indexName, String query);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.aliasadd/">FT.ALIASADD</a>. Summary: Add an alias to an
     * index Group: search
     *
     * @param alias
     *        the alias to be added to an index.
     * @param index
     *        the index
     */
    void ftAliasAdd(String alias, String index);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.aliasdel/">FT.ALIASDEL</a>. Summary: Remove an alias
     * from an index Group: search
     *
     * @param alias
     *        the alias to be removed
     */
    void ftAliasDel(String alias);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.aliasupdate/">FT.ALIASUPDATE</a>. Summary: Add an alias
     * to an index. If the alias is already associated with another index, FT.ALIASUPDATE removes the alias association
     * with the previous index. Group: search
     *
     * @param alias
     *        the alias to be added to an index.
     * @param index
     *        the index
     */
    void ftAliasUpdate(String alias, String index);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.alter/">FT.ALTER</a>. Summary: Add a new attribute to
     * the index. Adding an attribute to the index causes any future document updates to use the new attribute when
     * indexing and reindexing existing documents. Group: search
     *
     * @param index
     *        the index
     * @param field
     *        the indexed field to add
     * @param skipInitialScan
     *        whether to skip the initial scan, if set to {@code true}, does not scan and index.
     */
    void ftAlter(String index, IndexedField field, boolean skipInitialScan);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.alter/">FT.ALTER</a>. Summary: Add a new attribute to
     * the index. Adding an attribute to the index causes any future document updates to use the new attribute when
     * indexing and reindexing existing documents. Group: search
     *
     * @param index
     *        the index
     * @param field
     *        the indexed field to add
     */
    default void ftAlter(String index, IndexedField field) {
        ftAlter(index, field, false);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/ft.create/">FT.CREATE</a>. Summary: Create an index with
     * the given specification. Group: search
     *
     * @param index
     *        the index
     * @param args
     *        the creation arguments.
     */
    void ftCreate(String index, CreateArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.cursor-del/">FT.CURSOR DEL</a>. Summary: Delete a
     * cursor Group: search
     *
     * @param index
     *        the index
     * @param cursor
     *        the cursor id
     */
    void ftCursorDel(String index, long cursor);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.cursor-read/">FT.CURSOR READ</a>. Summary: Read next
     * results from an existing cursor Group: search
     *
     * @param index
     *        the index
     * @param cursor
     *        the cursor id
     */
    void ftCursorRead(String index, long cursor);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.cursor-read/">FT.CURSOR READ</a>. Summary: Read next
     * results from an existing cursor Group: search
     *
     * @param index
     *        the index
     * @param cursor
     *        the cursor id
     * @param count
     *        the number of results to read
     */
    void ftCursorRead(String index, long cursor, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.dropindex/">FT.DROPINDEX</a>. Summary: Delete an index
     * Group: search
     *
     * @param index
     *        the index
     */
    void ftDropIndex(String index);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.dropindex/">FT.DROPINDEX</a>. Summary: Delete an index
     * Group: search
     *
     * @param index
     *        the index
     * @param dd
     *        drop operation that, if set, deletes the actual document hashes.
     */
    void ftDropIndex(String index, boolean dd);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.dictadd/">FT.DICTADD</a>. Summary: Add terms to a
     * dictionary Group: search
     *
     * @param dict
     *        the dictionary name
     * @param words
     *        the terms to add to the dictionary
     */
    void ftDictAdd(String dict, String... words);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.dictdel/">FT.DICTDEL</a>. Summary: Remove terms from a
     * dictionary Group: search
     *
     * @param dict
     *        the dictionary name
     * @param words
     *        the terms to remove to the dictionary
     */
    void ftDictDel(String dict, String... words);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.dictdump/">FT.DICTDUMP</a>. Summary: Dump all terms in
     * the given dictionary Group: search
     *
     * @param dict
     *        the dictionary name
     */
    void ftDictDump(String dict);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.search/">FT.SEARCH</a>. Summary: Search the index with
     * a textual query, returning either documents or just ids Group: search
     *
     * @param index
     *        the index name. You must first create the index using {@code FT.CREATE}.
     * @param query
     *        the text query to search.
     * @param args
     *        the extra parameters
     */
    void ftSearch(String index, String query, QueryArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.search/">FT.SEARCH</a>. Summary: Search the index with
     * a textual query, returning either documents or just ids Group: search
     *
     * @param index
     *        the index name. You must first create the index using {@code FT.CREATE}.
     * @param query
     *        the text query to search.
     */
    void ftSearch(String index, String query);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.spellcheck/">FT.SPELLCHECK</a>. Summary: Perform
     * spelling correction on a query, returning suggestions for misspelled terms Group: search
     *
     * @param index
     *        the index name. You must first create the index using {@code FT.CREATE}.
     * @param query
     *        the text query to search.
     */
    void ftSpellCheck(String index, String query);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.spellcheck/">FT.SPELLCHECK</a>. Summary: Perform
     * spelling correction on a query, returning suggestions for misspelled terms Group: search
     *
     * @param index
     *        the index name. You must first create the index using {@code FT.CREATE}.
     * @param query
     *        the text query to search.
     * @param args
     *        the extra parameters
     */
    void ftSpellCheck(String index, String query, SpellCheckArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.syndump/">FT.SYNDUMP</a>. Summary: Dump the contents of
     * a synonym group Group: search
     *
     * @param index
     *        the index name.
     */
    void ftSynDump(String index);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.synupdate/">FT.SYNUPDATE</a>. Summary: Update a synonym
     * group Group: search
     *
     * @param index
     *        the index name.
     * @param groupId
     *        the synonym group
     * @param words
     *        the synonyms
     */
    void ftSynUpdate(String index, String groupId, String... words);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.synupdate/">FT.SYNUPDATE</a>. Summary: Update a synonym
     * group Group: search
     *
     * @param index
     *        the index name.
     * @param groupId
     *        the synonym group
     * @param skipInitialScan
     *        if set to {@code true}, does not scan and index, and only documents that are indexed after the update
     *        are affected.
     * @param words
     *        the synonyms
     */
    void ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.tagvals/">FT.TAGVALS</a>. Summary: return a distinct
     * set of values indexed in a Tag field Group: search
     *
     * @param index
     *        the index name.
     * @param field
     *        the name of a tag file defined in the schema.
     */
    void ftTagVals(String index, String field);
}
