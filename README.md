# Centralizing dependencies in buildSrc convention plugins with version catalog access

This guide demonstrates how to implement convention plugins using precompiled scripts in `buildSrc` to centralize
dependencies while leveraging the root version catalog.

## Versions Used

- Java: 21
- Gradle wrapper: 8.12.1
- Gradle with Kotlin DSL

## Project Structure

Our project consists of two modules that share the same framework (Spring Boot) but have different dependencies.
We need to establish a way to share common dependencies and configurations between these projects while maintaining the
ability to provide module-specific configurations.

The first module uses the *Spring Web* dependency, while the second uses *Spring Reactive Web*. Despite their
differences, both modules need to share certain configurations and common dependencies.

## Creating the Version Catalog

First we define all of our versions in a centralized version catalog.

We'll create a [`libs.versions.toml`](gradle/libs.versions.toml) file in the `gradle` folder where we'll define our list
of dependencies and plugins:

```toml
[versions]
spring-boot = "3.4.2"
spring-dependency-management = "1.1.7"
mapstruct = "1.6.3"

[libraries]
# MapStruct is a code generator that simplifies the implementation of mappings between Java bean types
mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
# The Spring Dependency Management plugin provides Maven BOM management for Gradle and helps us to inherit library versions that are compatible with our chosen Spring Boot version
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "spring-dependency-management" }
```

## Setting Up buildSrc

Create a `buildSrc` directory and add basic configurations in [`build.gradle.kts`](buildSrc/build.gradle.kts#L1-L7)

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}
```

and in [`settings.gradle.kts`](buildSrc/settings.gradle.kts#L1):

```kotlin
rootProject.name = "buildSrc"
```

## Creating the Convention Plugin

### Defining Common Configurations

Create the convention plugin [
`spring-boot-conventions.gradle.kts`](buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts#L1-L25) in
`buildSrc/src/main/kotlin` to define the common configurations:

```kotlin
plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Defining the MapStruct Dependency

In order to be able to access the version catalog from the convention plugin created in the previous step, we need to:

1. Add this dependency to the [`buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts#L12):

    ```kotlin
    dependencies {
        implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    }
    ```

2. Create the Kotlin file [`VersionCatalog.kt`](buildSrc/src/main/kotlin/VersionCatalog.kt) in
   `buildSrc/src/main/kotlin`:

    ```kotlin
    import org.gradle.accessors.dm.LibrariesForLibs
    import org.gradle.api.Project
    import org.gradle.kotlin.dsl.the

    val Project.libs: LibrariesForLibs
        get() = the()
    ```

3. Add the dependency declarations using the version catalog to the [
   `buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts`](buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts#L36-L37):

    ```kotlin
    dependencies {
        implementation(libs.mapstruct)
        annotationProcessor(libs.mapstruct.processor)
    }
    ```

### Defining the common SpringBoot plugins and dependencies

#### Declare the plugins as implementation dependencies of the buildSrc module

Setting up plugins from a version catalog within a convention plugin (written as a precompiled script) requires some
special consideration.

To begin, we need to declare the plugins as implementation dependencies of the buildSrc module. Instead of using their
plugin IDs, we'll use their **Maven artifact coordinates**.

Here are the Maven coordinates for our required plugins:

| Gradle plugin id                | Maven artifact cordinates                                                                                                                                                                         |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| org.springframework.boot        | [org.springframework.boot:spring-boot-gradle-plugin](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-gradle-plugin)                                                       |
| io.spring.dependency-management | [io.spring.dependency-management:io.spring.dependency-management.gradle.plugin](https://mvnrepository.com/artifact/io.spring.dependency-management/io.spring.dependency-management.gradle.plugin) |

Without a version catalog, we would declare these dependencies in [
`buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts) like this:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.4.2")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.7")
}
```

#### Using plugin marker artifact cordinates instead of Maven artifact cordinates

As suggested in
this [answer](https://discuss.gradle.org/t/applying-a-plugin-version-inside-a-convention-plugin/42160/3), we can
simplify this process using
the [plugin marker artifact cordinates](https://discuss.gradle.org/t/applying-a-plugin-version-inside-a-convention-plugin/42160/3).
This approach provides a predictable plugin artifact coordinate in the format
`<plugin id>:<plugin id>.gradle.plugin:<version>`.

> Note: The Spring Dependency Management Plugin already uses the marker artifact format

Using this format, we can rewrite the `dependencies` block in [`buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts)
as:

```kotlin
dependencies {
    implementation("org.springframework.boot:org.springframework.boot.gradle.plugin:3.4.2")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.7")
}
```

#### Version catalog integration

To leverage the version catalog, we could declare these plugins in [
`gradle/libs.versions.toml`](gradle/libs.versions.toml) under the `libraries` section using marker artifact coordinates:

```toml
[libraries]
spring-boot = { module = "org.springframework.boot:org.springframework.boot.gradle.plugin", version.ref = "spring-boot" }
spring-dependency-management = { module = "io.spring.dependency-management:io.spring.dependency-management.gradle.plugin", version.ref = "spring-dependency-management" }
```

Then reference them in the [`buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts):

```kotlin
dependencies {
    implementation(libs.spring.boot)
    implementation(libs.spring.dependencies)
}
```

However, to maintain better organization, we should keep these declarations in the `plugins` section instead of the
`libraries` section the version catalog.
To make this work, we can add a helper function in [`buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts#L15-L27) that
converts the version catalog plugin key to the relative marker artifact string coordinates:

```kotlin
/**
 * Maps a version catalog plugin key into a string that represents the plugin marker artifact format.
 *
 * Plugin marker artifacts follow the pattern: `group.id:group.id.gradle.plugin:version`
 * For plugins, the group ID and artifact ID are typically the same as the plugin ID.
 *
 * @param plugin A [Provider] containing the plugin dependency information
 * @return A string in the format `pluginId:pluginId.gradle.plugin:version`
 * @see <a href="https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers">Gradle Plugin Marker Artifacts</a>
 */
fun plugin(plugin: Provider<PluginDependency>) = plugin.map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}"
    }
```

With this helper function in place, we can now:

1. Declare dependencies in [`buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts#L10-L11):

    ```kotlin
    dependencies {
        implementation(plugin(libs.plugins.spring.boot))
        implementation(plugin(libs.plugins.spring.dependency.management))
    }
    ```

2. Apply the [plugins](buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts#L3-L4) and
   the [dependencies](buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts#L28-L35) in [
   `buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts`](buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts):

    ```kotlin
    plugins {
        id("org.springframework.boot")
        id("io.spring.dependency-management")
    }

    ...

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("io.micrometer:micrometer-tracing-bridge-brave")
        compileOnly("org.projectlombok:lombok")
        developmentOnly("org.springframework.boot:spring-boot-devtools")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
    ```

   Key points to note:

    1. Plugin versions aren't needed here since they're defined in [
       `buildSrc/build.gradle.kts`](buildSrc/build.gradle.kts)
    2. Spring Boot-related dependencies don't require version numbers as they're managed by the Spring dependency
       management plugin

## Applying the convention plugin and adding module-specific dependencies

For the `spring-reactive-demo` module, add to the [
`spring-reactive-demo/build.gradle.kts`](spring-reactive-demo/build.gradle.kts) the convention plugin and the Spring Web
dependency:

```kotlin
plugins {
    id("spring-boot-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
```

Similarly, for the `spring-web-demo` module, add to [
`spring-web-demo/build.gradle.kts`](spring-web-demo/build.gradle.kts) the convention plugin and the Spring Reactive Web
dependency:

```kotlin
plugins {
    id("spring-boot-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

Note that version numbers for Spring Boot dependencies aren't needed here either, as they're managed by the
dependency-management plugin available through our `spring-boot-conventions` plugin.

## Final considerations

Since we are using the `buildSrc` module we need to take in consideration its build related performance problem.

As the [Gradle docs](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources) say:

> ℹ️
>
> A change in buildSrc causes the whole project to become out-of-date.
>
> Thus, when making small incremental changes, the --no-rebuild command-line option is often helpful to get faster
> feedback. Remember to run a full build regularly.

To address this issue, we can move our logic from buildSrc into a separate module and include it as part of a composite
build following the approach demonstrated in
this [article](https://proandroiddev.com/stop-using-gradle-buildsrc-use-composite-builds-instead-3c38ac7a2ab3).

Take a look at
this [example](https://github.com/leonardo-marziali/gradle-playground/tree/centralizing-dependencies-in-composite-build-convention-plugins-with-version-catalog)
that shows how to convert this project to the composite build version.
