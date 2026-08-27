plugins {
	application
	kotlin("jvm") version "2.3.20"
	kotlin("plugin.serialization") version "2.3.20"
	id("org.graalvm.buildtools.native") version "1.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
	mainClass.set("org.example.GenerateReproducibleGamesKt")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("io.github.oshai:kotlin-logging:7.0.7")
	implementation("ch.qos.logback:logback-classic:1.5.38")
	testImplementation(kotlin("test"))

	testImplementation("net.jqwik:jqwik:1.10.1")

	testImplementation("com.code-intelligence:jazzer-junit:0.24.0")
	testImplementation("com.code-intelligence:jazzer-api:0.24.0")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

	implementation("org.jetbrains.kotlinx:multik-default:0.3.1")
	// https://central.sonatype.com/artifact/io.jhdf/jhdf
	implementation("io.jhdf:jhdf:0.13.0")

	// Source: https://mvnrepository.com/artifact/commons-cli/commons-cli
	implementation("commons-cli:commons-cli:1.11.0")

	// Source: https://mvnrepository.com/artifact/org.duckdb/duckdb_jdbc
	implementation("org.duckdb:duckdb_jdbc:1.5.5.1")
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
		vendor.set(JvmVendorSpec.GRAAL_VM)
	}
	compilerOptions {
//		freeCompilerArgs.add("-Xmx8g -Xmx16g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/peachyfox/Downloads/matrx_gipf/log/java")
	}
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