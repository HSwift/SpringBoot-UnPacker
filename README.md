# SpringBoot-UnPacker

This is a simple tool for recovering SpringBoot packaged JAR files, its main purpose is to recover the given JAR files into SpringBoot projects that can be modified, compiled and debugged in CTF competitions. The features of this tool include rebuilding the source code structure, extracting lib files and decompiling class files.

This tool does not verify that the JAR is packaged by SpringBoot and if the input JAR file does not meet the requirements, it may cause an exception

## Usage

```
SpringBoot UnPacker options_list
Arguments:
jarPath -> SpringBoot jar file path { String }
Options:
--overwrite, -o [false] -> Overwrite the project dir, if it already exists
--decompiler, -d [FernFlower] -> Class files decompiler { Value should be one of [fernflower, cfr] }
--help, -h -> Usage info
```

## Example

```
java -jar springboot-unpacker-1.0-SNAPSHOT.jar /path/to/jar
```

## Thanks

https://github.com/QuiltMC/quiltflower  
https://github.com/leibnitz27/cfr