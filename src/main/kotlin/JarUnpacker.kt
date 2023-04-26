import java.io.File
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class JarUnpack(filename: File, private val target: File) {
    private val jarFile: ZipFile
    private var excludeClassesRegex: Regex? = null
    private var includeClassesRegex: Regex? = null
    private var artifactId = ""
    private var groupId = ""
    private var springBootVersion = ""
    private var javaVersion = ""
    private var startClass = ""
    private var implementationTitle = ""

    init {
        jarFile = ZipFile(filename)
        excludeClassesRegex =
            excludeClasses?.split(":")
                ?.map { "^" + Regex.escape(it.replace(".", "/")) + ".*" }
                ?.reduce { acc, s -> "$acc|$s" }
                ?.toRegex()
        includeClassesRegex =
            includeClasses?.split(":")
                ?.map { "^" + Regex.escape(it.replace(".", "/")) + ".*" }
                ?.reduce { acc, s -> "$acc|$s" }
                ?.toRegex()
    }

    private fun copyPomFile(entryName: String) {
        val entry = jarFile.getEntry(entryName)
        jarFile.getInputStream(entry).use { input ->
            target.resolve("pom.xml").outputStream().use { output ->
                println("[INFO] copy $entryName to pom.xml")
                input.copyTo(output)
            }
        }
    }

    private fun createDefaultPomFile() {
        println("[INFO] use default pom.xml")
        val defaultPom = JarUnpack::class.java.getResource("pom.template.xml")!!.readText()
        val pomContent = defaultPom
            .replace("\${groupId}", groupId)
            .replace("\${artifactId}", artifactId)
            .replace("\${javaVersion}", javaVersion)
            .replace("\${springBootVersion}", springBootVersion)
        target.resolve("pom.xml").writeText(pomContent)
    }

    private fun pomHandler(pomFileList: List<String>) {
        val packageNames =
            pomFileList.map { it.removePrefix("META-INF/maven/").removeSuffix("/pom.xml").replace("/", ".") }
        var targetPackage = packageNames.indexOfFirst { startClass.startsWith("$it.") }
        if (targetPackage != -1) {
            copyPomFile(pomFileList[targetPackage])
            return
        }
        if (implementationTitle != "") {
            targetPackage =
                packageNames.indexOfFirst { "${startClass.substringBeforeLast(".")}.$implementationTitle" == it }
            if (targetPackage != -1) {
                copyPomFile(pomFileList[targetPackage])
                return
            }
        }
        val groups =
            pomFileList.map { it.removePrefix("META-INF/maven/").removeSuffix("/pom.xml").substringBefore("/") }
        if (groups.filter { startClass.startsWith("$it.") }.size == 1) {
            targetPackage = groups.indexOfFirst { startClass.startsWith(it) }
            if (targetPackage != -1) {
                copyPomFile(pomFileList[targetPackage])
                return
            }
        }
        val packageName = startClass.substringBeforeLast(".")
        if (implementationTitle != "") {
            artifactId = implementationTitle
            groupId = packageName.removeSuffix(implementationTitle)
        } else {
            if (packageName.indexOf(".") == -1) {
                artifactId = "demo"
                groupId = packageName
            } else {
                artifactId = packageName.substringBeforeLast(".")
                groupId = packageName
            }
        }
        createDefaultPomFile()
    }

    private fun manifestHandler(entry: ZipEntry) {
        val manifest = Manifest(jarFile.getInputStream(entry))
        startClass = manifest.mainAttributes.getValue("Start-Class")
        springBootVersion = manifest.mainAttributes.getValue("Spring-Boot-Version")
        try {
            javaVersion = manifest.mainAttributes.getValue("Build-Jdk-Spec")
        } catch (e: java.lang.NullPointerException) {
            javaVersion = "1.8"
        }
        try {
            implementationTitle = manifest.mainAttributes.getValue("Implementation-Title")
        } catch (e: java.lang.NullPointerException) {
            implementationTitle = ""
        }

    }

    private fun libsHandler(entry: ZipEntry) {
        val libDir = target.resolve("lib")
        if (!entry.isDirectory) {
            val filename = entry.name.split("/").last()
            jarFile.getInputStream(entry).use { input ->
                libDir.resolve(filename).outputStream().use { output ->
                    println("[INFO] create $filename")
                    input.copyTo(output)
                }
            }
        }
    }

    private fun classesHandler(entry: ZipEntry, prefix: String) {
        var classesDir = target.resolve("src/main/java/")
        val filename = entry.name.removePrefix(prefix)
        val packagePath = ".+?/(?=[^/]+\$)".toRegex().find(filename)?.value
        if (includeClassesRegex?.matches(filename) == false) {
            classesDir = target.resolve("classes")
        }
        if (excludeClassesRegex?.matches(filename) == true) {
            classesDir = target.resolve("classes")
        }
        if (packagePath != null && !classesDir.resolve(packagePath).exists()) {
            classesDir.resolve(packagePath).mkdirs()
        }
        jarFile.getInputStream(entry).use { input ->
            classesDir.resolve(filename).outputStream().use { output ->
                println("[INFO] create class $filename")
                input.copyTo(output)
            }
        }
    }

    private fun resourcesHandler(entry: ZipEntry, prefix: String) {
        val resourcesDir = target.resolve("src/main/resources/")
        if (!entry.isDirectory) {
            val filename = entry.name.removePrefix(prefix)
            val packagePath = ".+?/(?=[^/]+\$)".toRegex().find(filename)?.value
            if (packagePath != null && !resourcesDir.resolve(packagePath).exists()) {
                resourcesDir.resolve(packagePath).mkdirs()
            }
            jarFile.getInputStream(entry).use { input ->
                resourcesDir.resolve(filename).outputStream().use { output ->
                    println("[INFO] create resource $filename")
                    input.copyTo(output)
                }
            }
        }
    }

    fun unpack() {
        target.resolve("lib/").mkdirs()
        target.resolve("src/main/java/").mkdirs()
        target.resolve("src/main/resources/").mkdirs()

        val entries = jarFile.entries()
        val pomFiles = mutableListOf<String>()
        entries.asSequence().forEach {
            val fileName = it.name
            when {
                fileName.startsWith("META-INF/maven/") && fileName.endsWith("pom.xml") -> {
                    pomFiles.add(fileName)
                }

                fileName == "META-INF/MANIFEST.MF" -> {
                    manifestHandler(it)
                }

                fileName.startsWith("BOOT-INF/lib/") -> {
                    libsHandler(it)
                }

                fileName.startsWith("BOOT-INF/classes/") && fileName.endsWith(".class") -> {
                    classesHandler(it, "BOOT-INF/classes/")
                }

                fileName.startsWith("BOOT-INF/classes/") && !fileName.endsWith(".class") -> {
                    resourcesHandler(it, "BOOT-INF/classes/")
                }

                fileName.startsWith("WEB-INF/lib/") -> {
                    libsHandler(it)
                }

                fileName.startsWith("WEB-INF/lib-provided/") -> {
                    libsHandler(it)
                }

                fileName.startsWith("WEB-INF/classes/") && fileName.endsWith(".class") -> {
                    classesHandler(it, "WEB-INF/classes/")
                }

                fileName.startsWith("WEB-INF/classes/") && !fileName.endsWith(".class") -> {
                    resourcesHandler(it, "WEB-INF/classes/")
                }
            }
        }
        pomHandler(pomFiles)
        jarFile.close()
    }
}