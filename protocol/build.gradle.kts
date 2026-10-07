plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.okhttp)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

application {
    // Read-only discovery CLI (see tools/README.md)
    mainClass.set("es.routerapp.protocol.discovery.DiscoverKt")
}

tasks.test {
    useJUnit()
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

// Read-only data model probe: gradlew :protocol:probe --args="<pwdFile> <user> OID1,OID2"
tasks.register<JavaExec>("probe") {
    group = "discovery"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("es.routerapp.protocol.discovery.ProbeKt")
    workingDir = rootProject.projectDir
}

// Read-only check of the screen catalog against the real router: gradlew :protocol:pagesCheck --args="<pwdFile> user"
tasks.register<JavaExec>("pagesCheck") {
    group = "discovery"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("es.routerapp.protocol.discovery.PagesCheckKt")
    workingDir = rootProject.projectDir
}

// Reversible write test (renames guest SSID and restores it): gradlew :protocol:writeTest --args="<pwdFile> user"
tasks.register<JavaExec>("writeTest") {
    group = "discovery"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("es.routerapp.protocol.discovery.WriteTestKt")
    workingDir = rootProject.projectDir
}

// Reversible add/delete test on port forwarding: gradlew :protocol:addDeleteTest --args="<pwdFile> user"
tasks.register<JavaExec>("addDeleteTest") {
    group = "discovery"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("es.routerapp.protocol.discovery.AddDeleteTestKt")
    workingDir = rootProject.projectDir
}

// Safe writes (restored and verified): gradlew :protocol:safeTests --args="<pwdFile> user"
tasks.register<JavaExec>("safeTests") {
    group = "discovery"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("es.routerapp.protocol.discovery.SafeTestsKt")
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("dashCheck") {
    group = "discovery"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("es.routerapp.protocol.discovery.DashCheckKt")
    workingDir = rootProject.projectDir
}
