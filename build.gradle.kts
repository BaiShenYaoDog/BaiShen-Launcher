import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.ZipFile

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val isOfficial = System.getenv("BSL_SIGNATURE_KEY") != null
        || (System.getenv("GITHUB_REPOSITORY_OWNER") == "ChengZhiNB" && System.getenv("GITHUB_BASE_REF")
    .isNullOrEmpty())

val buildNumber = System.getenv("BUILD_NUMBER")?.toInt().let { number ->
    val offset = System.getenv("BUILD_NUMBER_OFFSET")?.toInt() ?: 0
    if (number != null) {
        (number - offset).toString()
    } else {
        val shortCommit = System.getenv("GITHUB_SHA")?.toLowerCase()?.substring(0, 7)
        val prefix = if (isOfficial) "dev" else "unofficial"
        if (!shortCommit.isNullOrEmpty()) "$prefix-$shortCommit" else "SNAPSHOT"
    }
}
val versionRoot = System.getenv("VERSION_ROOT") ?: "1.0.3"
val versionType = System.getenv("VERSION_TYPE") ?: if (isOfficial) "nightly" else "unofficial"

version = "$versionRoot.$buildNumber"

dependencies {
    implementation(project(":BSLCore"))
    implementation("libs:JFoenix")
}

fun digest(algorithm: String, bytes: ByteArray) = MessageDigest.getInstance(algorithm).digest(bytes)

fun createChecksum(file: File) {
    val algorithms = linkedMapOf(
        "MD5" to "md5",
        "SHA-1" to "sha1",
        "SHA-256" to "sha256",
        "SHA-512" to "sha512"
    )

    algorithms.forEach { (algorithm, ext) ->
        File(file.parentFile, "${file.name}.$ext").writeText(
            digest(algorithm, file.readBytes()).joinToString(separator = "", postfix = "\n") { "%02x".format(it) }
        )
    }
}

fun attachSignature(jar: File) {
    val keyLocation = System.getenv("BSL_SIGNATURE_KEY")
    if (keyLocation == null) {
        logger.warn("Missing signature key")
        return
    }

    val privatekey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(File(keyLocation).readBytes()))
    val signer = Signature.getInstance("SHA512withRSA")
    signer.initSign(privatekey)
    ZipFile(jar).use { zip ->
        zip.stream()
            .sorted(Comparator.comparing { it.name })
            .filter { it.name != "META-INF/BSL_signature" }
            .forEach {
                signer.update(digest("SHA-512", it.name.toByteArray()))
                signer.update(digest("SHA-512", zip.getInputStream(it).readBytes()))
            }
    }
    val signature = signer.sign()
    FileSystems.newFileSystem(URI.create("jar:" + jar.toURI()), emptyMap<String, Any>()).use { zipfs ->
        Files.newOutputStream(zipfs.getPath("META-INF/BSL_signature")).use { it.write(signature) }
    }
}

val java11 = sourceSets.create("java11") {
    java {
        srcDir("src/main/java11")
    }
}

tasks.getByName<JavaCompile>(java11.compileJavaTaskName) {
    if (JavaVersion.current() < JavaVersion.VERSION_11) {
        javaCompiler.set(javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        })
    }
    options.compilerArgs.add("--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED")
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.jar {
    enabled = false
    dependsOn(tasks["shadowJar"])
}

val jarPath = tasks.jar.get().archiveFile.get().asFile

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set(null as String?)

    minimize {
        exclude(dependency("com.google.code.gson:.*:.*"))
        exclude(dependency("com.github.steveice10:.*:.*"))
        exclude(dependency("libs:JFoenix:.*"))
    }

    manifest {
        attributes(
            "Created-By" to "Copyright(c) 2023 ChengZhiYa",
            "Main-Class" to "cn.ChengZhiYa.BaiShenLauncher.Main",
            "Multi-Release" to "true",
            "Implementation-Version" to project.version,
            "Build-Channel" to versionType,
            "Class-Path" to "pack200.jar",
            "Add-Opens" to listOf(
                "java.base/java.lang",
                "java.base/java.lang.reflect",
                "java.base/jdk.internal.loader",
                "javafx.base/com.sun.javafx.binding",
                "javafx.base/com.sun.javafx.event",
                "javafx.base/com.sun.javafx.runtime",
                "javafx.graphics/javafx.css",
                "javafx.graphics/com.sun.javafx.stage",
                "javafx.graphics/com.sun.prism",
                "javafx.controls/com.sun.javafx.scene.control",
                "javafx.controls/com.sun.javafx.scene.control.behavior",
                "javafx.controls/javafx.scene.control.skin"
            ).joinToString(" ")
        )

        System.getenv("GITHUB_SHA")?.also {
            attributes("GitHub-SHA" to it)
        }
    }

    doLast {
        attachSignature(jarPath)
        createChecksum(jarPath)
    }
}

fun createExecutable(suffix: String, header: String) {
    val output = File(jarPath.parentFile, jarPath.nameWithoutExtension + '.' + suffix)

    output.outputStream().use {
        it.write(File(project.projectDir, header).readBytes())
        it.write(jarPath.readBytes())
    }

    createChecksum(output)
}

tasks.processResources {
    fun convertToBSS(resource: String) {
        doFirst {
            val cssFile = File(projectDir, "src/main/resources/$resource")
            val bssFile = File(projectDir, "build/compiled-resources/${resource.substring(0, resource.length - 4)}.bss")
            bssFile.parentFile.mkdirs()
            javaexec {
                classpath = sourceSets["main"].compileClasspath
                mainClass.set("com.sun.javafx.css.parser.Css2Bin")
                args(cssFile, bssFile)
            }
        }
    }

    from("build/compiled-resources")

    convertToBSS("assets/css/root.css")
    convertToBSS("assets/css/pink.css")

    into("META-INF/versions/11") {
        from(sourceSets["java11"].output)
    }
    dependsOn(tasks["java11Classes"])

    into("assets") {
        from(project.buildDir.resolve("openjfx-dependencies.json"))
    }
    dependsOn(rootProject.tasks["generateOpenJFXDependencies"])
}

val makeExecutables = tasks.create("makeExecutables") {
    dependsOn(tasks.jar)
    doLast {
        createExecutable("exe", "src/main/resources/assets/BaiShen-Launcher.exe")
        createExecutable("sh", "src/main/resources/assets/BaiShen-Launcher.sh")
    }
}

tasks.build {
    dependsOn(makeExecutables)
}

tasks.create<JavaExec>("run") {
    dependsOn(tasks.jar)

    group = "application"

    classpath = files(jarPath)
    workingDir = rootProject.rootDir
}
