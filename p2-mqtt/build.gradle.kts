plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    //logging für active mq nötig
    implementation("ch.qos.logback:logback-classic:1.5.13")

    // Eclipse Paho MQTT Client
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Gson für JSON-Serialisierung
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.apache.activemq:activemq-broker:5.18.4")
    implementation("org.apache.activemq:activemq-client:5.18.4")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}