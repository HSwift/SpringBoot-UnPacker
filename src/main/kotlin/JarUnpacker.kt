import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class JarUnpack(filename: File, private val target: File) {
    private val jarFile: ZipFile
    private var isPOMCreated: Boolean = false

    init {
        jarFile = ZipFile(filename)
    }

    private fun mavenHandler(entry: ZipEntry) {
        if (entry.name.endsWith("pom.xml")) {
            if (isPOMCreated) {
                println("[WARN] pom.xml is written multiple times")
            }
            isPOMCreated = true
            jarFile.getInputStream(entry).use { input ->
                target.resolve("pom.xml").outputStream().use { output ->
                    println("[INFO] create pom.xml")
                    input.copyTo(output)
                }
            }
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
        val classesDir = target.resolve("classes")
        val filename = entry.name.removePrefix(prefix)
        val packagePath = ".+?/(?=[^/]+\$)".toRegex().find(filename)?.value
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
        entries.asSequence().forEach {
            val fileName = it.name
            when {
                fileName.startsWith("META-INF/maven/") -> {
                    mavenHandler(it)
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
    }
}