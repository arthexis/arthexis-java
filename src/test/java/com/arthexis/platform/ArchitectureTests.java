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

  @Test
  void usersBillingAndFirmwareModulesDoNotDependOnEachOther() {
    var classes = new ClassFileImporter().importPackages("com.arthexis.platform");

    noClasses()
        .that()
        .resideInAPackage("..users..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..billing..", "..firmware..")
        .check(classes);

    noClasses()
        .that()
        .resideInAPackage("..billing..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..users..", "..firmware..")
        .check(classes);

    noClasses()
        .that()
        .resideInAPackage("..firmware..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..users..", "..billing..")
        .check(classes);
  }
}
