package kni

import native.jni.*

/**
 * Return value of a JNI call
 */
typealias JResult = jint

/**
 * Name map of the JResult
 */
@Suppress("SpellCheckingInspection")
val namedJResult = mapOf(
    JNI_OK to "JNI_OK",
    JNI_ERR to "JNI_ERR",
    JNI_EDETACHED to "JNI_EDETACHED",
    JNI_EVERSION to "JNI_EVERSION",
    JNI_ENOMEM to "JNI_ENOMEM",
    JNI_EEXIST to "JNI_EEXIST",
    JNI_EINVAL to "JNI_EINVAL"
)

/**
 * Return true if [JResult] is [JNI_OK]
 */
inline val JResult.succeed: Boolean get() = this == JNI_OK

/**
 * Do action if JNI call succeed
 */
inline fun JResult.ifSucceed(action: (JResult) -> Unit) {
    if (succeed) action(this)
}

/**
 * Do action if JNI call failed
 */
inline fun JResult.ifFailed(action: (JResult) -> Unit) {
    if (!succeed) action(this)
}

/**
 * Throws when a JNI call failed
 */
open class JNIError(result: JResult, message: String?) :
    Error("$message->JResult:${namedJResult.getOrElse(result) { "$result" }}")

/**
 * Throws when there is a pending exception in VM
 */
open class VMException(message: String?) : Exception(message)

/**
 * Throws when [JavaVM] runs out of memory
 */
class VMOutOfMemoryException(message: String?) : VMException(message)

/**
 * Throws [Error] if the JNI call failed
 *
 * @throws OutOfMemoryError if [JNI_ENOMEM]
 * @throws JNIError in other conditions
 */
fun JResult.succeedOrThr(message: String? = null) {
    this.ifFailed { result ->
        when (result) {
            JNI_ENOMEM -> throw VMOutOfMemoryException(message)
            else -> throw JNIError(result, message)
        }
    }
}