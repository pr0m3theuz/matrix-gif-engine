plugins {
	application
	kotlin("jvm") version "2.3.20"
	kotlin("plugin.serialization") version "2.3.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation("io.github.oshai:kotlin-logging:7.0.7")
	implementation("ch.qos.logback:logback-classic:1.5.38")
	testImplementation(kotlin("test"))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

	implementation("org.jetbrains.kotlinx:multik-default:0.3.1")
	// Source: https://mvnrepository.com/artifact/org.hdfgroup/hdf-java
	implementation("org.hdfgroup:hdf-java:2.6.1")

	// Source: https://mvnrepository.com/artifact/commons-cli/commons-cli
	implementation("commons-cli:commons-cli:1.11.0")
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
//		freeCompilerArgs.add("-Xmx8g -Xmx16g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/peachyfox/Downloads/matrx_gipf/log/java")
	}
}


tasks {
	val fatJar = register<Jar>("fatJar") {
		dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
		archiveClassifier.set("standalone") // Naming the jar
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		manifest { attributes(mapOf("Main-Class" to "org.example.MainKt")) } // Provided we set it up in the application plugin configuration
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