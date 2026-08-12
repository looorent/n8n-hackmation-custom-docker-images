pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
	plugins {
		id(providers.gradleProperty("quarkusPluginId").get()) version providers
			.gradleProperty("quarkusPluginVersion")
			.get()
	}
}

rootProject.name = "hackmation-java-qr-code-cli"
