import java.io.File
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.streams.toList
import org.benf.cfr.reader.Main as CFR
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler as FernFlower

enum class Decompilers {
    FernFlower,
    CFR,
}

fun fernFlowerDecompile(classDir: File) {
    println("[INFO] decompile class file with fernFlower")
    FernFlower.main(arrayOf(classDir.absolutePath, classDir.absolutePath))
}

fun cfrDecompile(classDir: File) {
    println("[INFO] decompile class file with cfr")
    val all = Files.walk(classDir.toPath()).map { it.pathString }.filter { it.endsWith(".class") }.toList<String>()
        .toMutableList()
    all.add("--outputpath")
    all.add(classDir.absolutePath)
    println("[INFO] decompile class file with cfr")
    CFR.main(all.toTypedArray())
}

fun decompile(decompiler: Decompilers, target: File) {
    if (decompiler == Decompilers.FernFlower) {
        fernFlowerDecompile(target)
    } else if (decompiler == Decompilers.CFR) {
        cfrDecompile(target)
    }

}