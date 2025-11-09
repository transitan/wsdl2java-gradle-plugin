plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "io.github.transitan"
version = "1.1"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// The integration test folder is an input to the unit test in the root project
// Register these files as inputs
tasks.withType<Test>().configureEach {
    inputs
        .files(layout.projectDirectory.dir("integration-test").asFileTree.matching {
            exclude("**/build/**")
            exclude("**/gradle/**")
        })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    useJUnitPlatform()
    systemProperty("GRADLE_ROOT_FOLDER", projectDir.absolutePath)
    systemProperty("GRADLE_PLUGIN_VERSION", version)
}

tasks.withType<Wrapper> {
    gradleVersion = "latest"
}

dependencies {
    compileOnly("org.apache.cxf:cxf-tools-wsdlto-core:+")
    testImplementation("commons-io:commons-io:+")
    testImplementation("org.junit.jupiter:junit-jupiter-api:+")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    compileOnly("org.projectlombok:lombok:+")
    testImplementation("org.projectlombok:lombok:+")
    testAnnotationProcessor("org.projectlombok:lombok:+")
}

gradlePlugin {
    website.set("https://github.com/transitan/wsdl2java-gradle-plugin")
    vcsUrl.set("https://github.com/transitan/wsdl2java-gradle-plugin")
    plugins {
        create("wsdl2JavaLombokPlugin") {
            id = "io.github.transitan.wsdl2java"
            displayName = "Gradle Wsdl2Java plugin With Lombok Support"
            tags.set(listOf("wsdl2java", "cxf", "wsimport"))
            implementationClass = "com.github.bjornvester.wsdl2java.Wsdl2JavaPlugin"
            description = "WSDL2 Java Plugin with Lombok support"
        }
    }
}
