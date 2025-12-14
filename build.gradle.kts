plugins {
    id("java")
    id("application")
}

group = "io.tradecraft"
version = "1.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.quickfixj:quickfixj-core:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-fix44:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-all:2.3.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.apache.logging.log4j:log4j-api:2.25.0")
    implementation("org.apache.logging.log4j:log4j-core:2.25.0")
    implementation("org.apache.logging.log4j:log4j-jul:2.25.0")      // optional
    implementation("org.jctools:jctools-core:4.0.3")
    implementation("io.micrometer:micrometer-core:1.13.4")
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0") // <-- needed for mockStatic

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.1")
}


tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("io.tradecraft.TradeCraft")
    applicationDefaultJvmArgs = listOf(
        "-Dlog4j.configurationFile=src/main/resources/log4j2.xml",
        "-Duser.timezone=UTC"
    )
}

tasks.register<JavaExec>("runTradeCraft") {
    dependsOn("classes")
    group = "application"
    description = "Run the TradeCraft app"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.tradecraft.bootstrap.TradeCraft")
}

tasks.register<JavaExec>("runTradeClient") {
    dependsOn("classes")
    group = "application"
    description = "Run the TradeClient app"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.tradecraft.ext.TradeClient")
}

