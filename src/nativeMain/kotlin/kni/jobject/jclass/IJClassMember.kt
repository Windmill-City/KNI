package kni.jobject.jclass

import kni.JEnv
import kni.jobject.JObject

interface IJClassMember {
    /**
     * If the class member is static
     */
    val isStatic: Boolean

    /**
     * Convert a [IJClassMember] to a reflection object
     */
    fun toReflected(env: JEnv, clz: JClass): JObject
}