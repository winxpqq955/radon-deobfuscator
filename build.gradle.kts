plugins {
    kotlin("jvm") version "1.4.0-rc"
}

group = "com.github.gitantoinee.deobfuscator.radon"
version = "1.0.0"

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    explicitApi()

    target.compilations.forEach { compilation ->
        compilation.kotlinOptions.jvmTarget = "1.8"
        compilation.kotlinOptions.allWarningsAsErrors = true
    }
}
