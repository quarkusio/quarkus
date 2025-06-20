package model

class TargetMetadata(
    var type: String? = null,
    var description: String? = null
) {
    var phase: String? = null
    var plugin: String? = null
    var goal: String? = null
    var executionId: String? = null
    var technologies: MutableList<String> = mutableListOf()
}