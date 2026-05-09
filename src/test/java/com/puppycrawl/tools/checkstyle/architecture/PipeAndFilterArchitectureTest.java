///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// Licensed under the GNU Lesser General Public License v2.1.
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Encodes the Pipe-and-Filter invariants as ArchUnit rules R1–R12.
 *
 * <p>FR-009, FR-015, SC-003, SC-006 / T071–T086b. Each rule is a
 * separate {@code @Test} so failure messages stay focused.
 */
public class PipeAndFilterArchitectureTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("com.puppycrawl.tools.checkstyle");

    private static final String PIPELINE_CORE = "..checks.pipeline..";
    private static final String METRICS_PIPELINE = "..checks.metrics.pipeline..";
    private static final String SIZES_PIPELINE = "..checks.sizes.pipeline..";
    private static final String API = "com.puppycrawl.tools.checkstyle.api..";
    private static final String FILTER_INTERFACE =
            "com.puppycrawl.tools.checkstyle.checks.pipeline.Filter";

    /** R1: pipeline core classes must not extend Checkstyle Check base classes. */
    @Test
    public void r1_pipelineCoreDoesNotExtendCheckBases() {
        noClasses().that().resideInAPackage(PIPELINE_CORE)
            .should().beAssignableTo("com.puppycrawl.tools.checkstyle.api.AbstractCheck")
            .orShould().beAssignableTo(
                "com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck")
            .check(MAIN_CLASSES);
    }

    /** R2: per-check measurement filters must not extend Checkstyle Check base classes. */
    @Test
    public void r2_measurementFiltersDoNotExtendCheckBases() {
        noClasses().that()
                .resideInAnyPackage(METRICS_PIPELINE, SIZES_PIPELINE)
            .should().beAssignableTo("com.puppycrawl.tools.checkstyle.api.AbstractCheck")
            .orShould().beAssignableTo(
                "com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck")
            .check(MAIN_CLASSES);
    }

    /** R3: no filter implementation calls AbstractCheck.log(..). */
    @Test
    public void r3_filtersDoNotCallAbstractCheckLog() {
        noClasses().that()
                .resideInAnyPackage(PIPELINE_CORE, METRICS_PIPELINE, SIZES_PIPELINE)
            .should().callMethodWhere(target ->
                target.getTarget().getOwner().getName()
                    .equals("com.puppycrawl.tools.checkstyle.api.AbstractCheck")
                && target.getTarget().getName().equals("log"))
            .check(MAIN_CLASSES);
    }

    /** R4: every concrete class in a filter package implements {@code Filter<?,?>}. */
    @Test
    public void r4_concreteClassesInFilterPackagesImplementFilter() {
        classes().that()
                .resideInAnyPackage(
                    "..checks.pipeline.filter..",
                    METRICS_PIPELINE,
                    SIZES_PIPELINE)
                .and().areNotInterfaces()
                .and().areNotAnonymousClasses()
                .and().areNotMemberClasses()
                .and().haveSimpleNameNotEndingWith("package-info")
            .should().implement(FILTER_INTERFACE)
            .check(MAIN_CLASSES);
    }

    /**
     * R5: metrics pipeline depends only on JDK + Checkstyle core/utils + allow-listed
     * AST types + the pipeline core.
     */
    @Test
    public void r5_metricsPipelineDependencyAllowList() {
        classes().that().resideInAPackage(METRICS_PIPELINE)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "java..",
                PIPELINE_CORE,
                METRICS_PIPELINE,
                "com.puppycrawl.tools.checkstyle.api..",
                "com.puppycrawl.tools.checkstyle.utils..")
            .check(MAIN_CLASSES);
    }

    /** R6: sizes pipeline obeys the same allow-list as R5. */
    @Test
    public void r6_sizesPipelineDependencyAllowList() {
        classes().that().resideInAPackage(SIZES_PIPELINE)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "java..",
                PIPELINE_CORE,
                SIZES_PIPELINE,
                "com.puppycrawl.tools.checkstyle.api..",
                "com.puppycrawl.tools.checkstyle.utils..")
            .check(MAIN_CLASSES);
    }

    /**
     * R7: pipeline drivers depend on pipeline composition, not directly on concrete
     * filter types (interaction goes through {@code Pipeline}). Encoded weakly: every
     * driver references {@code Pipeline}.
     */
    @Test
    public void r7_driversReferencePipeline() {
        classes().that()
                .resideInAnyPackage(
                    "com.puppycrawl.tools.checkstyle.checks.metrics",
                    "com.puppycrawl.tools.checkstyle.checks.sizes")
                .and().haveSimpleNameEndingWith("Check")
                .and().areNotAbstract()
                .and().haveSimpleNameNotStartingWith("Abstract")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                "com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline")
            .check(MAIN_CLASSES);
    }

    /** R8: pipeline core does not depend on metrics or sizes packages. */
    @Test
    public void r8_pipelineCoreDoesNotDependOnSlicePackages() {
        noClasses().that().resideInAPackage(PIPELINE_CORE)
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.puppycrawl.tools.checkstyle.checks.metrics",
                METRICS_PIPELINE,
                "com.puppycrawl.tools.checkstyle.checks.sizes",
                SIZES_PIPELINE)
            .check(MAIN_CLASSES);
    }

    /**
     * R9: no filter has a field typed as another concrete {@code Filter} implementation.
     * Pipes are the only allowed inter-stage reference.
     */
    @Test
    public void r9_filtersDoNotHaveFieldsTypedAsOtherFilters() {
        fields().that().areDeclaredInClassesThat().implement(FILTER_INTERFACE)
            .should(new com.tngtech.archunit.lang.ArchCondition<
                    com.tngtech.archunit.core.domain.JavaField>(
                    "not be typed as a concrete Filter implementation other than self") {
                @Override
                public void check(
                        com.tngtech.archunit.core.domain.JavaField field,
                        com.tngtech.archunit.lang.ConditionEvents events) {
                    final com.tngtech.archunit.core.domain.JavaClass type =
                            field.getRawType();
                    final boolean concreteFilter =
                            !type.isInterface()
                            && !type.getModifiers().contains(
                                com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                            && type.getAllRawInterfaces().stream()
                                .anyMatch(i -> i.getFullName().equals(FILTER_INTERFACE));
                    final boolean isSelf = type.equals(field.getOwner());
                    if (concreteFilter && !isSelf) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                                field, field.getFullName()
                                    + " is typed as concrete filter " + type.getName()));
                    }
                }
            })
            .check(MAIN_CLASSES);
    }

    /** R10: Checkstyle {@code api} package does not depend on the pipeline. */
    @Test
    public void r10_apiDoesNotDependOnPipeline() {
        noClasses().that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAPackage(PIPELINE_CORE)
            .check(MAIN_CLASSES);
    }

}
