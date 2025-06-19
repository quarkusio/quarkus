package model

data class CreateNodesResult(
    var projects: MutableMap<String, ProjectConfiguration> = mutableMapOf(),
    var externalNodes: MutableMap<String, Any> = mutableMapOf()
) {
    fun addProject(name: String, project: ProjectConfiguration) {
        projects[name] = project
    }
}