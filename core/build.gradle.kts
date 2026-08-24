plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
}

dependencies {
	implementation("io.github.oshai:kotlin-logging:7.0.7")
	implementation("ch.qos.logback:logback-classic:1.5.38")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
	implementation("org.jetbrains.kotlinx:multik-default:0.3.1")
	implementation("io.jhdf:jhdf:0.13.0")
	implementation("com.github.Hexworks.mixite:mixite.core-jvm:2018.2.0-RELEASE")

	testImplementation(kotlin("test"))
	testImplementation("net.jqwik:jqwik:1.10.1")
	testImplementation("com.code-intelligence:jazzer-junit:0.24.0")
	testImplementation("com.code-intelligence:jazzer-api:0.24.0")
	testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
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
