package model

/**
 * Represents the result of CreateNodesV2 function
 */
data class CreateNodesResult(
    var projects: MutableMap<String, ProjectConfiguration> = linkedMapOf(),
    var externalNodes: MutableMap<String, Any> = linkedMapOf()
) {
    fun addProject(name: String, project: ProjectConfiguration) {
        projects[name] = project
    }
}