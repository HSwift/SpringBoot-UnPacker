import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.streams.toList
import org.benf.cfr.reader.Main as CFR
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler as FernFlower

enum class Decompilers {
    FernFlower,
    CFR,
}

fun fernFlowerDecompile(args: MutableList<String>, sourceDir: String) {
    args.add(sourceDir)
    println("[INFO] decompile class file with fernFlower")
    FernFlower.main(args.toTypedArray())
}

fun cfrDecompile(args: MutableList<String>, sourceDir: String) {
    args.add("--outputpath")
    args.add(sourceDir)
    println("[INFO] decompile class file with cfr")
    CFR.main(args.toTypedArray())
}

fun decompile(decompiler: Decompilers, target: File, excludeClasses: Regex?) {
    val classesDir = target.resolve("classes")
    val sourceDir = target.resolve("src/main/java/")
    sourceDir.mkdirs()
    var classes = Files.walk(classesDir.toPath()).map { it.absolutePathString() }.filter { it.endsWith(".class") }
        .toList<String>()

    if (excludeClasses != null) {
        classes = classes.filter {
            !excludeClasses.matches(it)
        }
    }
    if (decompiler == Decompilers.FernFlower) {
        fernFlowerDecompile(classes.toMutableList(), sourceDir.path)
    } else if (decompiler == Decompilers.CFR) {
        cfrDecompile(classes.toMutableList(), sourceDir.path)
    }

}