package kni

import kni.jobject.*
import kotlinx.cinterop.*
import native.jni.JNI_FALSE
import native.jni.JNI_TRUE
import native.jni.jboolean
import native.jni.jvalue

//region jboolean
/**
 * Convert [jboolean] to [Boolean]
 *
 * @throws IllegalArgumentException when the value of [jboolean] is invalid
 */
fun jboolean.toBoolean(): Boolean {
    return when (this) {
        JNI_TRUE.toUByte() -> true
        JNI_FALSE.toUByte() -> false
        else -> throw IllegalArgumentException("Invalid jboolean:$this")
    }
}

/**
 * Convert [Boolean] to [jboolean]
 */
@Suppress("SpellCheckingInspection")
fun Boolean.tojboolean(): jboolean {
    return when (this) {
        true -> JNI_TRUE.toUByte()
        false -> JNI_FALSE.toUByte()
    }
}
//endregion

//region jvalue
/**
 * Convert va list to [jvalue] array
 */
fun Array<out Any?>.toJValues(scope: AutofreeScope): CPointer<jvalue> {
    val values = scope.allocArray<jvalue>(size)
    map { it?.asJValue() ?: zeroValue() }.onEachIndexed { i, it -> it.place(values[i].ptr) }
    return values
}

/**
 * Convert Kotlin types to [jvalue]
 */
inline fun <reified T> T.asJValue(): CValue<jvalue> {
    return cValue {
        when (this@asJValue) {
            is Boolean -> z = this@asJValue.tojboolean()
            is Byte -> b = this@asJValue
            is Char -> c = this@asJValue.code.toUShort()
            is UShort -> c = this@asJValue
            is Short -> s = this@asJValue
            is Int -> i = this@asJValue
            is Long -> j = this@asJValue
            is Float -> f = this@asJValue
            is Double -> d = this@asJValue
            is JRef -> l = this@asJValue.obj
            is JObject -> l = this@asJValue.ref.obj
            else -> throw IllegalArgumentException("Unsupported Convention:${this@asJValue!!::class.qualifiedName} to ${jvalue::class.qualifiedName}")
        }
    }
}

/**
 * Cast [jvalue] to [Boolean]
 */
fun jvalue.asBoolean(): Boolean {
    return this.z.toBoolean()
}

/**
 * Cast [jvalue] to [Byte]
 */
fun jvalue.asByte(): Byte {
    return this.b
}

/**
 * Cast [jvalue] tp [Char]
 */
fun jvalue.asChar(): Char {
    return this.c.toInt().toChar()
}

/**
 * Cast [jvalue] to [UShort]
 */
fun jvalue.asUShort(): UShort {
    return this.c
}

/**
 * Cast [jvalue] to [Short]
 */
fun jvalue.asShort(): Short {
    return this.s
}

/**
 * Cast [jvalue] to [Int]
 */
fun jvalue.asInt(): Int {
    return this.i
}

/**
 * Cast [jvalue] to [Long]
 */
fun jvalue.asLong(): Long {
    return this.j
}

/**
 * Cast [jvalue] to [Float]
 */
fun jvalue.asFloat(): Float {
    return this.f
}

/**
 * Cast [jvalue] to [Double]
 */
fun jvalue.asDouble(): Double {
    return this.d
}

/**
 * Cast [jvalue] to [JRefLocal]
 */
inline fun <reified T : JRef> jvalue.asJRef(): JRef? {
    return this.l?.let {
        when (T::class) {
            JRefLocal::class -> JRefLocal(it)
            JRefGlobal::class -> JRefGlobal(it)
            JRefWeak::class -> JRefWeak(it)
            else -> throw IllegalArgumentException("Unsupported JRef:${T::class.qualifiedName}")
        }
    }
}
//endregion