pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven(url = "https://plan-maven.apal-research.com")
		maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "code"