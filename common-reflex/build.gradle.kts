import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    implementation("org.tabooproject.reflex:reflex:1.1.0")
    implementation("org.tabooproject.reflex:analyser:1.1.0")
}

tasks {
    withType<ShadowJar> {
        dependencies {
            include(dependency("org.tabooproject.reflex:reflex:1.1.0"))
            include(dependency("org.tabooproject.reflex:analyser:1.1.0"))
        }
        relocate("org.taboooproject", "taboolib.library")
    }
}