import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.21")
    id("com.github.johnrengelman.shadow").version("4.0.2")
    application
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.beust:klaxon:5.0.1")
    compile("com.google.guava:guava:28.0-jre")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks {
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

application {
    mainClassName = "io.github.lambdallama.MainKt"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}