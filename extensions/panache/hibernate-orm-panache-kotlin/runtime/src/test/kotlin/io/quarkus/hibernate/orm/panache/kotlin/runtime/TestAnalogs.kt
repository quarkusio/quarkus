package io.quarkus.hibernate.orm.panache.kotlin.runtime


import io.quarkus.gizmo.Gizmo
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.panache.common.deployment.ByteCodeType
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

val OBJECT = ByteCodeType(Object::class.java)

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
        val companionMethods = map(PanacheCompanionBase::class,
            ByteCodeType(PanacheEntityBase::class.java)).methods
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
            it.name.endsWith("Optional") || it in implemented
        }

//        methods("javaMethods", javaMethods)
//        methods("kotlinMethods", kotlinMethods)
//        methods("companionMethods", companionMethods)

        assertTrue(javaMethods.isEmpty(), "New methods not implemented: \n${javaMethods.byLine()}")
        assertTrue(kotlinMethods.isEmpty(), "Old methods not removed: \n${kotlinMethods.byLine()}")
        assertTrue(companionMethods.isEmpty(), "Old methods not removed: \n${companionMethods.byLine()}")
    }

    private fun map(type: KClass<*>, erasedType: ByteCodeType? = null): AnalogVisitor {
        return AnalogVisitor(erasedType).also { node ->
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
            it.name.endsWith("Optional") ||
                it.name in whiteList ||
                it in implemented
        }

//        methods("javaMethods", javaMethods)
//        methods("kotlinMethods", kotlinMethods)

        assertTrue(javaMethods.isEmpty(), "New methods not implemented: \n${javaMethods.byLine()}")
        assertTrue(kotlinMethods.isEmpty(), "Old methods not removed: ${kotlinMethods.byLine()}")
    }

    @Suppress("unused")
    private fun methods(label: String, methods: List<Method>) {
        println("$label: ")
        methods.toSortedSet(compareBy { it.toString() })
                .forEach {
                    println(it)
                }
    }
}

private fun <E> List<E>.byLine(): String {
    val map = map { it.toString() }
    return map
        .joinToString("\n" )
}

class AnalogVisitor(val erasedType: ByteCodeType? = null) : ClassVisitor(Gizmo.ASM_API_VERSION) {
    val methods = mutableListOf<Method>()
    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?,
                             exceptions: Array<out String>?): MethodVisitor? {
        if (name != "<init>") {
            val type = descriptor.substringAfterLast(")").trim()
            var parameters = descriptor.substring(
                descriptor.indexOf("("),
                descriptor.lastIndexOf(")") + 1
            )
            erasedType?.let { type->
                parameters = parameters.replace(type.descriptor(), OBJECT.descriptor())
            }

            methods += Method(access, name, type, parameters)
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
