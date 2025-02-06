# Gradle Playground

A collection of Gradle build configuration examples, patterns, and best practices.

## Usage

Each branch in this repository focuses on a specific Gradle feature or use case.

- [`main`](https://github.com/leonardo-marziali/gradle-playground) - Provides the fundamental Gradle project structure shared across all branches. Note that we've omitted the build.gradle.kts file since some branches demonstrate multi-module projects that don't require a root-level build script
- [`centralizing-dependencies-in-buildSrc-convention-plugins-with-version-catalog-access`](https://github.com/leonardo-marziali/gradle-playground/tree/centralizing-dependencies-in-buildSrc-convention-plugins-with-version-catalog-access) - Demonstrates how to implement convention plugins in buildSrc for centralizing dependencies while leveraging the root version catalog

## Default version used

- Java: 21
- Gradle wrapper: 8.12.1
