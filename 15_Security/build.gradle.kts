plugins {
    java
 //   `java-library`
    `maven-publish`
    jacoco
    application
    distribution
    groovy

    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "8.12"
    id("org.openapi.generator") version "7.10.0"
//    id("org.springframework.cloud.contract") version "4.2.0"
    id("org.sonarqube") version "6.0.1.5171"
}

group = "io.forest"
version = "latest"

repositories {
    mavenCentral()
}


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.28")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("jakarta.validation:jakarta.validation-api")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.4")


    //  implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")


    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

//dependencyManagement {
//    imports {
//        mavenBom("org.springframework.cloud:spring-cloud-contract-dependencies:4.2.0")
//    }
//}

sourceSets {
    main {
        java.srcDirs(
                "src/main/java", "build/generated/src/main/java"
        )
        resources.srcDirs(
                "src/main/resources"
        )
    }

    test {
        java.srcDirs(
                "src/test/java", "src/contractTest/java"
        )
        resources.srcDirs(
                "src/test/resources", "src/contractTest/resources"
        )
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
        vendor = JvmVendorSpec.ORACLE
    }
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(file("src/main/resources/contracts/server/forest-security-api.yml").absolutePath)
    outputDir.set(file("build/generated").absolutePath)
    apiPackage.set("io.forest.security.adapter.web.server")
    modelPackage.set("io.forest.security.adapter.web.server.model")
    generateModelTests.set(false)
    skipValidateSpec.set(false)
    templateResourcePath.set(file("src/main/resources/openapi/JavaSpring").absolutePath)
    configOptions.set(
            mapOf(
                    "dateLibrary" to "java8",
                    "documentationProvider" to "springdoc",
                    "generateBuilders" to "true",
                    // "delegatePattern" to "false",
                    "interfaceOnly" to "true",
                    "openApiNullable" to "true",
                    "useJakartaEe" to "true",
                    "useSpringBoot3" to "true"

            )
    )
    additionalProperties.set(
            mapOf(

            )
    )
}

tasks {
    withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy("jacocoTestReport")
    }
}