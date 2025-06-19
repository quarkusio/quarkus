package model

data class CreateNodesV2Entry(
    var pomFilePath: String? = null,
    var result: CreateNodesResult? = null
) {
    fun toArray(): Array<Any?> {
        return arrayOf(pomFilePath, result)
    }
}