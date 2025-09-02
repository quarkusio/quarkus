package io.quarkus.hibernate.reactive.panache.kotlin

import io.quarkus.gizmo.Gizmo
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase as JavaPanacheEntityBase
import io.quarkus.hibernate.reactive.panache.PanacheQuery as JavaPanacheQuery
import io.quarkus.hibernate.reactive.panache.PanacheRepository as JavaPanacheRepository
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase as JavaPanacheRepositoryBase
import io.quarkus.panache.common.deployment.ByteCodeType
import java.util.Optional
import java.util.function.Consumer
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

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
        compare(map(JavaPanacheRepositoryBase::class), map(PanacheRepositoryBase::class))
    }

    @Test
    fun testPanacheEntityBase() {
        compare(JavaPanacheEntityBase::class, PanacheEntityBase::class, PanacheCompanionBase::class)
    }

    private fun compare(javaEntity: KClass<*>, kotlinEntity: KClass<*>, companion: KClass<*>) {
        val javaMethods = map(javaEntity).methods
        val kotlinMethods =
            map(kotlinEntity)
                .methods
                .filterNot {
                    it.name.contains("getId") ||
                        it.name.contains("setId") ||
                        it.name.contains("getOperations")
                }
                .toMutableList()
        val companionMethods = map(companion).methods
        val implemented = mutableListOf<Method>()

        javaMethods.forEach {
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
        javaMethods.removeIf { it.name == "findByIdOptional" || it in implemented }

        methods("javaMethods", javaMethods)
        methods("kotlinMethods", kotlinMethods)
        methods("companionMethods", companionMethods)

        assertTrue(javaMethods.isEmpty(), "New methods not implemented: $javaMethods")
        assertTrue(kotlinMethods.isEmpty(), "Old methods not removed: $kotlinMethods")
        assertTrue(companionMethods.isEmpty(), "Old methods not removed: $companionMethods")
    }

    private fun map(type: KClass<*>): AnalogVisitor {
        return AnalogVisitor().also { node -> ClassReader(type.bytes()).accept(node, SKIP_CODE) }
    }

    private fun KClass<*>.bytes() =
        java.classLoader.getResourceAsStream(qualifiedName.toString().replace(".", "/") + ".class")

    private fun compare(
        javaClass: AnalogVisitor,
        kotlinClass: AnalogVisitor,
        allowList: List<String> = listOf(),
    ) {
        val javaMethods = javaClass.methods
        val kotlinMethods = kotlinClass.methods
        val implemented = mutableListOf<Method>()

        javaMethods.forEach {
            if (it in kotlinMethods) {
                kotlinMethods -= it
                implemented += it
            }
        }

        javaMethods.removeIf { it.name in allowList || it in implemented }

        methods("javaMethods", javaMethods)
        methods("kotlinMethods", kotlinMethods)

        assertTrue(javaMethods.isEmpty(), "New methods not implemented: $javaMethods")
        assertTrue(kotlinMethods.isEmpty(), "Old methods not removed: $kotlinMethods")
    }

    @Suppress("unused")
    private fun methods(label: String, methods: List<Method>) {
        if (methods.isNotEmpty()) {
            println("$label: ")
            methods.forEach { println(it) }
            println()
        }
    }
}

class AnalogVisitor : ClassVisitor(Gizmo.ASM_API_VERSION) {
    val erasures =
        mapOf(
            Type.getType(PanacheEntityBase::class.java).descriptor to
                Type.getType(Object::class.java).descriptor
        )

    val methods = mutableListOf<Method>()

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {
        if (
            name != "<clinit>" &&
                name != "<init>" &&
                name != "injectionMissing" &&
                !name.contains("lambda") &&
                !name.endsWith("\$jd") && // JVM defaults synthetic methods generated by Kotlin
                !descriptor.endsWith(ByteCodeType(Optional::class.java).descriptor())
        ) {
            val method =
                Method(
                    access,
                    name,
                    erase(Type.getReturnType(descriptor)),
                    erase(Type.getArgumentTypes(descriptor)),
                )
            methods += method
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    private fun erase(type: Type): String {
        var value = type.descriptor
        erasures.entries.forEach(Consumer { value = value.replace(it.key, it.value) })
        return value
    }

    private fun erase(types: Array<Type>): List<String> = types.map { erase(it) }
}

class Method(val access: Int, val name: String, val type: String, val parameters: List<String>) {
    fun isStatic() = access.matches(Opcodes.ACC_STATIC)

    override fun toString(): String {
        return (if (isStatic()) "static " else "") +
            "fun $name(${parameters.joinToString(", ")})" +
            (if (type != Unit::class.qualifiedName) ": $type" else "")
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
