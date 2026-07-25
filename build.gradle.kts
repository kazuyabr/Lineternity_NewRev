import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar 
import org.gradle.api.tasks.Copy 
import org.gradle.api.tasks.JavaExec 
import org.gradle.api.Task
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date

// 1. Plugins
plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "2.3.0-Beta2" 
}

// 2. Repositórios
repositories {
    mavenCentral()
    google()
    mavenLocal()
    maven { url = uri("https://artifacts.deepl.com/maven/") } 
}

// Define o caminho 'build/classes' para unificar classes Java e Kotlin (igual ao Ant)

val antClassesDir = layout.buildDirectory.dir("classes")

// 3. Estrutura de Pastas
sourceSets {
    main {
        java.srcDirs("java") 
        kotlin.srcDirs("kotlin") 
        kotlin {
            // As exclusões permanecem
        }
    }
}

// 3.5. Configuração de Compilação Híbrida (Compatibilidade Máxima)

// Configura JVM Toolchain para Java 25 (garante que Kotlin e Java usem JDK 25)
kotlin {
    jvmToolchain(25)
}

tasks.withType<JavaCompile> {
    // Apenas main compila para build/classes; test usa saída padrão (evita conflito com syncBinClasses)
    if (name == "compileJava") {
        destinationDirectory.set(antClassesDir)
    }
    // Evita cache que não restaura classes no build/classes
    outputs.cacheIf { false }
}

tasks.withType<KotlinCompile>().all {
    // Apenas main compila para build/classes; test usa saída padrão (evita conflito com syncBinClasses)
    if (name == "compileKotlin") {
        destinationDirectory.set(antClassesDir)
        // Garante que o diretório existe antes da compilação
        doFirst {
            antClassesDir.get().asFile.mkdirs()
        }
    }
    // Evita cache que não restaura classes no build/classes
    outputs.cacheIf { false }
    
    // Usa compilerOptions (nova API recomendada)
    compilerOptions {
        // Kotlin 2.3.0-Beta2+ suporta JVM target 25
        // Usa JVM_25 diretamente (suportado nesta versão)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        freeCompilerArgs.addAll(
            "-Xno-call-assertions", 
            "-Xno-param-assertions", 
            "-Xno-receiver-assertions"
        )
    }
}


// O Gradle será forçado a compilar o Kotlin primeiro devido a essa dependência.
tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("compileKotlin"))
    // Garante que copyDependencies execute antes (para ter JARs disponíveis no classpath)
    tasks.findByName("copyDependencies")?.let {
        dependsOn(it)
    }
    val kotlinCompile = tasks.named<KotlinCompile>("compileKotlin").get()
    // Classpath para ver classes Kotlin: prioriza saída do Kotlin (CoroutinePool, LoginServerThread, etc.)
    classpath = files(antClassesDir.get().asFile) + files(kotlinCompile.destinationDirectory) + files(kotlinCompile.outputs.files) + classpath
}


// 4. Configuração do Java/Kotlin
// Kotlin 2.3.0-Beta2+ suporta JVM target 25 nativamente
// Ambos Java e Kotlin compilam para JVM 25 com suporte completo
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// 5. Dependências
dependencies {
    // DEPENDÊNCIAS DO KOTLIN
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.0-Beta2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") 
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0-Beta2")

    // --- Suas Bibliotecas ---
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    // Cap'n Proto
    implementation("org.capnproto:runtime:0.1.16")
    
    // Zstd Compression
    implementation("com.github.luben:zstd-jni:1.5.6-3")

    implementation(kotlin("stdlib"))
    
    // Fast Collections (FastUtil)
    implementation("it.unimi.dsi:fastutil-core:8.5.18")
	
	implementation("io.netty:netty-all:4.1.107.Final")
	
	implementation("com.lmax:disruptor:3.4.4")
	
	
    
    
    
    // Dependências locais da pasta libs (igual ao Ant)
    // O Ant usa ${src-lib} que aponta para "libs"
    // IMPORTANTE: Estes JARs já existem e não são gerados pelo copyDependencies
    // Usamos fileTree para evitar problemas de dependência de tasks
    implementation(fileTree("libs") {
        include("DeepL.jar")
        include("license.jar")
        include("mariadb.jar")
        include("c3p0-0.9.5-pre5.jar")
        include("mchange-commons-java-0.2.6.2.jar")
        
    })
}

// 6. TASK CUSTOMIZADA: Criptografia de Classes 
tasks.register<JavaExec>("encryptCryptaClasses") {
    dependsOn(tasks.classes) 
    group = "security"
    description = "Criptografa as classes do pacote ext.mods.Crypta"
    classpath = sourceSets.getByName("main").runtimeClasspath + files(antClassesDir) 
    mainClass.set("ext.mods.util.ClassEncryptor")
    args("crypta")
}


tasks.register<Jar>("buildSecurityTools") {
    dependsOn(tasks.named("compileKotlin"), tasks.named("compileJava"))
    tasks.findByName("compileTestKotlin")?.let { 
        mustRunAfter(it)
    }
    tasks.findByName("compileTestJava")?.let { 
        mustRunAfter(it)
    }
    group = "security"
    archiveBaseName.set("security-tools")
    destinationDirectory.set(file("libs"))
    // Usa apenas as classes compiladas do sourceSet main (não test)
    from(sourceSets.main.get().output.classesDirs) {
        include("ext/mods/util/SecureKeyManager.class")
        include("ext/mods/util/SecureConfig.class")
        include("ext/mods/util/ClassEncryptor.class")
        include("ext/mods/util/Util.class") 
    }
    manifest {
        attributes("Main-Class" to "ext.mods.util.SecureKeyManager")
    }
}

// 8. Configuração do JAR principal (O 'server.jar')
tasks.jar {
    dependsOn(tasks.named("compileJava"))
    mustRunAfter(tasks.named("syncBinClasses"))
    
    // Usando a referência simples que funciona na maioria dos ambientes
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE 
    
    // Filtra as dependências locais e expande o restante (Fat JAR)
    from(configurations.runtimeClasspath.get().map { 
        val path = it.absolutePath
        if (it.isDirectory) it 
        else if (path.endsWith(".jar") && path.contains("lib")) it 
        else zipTree(it) 
    })
    
    // Inclui os JARs locais da pasta libs (igual ao Ant)
    // O Ant usa ${src-lib} que aponta para "libs"
    from(files("libs/c3p0-0.9.5-pre5.jar").filter { it.exists() })
    from(files("libs/DeepL.jar").filter { it.exists() })
    from(files("libs/license.jar").filter { it.exists() })
    from(files("libs/mariadb.jar").filter { it.exists() })
    
    // Exclui arquivos de metadados redundantes que causam o erro DuplicatesStrategy
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/services/*", "META-INF/versions/**")
    
    manifest {
        attributes(mapOf("Main-Class" to "ext.mods.gameserver.GameServer", "Build-Date" to Instant.now().toString()))
    }
    archiveBaseName.set("server")
    destinationDirectory.set(file("libs")) 
    
    
    from(antClassesDir)
}

// 9. TASK CUSTOMIZADA: Copia as dependências 
// IMPORTANTE: Não sobrescreve JARs locais existentes (DeepL.jar, license.jar, etc)
tasks.register<Copy>("copyDependencies") {
    group = "distribution"
    description = "Copia dependências Maven para a pasta /libs e pasta crypta para build/distribution"
    
    // Copia JARs do Maven para /libs
    from(configurations.runtimeClasspath.get().filter { 
        // Apenas copia JARs do Maven, não os JARs locais já existentes
        val fileName = it.name
        !fileName.contains("DeepL.jar") &&
        !fileName.contains("license.jar") &&
        !fileName.contains("mariadb.jar") &&
        !fileName.contains("c3p0") &&
        !fileName.contains("mchange")
    })
    into(file("libs"))
    
    // Não sobrescreve arquivos existentes
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 9.1.1. TASK: Patch Java sources - compila 9 arquivos Java e injeta no server.jar
tasks.register("patchJava") {
    dependsOn(tasks.named("jar"))
    group = "build"
    description = "Compiles Java patch files and injects them into server.jar"

    doLast {
        val serverJar = file("libs/server.jar")
        val srcDir = file("java")
        val outDir = file("build/patch-java-out")
        outDir.mkdirs()

        val patches = listOf(
            "ext/mods/security/LicenseInit.java",
            "ext/mods/security/LicenseValidator.java",
            "ext/mods/Config.java",
            "ext/mods/commons/config/ExProperties.java",
            "ext/mods/commons/lang/StringReplacer.java",
            "ext/mods/loginserver/data/manager/GameServerManager.java",
            "ext/mods/commons/util/JvmOptimizer.java",
            "ext/mods/commons/logging/formatter/NoTimestampConsoleFormatter.java",
            "ext/mods/gameserver/network/clientpackets/MoveBackwardToLocation.java"
        )

        val sep = File.pathSeparator

        // Compile all patch files against server.jar
        val existingPatches = patches.filter { File(srcDir, it).exists() }
        if (existingPatches.isEmpty()) {
            println("  No Java patch files found, skipping")
            return@doLast
        }

        val classpath = "${serverJar.absolutePath}${sep}${outDir.absolutePath}"
        project.exec {
            commandLine(
                "javac",
                "-d", outDir.absolutePath,
                "-cp", classpath,
                *existingPatches.map { File(srcDir, it).absolutePath }.toTypedArray()
            )
        }
        println("  Compiled ${existingPatches.size} Java patches")

        // Inject patched classes into server.jar (replace originals)
        val tempJar = File("libs/server.jar.patch-java")
        serverJar.copyTo(tempJar, overwrite = true)

        for (patch in existingPatches) {
            val classFile = patch.replace(".java", ".class")
            val classPath = File(outDir, classFile)
            if (classPath.exists()) {
                project.exec {
                    commandLine(
                        "jar", "uf", tempJar.absolutePath,
                        "-C", outDir.absolutePath, classFile
                    )
                }
            }
        }

        // Replace original with patched
        tempJar.copyTo(serverJar, overwrite = true)
        tempJar.delete()
        println("  Java patches injected into server.jar")
    }
}

// 9.1.2. TASK: Patch ASM bytecode - injeta MovementPatch em CreatureMove/PlayerMove
tasks.register<JavaExec>("patchBytecode") {
    dependsOn(tasks.named("patchJava"))
    group = "build"
    description = "Patches Kotlin bytecode in server.jar using ASM (CreatureMove + PlayerMove movement fix)"

    doFirst {
        val asmJar = file("libs/asm-9.8.jar")
        val asmTreeJar = file("libs/asm-tree-9.8.jar")

        // Download ASM 9.8 if not present
        if (!asmJar.exists() || !asmTreeJar.exists()) {
            println("  Downloading ASM 9.8...")
            listOf(
                "https://repo1.maven.org/maven2/org/ow2/asm/asm/9.8/asm-9.8.jar" to asmJar,
                "https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.8/asm-tree-9.8.jar" to asmTreeJar
            ).forEach { (url, dest) ->
                if (!dest.exists()) {
                    java.net.URL(url).openStream().use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    println("    Downloaded: ${dest.name}")
                }
            }
        }

        // Compile PatchBytecode.java against server.jar + ASM
        val patchSrc = file("docker/PatchBytecode.java")
        val patchOut = file("build/patch-out")
        patchOut.mkdirs()

        val sep = File.pathSeparator
        project.exec {
            commandLine(
                "javac",
                "-d", patchOut.absolutePath,
                "-cp", "${file("libs/server.jar").absolutePath}${sep}${asmJar.absolutePath}${sep}${asmTreeJar.absolutePath}",
                patchSrc.absolutePath
            )
        }
        println("  PatchBytecode compiled")
    }

    // Run the patcher
    val asmJar = file("libs/asm-9.8.jar")
    val asmTreeJar = file("libs/asm-tree-9.8.jar")
    val patchOut = file("build/patch-out")

    classpath = files(patchOut, asmJar, asmTreeJar, file("libs/server.jar"))
    mainClass.set("PatchBytecode")
    args = listOf(file("libs/server.jar").absolutePath)

    onlyIf { file("libs/server.jar").exists() }
}

// 9.2. TASK: Sincroniza classes do Gradle para a pasta bin (IDE)
tasks.register<Copy>("syncBinClasses") {
    group = "distribution"
    description = "Copia classes compiladas (build/classes) para bin/"
    dependsOn(tasks.named("compileKotlin"), tasks.named("compileJava"))
    from(antClassesDir)
    into(file("bin/main"))
}

// 9.3. TASK: Copia pasta crypta para build/distribution
tasks.register<Copy>("copyCrypta") {
    group = "distribution"
    description = "Copia pasta crypta para build/distribution"

    from(file("game/data/prevention/crypta")) {
        include("**/*")
        include("**/*.encrypted")
        include("**/*.meta")
        include("**/key.properties")
    }
    into(file("build/distribution/game/data/prevention/crypta"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { file("game/data/prevention/crypta").exists() }
}

// 9.4. TASK: Monta build/distribution/ com todos os arquivos runtime para Docker
tasks.create("distribution") {
    dependsOn(tasks.named("patchBytecode"))
    dependsOn(tasks.named("copyCrypta"))
    group = "distribution"
    description = "Monta build/distribution/ com arquivos runtime para Docker"

    doLast {
        val distDir = file("build/distribution")

        println("=== Montando build/distribution/ ===")

        // Limpar distribuicao anterior
        if (distDir.exists()) {
            distDir.deleteRecursively()
        }
        distDir.mkdirs()

        // 1. libs/ (server.jar patched + todas dependencias)
        project.copy {
            from("libs")
            into("$distDir/libs")
            include("**/*.jar")
        }
        println("  libs/ copiado")

        // 2. game/ (config + data)
        project.copy {
            from("game/config")
            into("$distDir/game/config")
            include("**/*.properties", "**/*.ini", "**/*.txt")
        }
        println("  game/config/ copiado")

        project.copy {
            from("game/data")
            into("$distDir/game/data")
            exclude("cache/**")
            exclude("log/**")
            exclude("prevention/**")
        }
        println("  game/data/ copiado")

        // 3. login/ (excluindo cache e log)
        project.copy {
            from("login")
            into("$distDir/login")
            exclude("cache/**")
            exclude("log/**")
        }
        println("  login/ copiado")

        // 4. tools/sql/ (schemas do GameServer)
        if (file("tools/sql").exists()) {
            project.copy {
                from("tools/sql")
                into("$distDir/tools/sql")
                include("**/*.sql")
            }
            println("  tools/sql/ copiado")
        }

        // 5. sql/ (schema do LoginServer)
        if (file("sql").exists()) {
            project.copy {
                from("sql")
                into("$distDir/sql")
                include("**/*.sql")
            }
            println("  sql/ copiado")
        }

        // 6. Diretorios estaticos
        val staticDirs = listOf("images", "sound", "Hwid", "tools")
        for (dir in staticDirs) {
            if (file(dir).exists()) {
                project.copy {
                    from(dir)
                    into("$distDir/$dir")
                }
                println("  $dir/ copiado")
            }
        }

        // 7. Scripts de entrada
        listOf("entrypoint.sh", "init-db.sh").forEach { script ->
            if (file(script).exists()) {
                project.copy {
                    from(script)
                    into(distDir)
                }
                println("  $script copiado")
            }
        }

        // 8. docker/entrypoint.sh e docker/init-db.sh (usados pelo stack.py compose)
        if (file("docker/entrypoint.sh").exists()) {
            project.copy {
                from("docker/entrypoint.sh")
                into("$distDir/docker")
            }
            println("  docker/entrypoint.sh copiado")
        }
        if (file("docker/init-db.sh").exists()) {
            project.copy {
                from("docker/init-db.sh")
                into("$distDir/docker")
            }
            println("  docker/init-db.sh copiado")
        }

        // 9. Dockerfiles (build context = build/distribution/)
        listOf("Dockerfile").forEach { df ->
            if (file(df).exists()) {
                project.copy {
                    from(df)
                    into(distDir)
                }
                println("  $df copiado")
            }
        }
        if (file("docker/Dockerfile").exists()) {
            project.copy {
                from("docker/Dockerfile")
                into("$distDir/docker")
            }
            println("  docker/Dockerfile copiado")
        }

        // 9. Cache scripts
        if (file("cache").exists()) {
            project.copy {
                from("cache")
                into("$distDir/cache")
                include("*.inc.bat")
            }
            println("  cache/ copiado")
        }

        println("=== build/distribution/ montado com sucesso ===")
        println("  Tamanho: ${distDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1024 / 1024} MB")
    }
}

// 10. Configuração do Build Principal 
tasks.build {
    dependsOn(tasks.named("compileKotlin"))
    dependsOn(tasks.named("compileJava"))
    dependsOn(tasks.named("jar"))
    dependsOn(tasks.named("patchJava"))
    dependsOn(tasks.named("patchBytecode"))
    dependsOn(tasks.named("distribution"))
    dependsOn(tasks.named("syncBinClasses"))
}



// 12. Configuração para garantir que compileKotlin use build/classes após clean
tasks.named("compileKotlin") {
    doFirst {
        val outputDir = antClassesDir.get().asFile
        if (!outputDir.exists()) {
            outputDir.mkdirs()
            logger.lifecycle("Diretório de saída criado: ${outputDir.absolutePath}")
        }
        logger.lifecycle("Compilando Kotlin para: ${outputDir.absolutePath}")
    }
}


