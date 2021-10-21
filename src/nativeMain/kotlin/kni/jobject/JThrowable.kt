package kni.jobject

import kni.JEnv
import kni.succeedOrThr
import kni.toBoolean
import kotlinx.cinterop.invoke

/**
 * Instance of [kni.JavaVM] Throwable
 */
@Suppress("MemberVisibilityCanBePrivate")
class JThrowable(ref: JRef) : JObject(ref) {
    /**
     * Throw an exception to the VM
     *
     * @throws Error on failure
     */
    fun thr(env: JEnv) {
        env.nativeInf.Throw!!.invoke(env.internalEnv, ref.obj)
            .succeedOrThr("Throwing exception to VM")
    }
}

/**
 * Get pending exception in VM
 *
 * @param clear [clearException] at the same time?
 */
fun JEnv.getException(clear: Boolean = true): JThrowable? {
    return nativeInf.ExceptionOccurred!!.invoke(internalEnv)?.let {
        if (clear) clearException()
        JThrowable(JRefLocal(it))
    }
}

/**
 * Check if any pending exceptions in VM
 */
fun JEnv.checkException(): Boolean {
    return nativeInf.ExceptionCheck!!.invoke(internalEnv).toBoolean()
}

/**
 * Clears any exception that is currently being thrown.
 * If no exception is currently being thrown, this routine has no effect.
 */
fun JEnv.clearException() {
    nativeInf.ExceptionClear!!.invoke(internalEnv)
}

/**
 * Prints an exception and a backtrace of the stack to a system error-reporting channel, such as stderr.
 * This is a convenience routine provided for debugging.
 */
fun JEnv.printException() {
    nativeInf.ExceptionDescribe!!.invoke(internalEnv)
}