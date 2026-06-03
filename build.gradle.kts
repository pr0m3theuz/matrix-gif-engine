plugins {
	kotlin("jvm") version "2.3.20"
	kotlin("plugin.serialization") version "2.3.20"
	id("org.jetbrains.kotlinx.fuzz") version "1.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
	maven(url = "https://plan-maven.apal-research.com")
	maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}

dependencies {
	testImplementation(kotlin("test"))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
	testImplementation("org.jetbrains:kotlinx.fuzz.jazzer:1.0.0")
	testImplementation("org.jetbrains:jazzer-junit:0.0.5")
}

fuzzConfig {
	instrument = listOf("org.example.**")
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}
