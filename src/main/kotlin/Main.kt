import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

fun main(args: Array<String>) {
    val parser = ArgParser("SpringBoot-UnPacker")
    val jarPath by parser.argument(ArgType.String, description = "SpringBoot jar file path")
    val overwrite by parser.option(
        ArgType.Boolean,
        shortName = "o",
        description = "Overwrite the project dir, if it already exists"
    ).default(false)
    val decompiler by parser.option(
        ArgType.Choice<Decompilers>(),
        shortName = "d",
        description = "Select the class files decompiler"
    ).default(Decompilers.FernFlower)
    val removeClassesFile by parser.option(
        ArgType.Boolean,
        shortName = "r",
        description = "Remove class files after decompiling"
    ).default(false)
    val excludeClasses by parser.option(
        ArgType.String,
        shortName = "e",
        description = "Package prefixes to be excluded during decompiling, multiple inputs separated by ','",
    )
    parser.parse(args)

    val jarFile = File(jarPath)
    if (!(jarFile.isFile && jarFile.canRead())) {
        println("[ERR] file $jarFile is not a readable file")
        return
    }
    val projectName = jarFile.name.split("\\.(?=[^.]+$)".toRegex())[0]
    val projectDir = jarFile.parentFile.resolve(projectName)
    if (projectDir.exists()) {
        if (overwrite) {
            projectDir.deleteRecursively()
        } else {
            println("[ERR] project dir $projectDir already exists")
            return
        }
    }
    projectDir.mkdir()

    val jarUnpack = JarUnpack(jarFile, projectDir)
    jarUnpack.unpack()

    val classesDir = projectDir.resolve("classes")
    val excludeClassesRegex =
        excludeClasses?.split(":")
            ?.map { "^" + Regex.escape("${classesDir.absolutePath}/${it.replace(".", "/")}") + ".*" }
            ?.reduce { acc, s -> "$acc|$s" }
            ?.toRegex()
    decompile(decompiler, projectDir, excludeClassesRegex)
    if (removeClassesFile) {
        if (excludeClassesRegex == null) {
            classesDir.deleteRecursively()
            return
        }
        Files.walkFileTree(classesDir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!excludeClassesRegex.matches(file.absolutePathString())) {
                    file.deleteIfExists()
                }
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (dir.listDirectoryEntries().isEmpty()) {
                    dir.deleteIfExists()
                }
                return FileVisitResult.CONTINUE
            }
        })
    }
}