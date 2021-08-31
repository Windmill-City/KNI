package kni

import native.jni.*

/**
 * Return value of the JNI method
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
 * Do action if succeed
 */
inline fun JResult.ifSucceed(action: (JResult) -> Unit) {
    if (succeed) action(this)
}

/**
 * Do action if failed
 */
inline fun JResult.ifFailed(action: (JResult) -> Unit) {
    if (!succeed) action(this)
}

/**
 * Throw an exception if it fails
 *
 * @throws OutOfMemoryError if it is [JNI_ENOMEM]
 * @throws Error on failure
 */
fun JResult.succeedOrThr(message: String = "Failed") {
    this.ifFailed { result ->
        when (result) {
            JNI_ENOMEM -> throw OutOfMemoryError("$message->JResult:${namedJResult[this]}")
            else -> throw Error("$message->JResult:${namedJResult.getOrElse(this) { "$this" }}")
        }
    }
}