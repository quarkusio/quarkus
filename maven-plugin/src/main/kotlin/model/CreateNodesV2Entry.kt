package model

/**
 * Represents a single entry in CreateNodesV2 results array
 * [pomFilePath, CreateNodesResult]
 */
data class CreateNodesV2Entry(
    var pomFilePath: String? = null,
    var result: CreateNodesResult? = null
) {
    /**
     * Convert to Object array format expected by Gson
     */
    fun toArray(): Array<Any?> {
        return arrayOf(pomFilePath, result)
    }
}