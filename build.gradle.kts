@file:Suppress("UNUSED_VARIABLE")

plugins {
    kotlin("multiplatform") version "1.5.31"
    `maven-publish`
}

group = "city.windmill"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.compilations.getByName("main").cinterops.create("jni") {
        defFile(project.file("src/nativeMain/resources/jni/jni.def"))
        headers(
            project.files(
                "src/nativeMain/resources/jni/jni.h",
                "src/nativeMain/resources/jni/jni_md.h"
            )
        )
        packageName("native.jni")
    }



    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting
        val nativeTest by getting {
            //Set the working path to where the jvm library locates, or cause failure in JavaVM.create
            tasks["nativeTest"].setProperty("workingDir", "${System.getenv("JAVA_HOME")}/bin/default")
        }
    }

    publishing {
        repositories {
            maven {
                name = "GithubPackages"
                url = uri("https://maven.pkg.github.com/Windmill-City/KNI")
                credentials {
                    username = properties["gpr.user"]?.toString() ?: System.getenv("USERNAME")
                    password = properties["gpr.key"]?.toString() ?: System.getenv("TOKEN_PUBLISH_PKG")
                }
            }
        }
    }
}