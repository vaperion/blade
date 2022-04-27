package me.vaperion.blade.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@UtilityClass
public class ClassUtil {

    public boolean classExists(@NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    public List<Class<?>> getClassesInPackage(@NotNull Class<?> clazz, @NotNull String packageName) {
        List<Class<?>> classes = new ArrayList<>();

        String packagePath = packageName.replace(".", "/");

        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        URL resource = codeSource.getLocation();
        String path = resource.getPath().replace("%20", " ");

        String jarPath = path.contains("!") ? path.substring(0, path.lastIndexOf("!")) : path;
        if (jarPath.startsWith("file:")) jarPath = jarPath.substring(5);

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith(packagePath) && name.endsWith(".class")) {
                    String className = name.substring(0, name.lastIndexOf(".")).replace("/", ".");

                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException ignored) {}
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get classes in package " + packageName, e);
        }

        return classes;
    }

}