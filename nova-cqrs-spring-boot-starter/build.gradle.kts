plugins {
    id("pe.edu.nova.java.library")
}

description = "Conector de CQRS con Spring Boot: los buses, los handlers como beans y los comportamientos de Nova."

val springBootVersion = "4.0.8"

repositories {
    // Los errores son los de nova-api-standard, que se publica en el GitHub Packages de su repositorio.
    maven {
        name = "NovaApiStandard"
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-01-api-standard")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("NOVA_PACKAGES_READ_TOKEN") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    api(project(":nova-cqrs"))

    // Spring Boot lo trae el servicio; el starter solo compila contra él, con las versiones de su BOM. Cada pieza
    // opcional se usa solo si el servicio la tiene: Micrometer, Bean Validation, transacciones y Spring Security.
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-tx")
    compileOnly("io.micrometer:micrometer-observation")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.slf4j:slf4j-api")
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework:spring-tx")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation("io.micrometer:micrometer-observation-test")
    testImplementation("ch.qos.logback:logback-classic")
}
