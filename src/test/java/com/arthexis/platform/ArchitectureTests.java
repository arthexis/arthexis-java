package com.arthexis.platform;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTests {

  @Test
  void ocppModuleDoesNotDependOnSecurityModule() {
    var classes = new ClassFileImporter().importPackages("com.arthexis.platform");

    noClasses()
        .that()
        .resideInAPackage("..ocpp..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..security..")
        .check(classes);
  }
}
