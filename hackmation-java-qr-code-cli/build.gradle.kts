plugins {
	java
	id("io.quarkus")
}

repositories {
	mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
	implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
	implementation("io.quarkus:quarkus-picocli")
	implementation("io.quarkus:quarkus-container-image-docker")
	implementation("com.google.zxing:core:3.5.3")

	testImplementation("io.quarkus:quarkus-junit5")
	// javase is only needed to decode QR codes in tests; it must stay out of
	// the runtime classpath to keep the native image free of java.awt.
	testImplementation("com.google.zxing:javase:3.5.3")
}

group = "dev.hackmation"
version = "1.0.0"

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.test {
	useJUnitPlatform()
	systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("-parameters")
}
