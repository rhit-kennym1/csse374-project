# Architectural Design Wiki

## 1) Design Purpose

We're developing our architecture as part of a project, so the design's focus will be on satisfying the requirements and preparing for an eventual release.

This design is based on a brownfield system, so the design focus is on thoroughly understanding the existing system. Our design is in a mature domain, so that we can use existing system examples as guidance for our solution.

---

## 2) Quality Attributes

| Attribute | Example Concrete Scenario | How We're Going to Enable It |
|-----------|---------------------------|------------------------------|
| Usability | User can select which checks to run and analyze a project or codebase with understandable output | Config file to hold the various linter check types and load the user's selections. Well formatted console output to display results of linter checks. |
| Maintainability | Developers can add linter checks without modifying existing code | Use of Open-Close principle through use of Strategy patterns while developing linter check classes |
| Cohesion | Each linter check is self contained in its own class and modifications don't require changing external files | Single Responsibility Principle - One class per check and use of three layer design separating domain classes (linter check logic) from data and presentation layers (console and file functionality) |
| Testability | Specific linter checks can be individually tested using sample Java projects that pass or fail them | JUnit tests with simple skeleton java projects to load in. |

---

## 3) Primary Functionality

The linter has to catch all problems, as seen below:

- Check naming conventions
- Warn of potentially bad programming practice
- Classes that define one of the equals() and hashCode() methods, but not both methods
- Tight coupling
- Poor cohesion
- Redundant interfaces
- Instances of the "Bad Patterns"
- Poorly written implementations of good design patterns (Decorator, especially, can be tricky to get right)
- Missing implementations of abstract types
- Classes that can't be publicly constructed
- Global state
- Check unused variable/method/class and give warnings
- Check whether the source code violates any user-defined coding style rules
- Warn of potentially incorrect programming
- Spell checks
- Tight coupling check
- Poor cohesion check
- Violations of Design Principles
- Check for poor/inconsistent naming conventions

The linter needs to be able to give output clearly and succinctly for the user.

The linter has to have a good settings configuration file that can be easily accessed and modified by the user for their own needs and purposes, for their own output, selected checks, etc.

The linter should be able to be run either on the command line or its own GUI.

The linter should be able to add checks without modifying existing code.

---

## 4) Architectural Concerns

### General Concerns
- Define a clear overall system structure and architectural layer
- Divide responsibility across different components to maintain high cohesion
- Support development by multiple teammates at a time
- Organize the codebase so things are easy to locate and understand
- Manage system startup and flow in a consistent and simple way

### Specific Concerns
- Manage dependencies between different components/layers
- Configuration of which linting rules are enabled/disabled at a time
- Error handling
- Logging authentication
- Reporting of results/analysis of linting rules

### Internal Requirements
- Be able to test linting rules separately
- Able to support the addition of new features
- Consistent interfaces for all lints
- Clear separation between linter logic and output format
- Maintaining the system as the number of rules increases

### Issues
- Risk of coupling if our linter rules need to depend on parsing or file-related logic
- Ensure consistent behaviour across all the different checks
- Balance flexibility and simplicity in the architecture
- Manage high complexity as the architecture grows bigger

---

## 5) Constraints

- Individually assigned features can't break other people's code
- Tests aren't allowed to call other teammates' code
- Separate class for each linter
- Load Java classes from any directory
- Separate run configurations for different tests
- Ability to add new linters without changing other classes
- Pushing to main branch is not allowed without approval of teammate
- Must be a Maven Project
- Must use ASM to read Bytecode