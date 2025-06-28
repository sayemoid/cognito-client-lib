import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidLibrary)
	alias(libs.plugins.serialization)
	id("com.github.gmazzo.buildconfig") version "5.6.7"
//	id("module.publication")
}

buildConfig {
	packageName("org.cognitox.clientlib")
	className("BuildKonfig")
	buildConfigField("String", "APP_NAME", "\"${project.property("app.name")}\"")
	buildConfigField("String", "VERSION_NAME", "\"${project.property("app.versionName")}\"")
	buildConfigField("String", "VERSION_CODE", "\"${project.property("app.versionCode")}\"")
}

kotlin {
	jvm {
		// Set compiler options directly here
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			// Set JVM target using the new DSL
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}
	androidTarget {
		publishLibraryVariants("release")
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}
	iosX64()
	iosArm64()
	iosSimulatorArm64()
//	linuxX64()

	sourceSets {
		val commonMain by getting {
			dependencies {
				api(libs.arrow.core)
				api(libs.arrow.fx.coroutines)
				// DateTime
				api(libs.kotlinx.datetime)

				// Logging
				implementation(libs.kermit)
				implementation(libs.kermit.stately)

				// Serialization
				api(libs.kotlinx.serialization)
				// https://mvnrepository.com/artifact/com.soywiz.korlibs.krypto/krypto
				implementation(libs.krypto)
				// ktor
				implementation(libs.ktor)
				implementation(libs.ktor.client.content.negotiation)
				implementation(libs.ktor.serialization.kotlinx.json)
				implementation(libs.ktor.client.auth)
				api(libs.krossbow.stomp.core)
				implementation(libs.krossbow.websocket.ktor)
				api(libs.krossbow.stomp.kxserialization)
				// DI
				api(libs.koin.core)
				api(libs.koin.test)
				// DataStore
				api(libs.datastore.preferences.core)

				// Capture Crashes
				api(libs.sentry.kotlin.multiplatform)
			}
		}
		val commonTest by getting {
			dependencies {
				implementation(libs.kotlin.test)
			}
		}

		val androidMain by getting {
			dependencies {
				api(libs.koin.android)
				api(libs.ktor.client.okhttp)
			}
		}
		val iosMain by creating {
			dependencies {
				api(libs.ktor.client.darwin)
			}
		}
		val jvmMain by getting {
			dependencies {
				api(libs.ktor.client.okhttp)
			}
		}
//		val linuxX64Main by getting {
//			dependencies {
//				implementation(libs.ktor.client.cio)
//			}
//		}
	}
}

android {
	namespace = "org.cognitox.clientlib"
	compileSdk = libs.versions.android.compileSdk.get().toInt()
	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
	}
	buildFeatures {
		buildConfig = true
	}
	kotlin {
		jvmToolchain(libs.versions.jdk.get().toInt())
	}
}
