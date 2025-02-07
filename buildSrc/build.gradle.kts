plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(plugin(libs.plugins.spring.boot))
    implementation(plugin(libs.plugins.spring.dependency.management))
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

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
