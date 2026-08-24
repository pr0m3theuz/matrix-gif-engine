plugins {
	application
	kotlin("jvm")
}

dependencies {
	implementation(project(":core"))

	// Coroutines missing in CLI target for runBlocking
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
	// Serialization missing in CLI target
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

	implementation("commons-cli:commons-cli:1.11.0")
	implementation("com.github.ajalt.clikt:clikt:5.1.0")

    // Logging missing in CLI target
	implementation("io.github.oshai:kotlin-logging:7.0.7")
	implementation("ch.qos.logback:logback-classic:1.5.38")
}

application {
	mainClass.set("org.example.MainKt")
}

kotlin {
	jvmToolchain(21)
}

tasks {
	val fatJar = register<Jar>("fatJar") {
		dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
		archiveClassifier.set("standalone") // Naming the jar
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		manifest { attributes(mapOf("Main-Class" to "org.example.MainKt")) }
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
