plugins {
    id("java")
    id("com.github.bjornvester.wsdl2java.internal.java-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.apache.cxf:cxf-bom:3.5.6"))
    compileOnly("org.projectlombok:lombok:1.18.32")
    testImplementation("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
