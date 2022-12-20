import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

fun main(args: Array<String>) {
    val parser = ArgParser("SpringBoot UnPacker")
    val jarPath by parser.argument(ArgType.String, description = "SpringBoot jar file path")
    val overwrite by parser.option(
        ArgType.Boolean,
        shortName = "o",
        description = "Overwrite the project dir, if it already exists"
    ).default(false)
    val decompiler by parser.option(
        ArgType.Choice<Decompilers>(),
        shortName = "d",
        description = "Class files decompiler"
    ).default(Decompilers.FernFlower)
    parser.parse(args)

    val jarFile = File(jarPath)
    if (!(jarFile.isFile && jarFile.canRead())) {
        println("file $jarFile is not a readable file")
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
    val classDir = projectDir.resolve("src/main/java/")
    jarUnpack.unpack()
    decompile(decompiler, classDir.absolutePath)
}
