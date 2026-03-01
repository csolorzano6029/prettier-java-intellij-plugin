
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.prettierjavaplugin"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        bundledPlugin("com.intellij.java")
    }
}

// ============================================================
// NPM: Install prettier + prettier-plugin-java in resources
// ============================================================
val prettierNodeDir = file("src/main/resources/prettier-node")

val npmInstall by tasks.registering(Exec::class) {
    group = "build"
    description = "Installs NPM dependencies for the Prettier runner"
    workingDir = prettierNodeDir
    commandLine = listOf("cmd", "/c", "npm", "install", "--omit=dev", "--legacy-peer-deps")
    inputs.file(File(prettierNodeDir, "package.json"))
    outputs.dir(File(prettierNodeDir, "node_modules"))
}

// Zip node_modules so we include ONE file in the JAR instead of thousands
val zipNodeModules by tasks.registering(Zip::class) {
    group = "build"
    description = "Zips node_modules into a single archive for bundling"
    dependsOn(npmInstall)
    from(File(prettierNodeDir, "node_modules")) {
        into("") // root of zip = node_modules root
    }
    destinationDirectory.set(prettierNodeDir)
    archiveFileName.set("node_modules.zip")
    inputs.dir(File(prettierNodeDir, "node_modules"))
    outputs.file(File(prettierNodeDir, "node_modules.zip"))
}

tasks {
    processResources {
        dependsOn(zipNodeModules)
        // Exclude the raw node_modules dir — only include the zip
        exclude("prettier-node/node_modules/**")
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("253.*")
    }

}

kotlin {
    jvmToolchain(17)
}
