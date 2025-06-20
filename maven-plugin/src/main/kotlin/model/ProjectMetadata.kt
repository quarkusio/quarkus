package model

class ProjectMetadata(
    var groupId: String? = null,
    var artifactId: String? = null,
    var version: String? = null,
    var packaging: String? = null
) {
    var targetGroups: MutableMap<String, List<String>> = mutableMapOf()
    
    fun addTargetGroup(groupName: String, targets: List<String>) {
        targetGroups[groupName] = targets
    }
}