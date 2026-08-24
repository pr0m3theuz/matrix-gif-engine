plugins {
	kotlin("jvm")
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
}

dependencies {
	implementation(project(":core"))

	// Mixite missing in desktop target
	implementation("com.github.Hexworks.mixite:mixite.core-jvm:2018.2.0-RELEASE")

	implementation(compose.desktop.currentOs)
	implementation(libs.kotlinx.coroutinesSwing)
	implementation(libs.compose.uiToolingPreview)
	implementation(libs.compose.runtime)
	implementation(libs.compose.foundation)
	implementation(libs.compose.material3)
	implementation(libs.compose.ui)
	implementation(libs.compose.components.resources)
	implementation(libs.androidx.lifecycle.viewmodelCompose)
	implementation(libs.androidx.lifecycle.runtimeCompose)
	implementation("androidx.compose.material:material-icons-core:1.7.8")
	runtimeOnly("androidx.compose.material:material-icons-extended:1.7.8")
}

kotlin {
	jvmToolchain(21)
}

tasks {
	val fatJar = register<Jar>("fatJar") {
		dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
		archiveClassifier.set("standalone") // Naming the jar
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//		manifest { attributes(mapOf("Main-Class" to "org.example.MainKt")) } // /Provided we set it up in the application plugin configuration
		val sourcesMain = sourceSets.main.get()
		val contents = configurations.runtimeClasspath.get()
			.map { if (it.isDirectory) it else zipTree(it) } +
				sourcesMain.output
		from(contents)
	}
	build {
		dependsOn(fatJar) // Trigger fat jar creation during build
	}
}
