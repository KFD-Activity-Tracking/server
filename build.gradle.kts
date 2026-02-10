plugins {

    var plugin_version = "1.9.25"


    id("org.springframework.boot") version "3.5.7"
    kotlin("jvm") version plugin_version
    kotlin("plugin.spring") version plugin_version
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("plugin.noarg") version plugin_version
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Activity tracking app"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {

    //drivers
    implementation("mysql:mysql-connector-java:8.0.33")

    //starters
	implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")



	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
