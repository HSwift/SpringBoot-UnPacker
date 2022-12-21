import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.streams.toList
import org.benf.cfr.reader.Main as CFR
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler as FernFlower

enum class Decompilers {
    FernFlower,
    CFR,
}

fun fernFlowerDecompile(path: String) {
    println("[INFO] decompile class file with fernFlower")
    FernFlower.main(arrayOf(path, path))
}

fun cfrDecompile(path: String) {
    val all = Files.walk(Paths.get(path)).map { it.pathString }.filter { it.endsWith(".class") }.toList<String>()
        .toMutableList()
    all.add("--outputpath")
    all.add(path)
    println("[INFO] decompile class file with cfr")
    CFR.main(all.toTypedArray())
}

fun decompile(decompiler: Decompilers, path: String) {
    if (decompiler == Decompilers.FernFlower) {
        fernFlowerDecompile(path)
    } else if (decompiler == Decompilers.CFR) {
        cfrDecompile(path)
    }
}