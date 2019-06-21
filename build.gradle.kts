plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.21")
    id("org.openjfx.javafxplugin").version("0.0.7")
    application
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    compile("no.tornado:tornadofx:1.7.17")
}

javafx {
    version = "11.0.1"
    modules = listOf(
        "javafx.controls",
        "javafx.graphics"
    )
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
application {
    mainClassName = "io.github.lambdallama.UiKt"
}
