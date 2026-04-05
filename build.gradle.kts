plugins {

    var plugin_version = "1.9.25"


    id("org.springframework.boot") version "3.5.7"
    kotlin("jvm") version plugin_version
    kotlin("plugin.spring") version plugin_version
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("plugin.noarg") version plugin_version
    kotlin("plugin.jpa") version plugin_version
}


noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
    // Add this to generate no-arg constructors for data classes
    invokeInitializers = true
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
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


    //auth
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-crypto")


    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")


    implementation("org.junit.jupiter:junit-jupiter:5.9.2")

    implementation("com.h2database:h2")


    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(kotlin("test"))
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
