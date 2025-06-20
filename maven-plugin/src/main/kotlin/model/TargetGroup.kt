package model

class TargetGroup(
    var phase: String? = null,
    var description: String? = null,
    var order: Int = 0
) {
    var targets: MutableList<String> = mutableListOf()
    
    fun addTarget(targetName: String) {
        if (targetName !in targets) {
            targets.add(targetName)
        }
    }
}