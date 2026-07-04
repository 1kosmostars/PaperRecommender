package com.recommender.jni;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Загружает нативные библиотеки из classpath (/native/<имя>.dll|.so|.dylib).
 * Если ресурс не найден — пробует System.loadLibrary (нужен java.library.path).
 */
public final class NativeLoader {

    private static final Set<String> loaded = new HashSet<>();

    private NativeLoader() {}

    public static synchronized void load(String name) {
        if (loaded.contains(name)) return;

        String os  = System.getProperty("os.name").toLowerCase();
        String ext    = os.contains("win") ? ".dll"    : os.contains("mac") ? ".dylib" : ".so";
        String prefix = os.contains("win") ? ""        : "lib";
        String filename = prefix + name + ext;
        String resource = "/native/" + filename;

        try (InputStream is = NativeLoader.class.getResourceAsStream(resource)) {
            if (is != null) {
                Path tmp = Files.createTempFile(name + "-", ext);
                tmp.toFile().deleteOnExit();
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                System.load(tmp.toAbsolutePath().toString());
            } else {
                /* fallback: библиотека должна быть в java.library.path */
                System.loadLibrary(name);
            }
            loaded.add(name);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить нативную библиотеку: " + name, e);
        }
    }
}
