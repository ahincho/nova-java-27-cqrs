plugins {
    id("pe.edu.nova.java.library")
}

description = "Núcleo de CQRS de Nova: los contratos, el Command Bus, el Query Bus y sus comportamientos."

repositories {
    // Los errores son los de ADR-031, de nova-api-standard, que se publica en el GitHub Packages de su repositorio.
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
    // Es parte del contrato: la autorización rechaza con un ApplicationError, y un servicio lo atrapa por su tipo.
    api("pe.edu.nova.java.libs:nova-api-standard:1.1.0")

    testImplementation("com.tngtech.archunit:archunit:1.5.1")
}
