package pe.edu.nova.java.libs.cqrs;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** El núcleo es una librería pura (ADR-015): no conoce ningún framework, ni el de logging. */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("pe.edu.nova.java.libs.cqrs");

    @Test
    void theCoreDependsOnNoFramework() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..", "io.quarkus..", "jakarta..", "io.micrometer..", "org.slf4j..")
                .check(classes);
    }
}
