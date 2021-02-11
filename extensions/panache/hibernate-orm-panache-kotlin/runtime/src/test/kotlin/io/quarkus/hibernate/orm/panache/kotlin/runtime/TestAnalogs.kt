package io.quarkus.hibernate.orm.panache.kotlin.runtime


import io.quarkus.gizmo.Gizmo
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass
import io.quarkus.hibernate.orm.panache.PanacheEntityBase as JavaPanacheEntityBase
import io.quarkus.hibernate.orm.panache.PanacheQuery as JavaPanacheQuery
import io.quarkus.hibernate.orm.panache.PanacheRepository as JavaPanacheRepository

class TestAnalogs {

    @Test
    fun testPanacheQuery() {
        compare(map(JavaPanacheQuery::class), map(PanacheQuery::class))
    }

    @Test
    fun testPanacheRepository() {
        compare(map(JavaPanacheRepository::class), map(PanacheRepository::class))
    }

    @Test
    fun testPanacheRepositoryBase() {
        compare(map(JavaPanacheRepository::class), map(PanacheRepository::class), listOf("findByIdOptional"))
    }

    @Test
    fun testPanacheEntityBase() {
        val javaMethods = map(JavaPanacheEntityBase::class).methods
        val kotlinMethods = map(PanacheEntityBase::class).methods
        val companionMethods = map(PanacheCompanion::class).methods
        val implemented = mutableListOf<Method>()

        javaMethods
                .forEach {
                    if (!it.isStatic()) {
                        if (it in kotlinMethods) {
                            kotlinMethods -= it
                            implemented += it
                        }
                    } else {
                        if (it in companionMethods) {
                            companionMethods -= it
                            implemented += it
                        }
                    }
                }
        javaMethods.removeIf {
            it.name == "findByIdOptional" ||
                    it in implemented
        }

//        methods("javaMethods", javaMethods)
//        methods("kotlinMethods", kotlinMethods)
//        methods("companionMethods", companionMethods)

        assertTrue(javaMethods.isEmpty(), "New methods not implemented: ${javaMethods}")
        assertTrue(kotlinMethods.isEmpty(), "Old methods not removed: ${kotlinMethods}")
        assertTrue(companionMethods.isEmpty(), "Old methods not removed: ${companionMethods}")
    }

    private fun map(type: KClass<*>): AnalogVisitor {
        return AnalogVisitor().also { node ->
            ClassReader(type.bytes()).accept(node, SKIP_CODE)
        }
    }


    private fun KClass<*>.bytes() =
            java.classLoader.getResourceAsStream(qualifiedName.toString().replace(".", "/") + ".class")

    private fun compare(javaClass: AnalogVisitor, kotlinClass: AnalogVisitor, whiteList: List<String> = listOf()) {
        val javaMethods = javaClass.methods
        val kotlinMethods = kotlinClass.methods
        val implemented = mutableListOf<Method>()

        javaMethods
                .forEach {
                    if (it in kotlinMethods) {
                        kotlinMethods -= it
                        implemented += it
                    }
                }

        javaMethods.removeIf {
            it.name in whiteList ||
                    it in implemented
        }

//        methods("javaMethods", javaMethods)
//        methods("kotlinMethods", kotlinMethods)

        assertTrue(javaMethods.isEmpty(), "New methods not implemented: ${javaMethods}")
        assertTrue(kotlinMethods.isEmpty(), "Old methods not removed: ${kotlinMethods}")
    }

    @Suppress("unused")
    private fun methods(label: String, methods: List<Method>) {
        println("$label: ")
        methods
                .forEach {
                    println(it)
                }
    }
}

class AnalogVisitor : ClassVisitor(Gizmo.ASM_API_VERSION) {
    val methods = mutableListOf<Method>()
    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?,
                             exceptions: Array<out String>?): MethodVisitor? {
        if (name != "<init>") {
            methods += Method(access, name,
                    descriptor.substringAfterLast(")").trim(), descriptor.substring(descriptor.indexOf("("),
                    descriptor.lastIndexOf(")") + 1)/*, signature*/)
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
}

class Method(val access: Int, val name: String, val type: String, val parameters: String) {
    fun isStatic() = access.matches(Opcodes.ACC_STATIC)

    override fun toString(): String {
        return (if (isStatic()) "static " else "") + "fun ${name}$parameters" +
                (if (type.isNotBlank()) ": $type" else "") //+
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Method) return false

        if (name != other.name) return false
        if (parameters != other.parameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

}

fun Int.matches(mask: Int) = (this and mask) == mask

fun Int.accDecode(): List<String> {
    val decode: MutableList<String> = ArrayList()
    val values: MutableMap<String, Int> = LinkedHashMap()
    try {
        for (f in Opcodes::class.java.declaredFields) {
            if (f.name.startsWith("ACC_")) {
                values[f.name] = f.getInt(Opcodes::class.java)
            }
        }
    } catch (e: IllegalAccessException) {
        throw RuntimeException(e.message, e)
    }
    for ((key, value) in values) {
        if (this.matches(value)) {
            decode.add(key)
        }
    }
    return decode
}