package kni.jobject.jclass

import kni.jobject.JArray
import kni.jobject.JObject
import kotlin.reflect.KClass

/**
 * A [JDescriptor] is a string representing the type of field or method
 */
typealias JDescriptor = String

val JDescriptor.type: KClass<*>
    get() {
        return when (this[0]) {
            'V' -> Unit::class
            'B' -> Byte::class
            'C' -> Char::class
            'D' -> Double::class
            'F' -> Float::class
            'I' -> Int::class
            'J' -> Long::class
            'S' -> Short::class
            'Z' -> Boolean::class
            'L' -> JObject::class
            '[' -> JArray::class
            else -> throw Error("Invalid signature:$this")
        }
    }

val JDescriptor.isArray: Boolean get() = this[0] == '['

val JDescriptor.arrayType: KClass<*> get() = this.drop(1).type

/**
 * [JDescriptor] of array value
 */
val JDescriptor.arrDsc: String get() = this.drop(1)

val JDescriptor.isObj: Boolean get() = this[0] == 'L'

/**
 * Qualified name of the object class
 */
val JDescriptor.objClz: String get() = this.drop(1)

val JDescriptor.isMethod: Boolean get() = this[0] == '('

/**
 * [JDescriptor] of return value
 */
val JDescriptor.retDsc: JDescriptor get() = this.substringAfter(')')