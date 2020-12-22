import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import groovy.lang.GroovyObject
import org.junit.platform.gradle.plugin.JUnitPlatformPlugin
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig

group = "ca.cutterslade.gradle"
version = "1.0.0-beta-8-p44"

repositories {
  jcenter()
  maven("http://artifactory.internal-p-44.com/artifactory/plugins-release")
}

buildscript {
  dependencies {
    classpath("org.junit.platform:junit-platform-gradle-plugin:1.1.0")
    classpath("org.jfrog.buildinfo:build-info-extractor-gradle:latest.release")
  }
}
plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  `junit-test-suite`
  kotlin("jvm") version "1.2.61"
  id("com.gradle.plugin-publish") version "0.10.0"
  id("com.jfrog.artifactory") version "4.18.2"
id("maven-publish")
}

sourceSets.create("functionalTest") {
  compileClasspath += sourceSets["main"].output
  runtimeClasspath += output
  runtimeClasspath += compileClasspath
  runtimeClasspath += configurations["runtime"]
  runtimeClasspath += configurations["functionalTestRuntime"]
}

configurations["functionalTestCompile"].extendsFrom(configurations["compile"])
configurations["functionalTestRuntime"].extendsFrom(configurations["runtime"])

dependencies {
  compile(kotlin("stdlib"))
  compile(kotlin("stdlib-jre8"))
  compile(kotlin("reflect"))
  compile(gradleApi())
  compile("com.squareup.okhttp3:okhttp:3.11.0")

  testCompile(kotlin("test"))
  testCompile("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")

  add("functionalTestCompile", kotlin("test"))
  add("functionalTestCompile", "org.jetbrains.spek:spek-api:1.1.5")
  add("functionalTestCompile", "com.google.guava:guava:27.0-jre")
  add("functionalTestCompile", "org.glassfish.grizzly:grizzly-http-server:2.4.3")
  add("functionalTestRuntime", "org.junit.platform:junit-platform-engine:1.3.1")
  add("functionalTestRuntime", "org.jetbrains.spek:spek-junit-platform-engine:1.1.5")
}
publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            /*artifact(sourcesJar.get())*/
        }
    }
}
artifactory {
    setContextUrl("http://artifactory.internal-p-44.com/artifactory/")
    publish(delegateClosureOf<PublisherConfig> {
        setProperty("repoKey", "plugins-release-local")
        repository(delegateClosureOf<GroovyObject> {
            setProperty("username", "admin")
            setProperty("password", "admin")
        })
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", "mavenJava")
            setProperty("publishPom", true)
            setProperty("publishArtifacts", true)
        })
    })
    resolve(delegateClosureOf<ResolverConfig> {
        setProperty("repoKey", "libs-release")
    })
}

gradlePlugin {
  plugins {
    create("helm") {
      id = "ca.cutterslade.helm"
      implementationClass = "ca.cutterslade.gradle.helm.HelmPlugin"
    }
  }
  testSourceSets(sourceSets["functionalTest"])
}

pluginBundle {
  website = "https://github.com/wfhartford/gradle-helm"
  vcsUrl = "https://github.com/wfhartford/gradle-helm.git"
  tags = listOf("helm")
  description = "Plugin supporting basic helm commands for a gradle build."

  (plugins) {
    "helm" {
      id = "ca.cutterslade.helm"
      displayName = "Gradle Helm Plugin"
      version = project.version.toString().replace("-SNAPSHOT", "-${DateTimeFormatter.ofPattern("uuuuMMddHHmmss").format(LocalDateTime.now())}")
    }
  }
}

/*tasks["publishPlugins"].dependsOn(tasks["check"])*/

tasks {
  create<Test>("functionalTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    mustRunAfter("test")
  }.also { tasks["check"].dependsOn(it) }

  withType(KotlinCompile::class.java).all {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
  }
}

