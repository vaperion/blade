package me.vaperion.blade.utils;

import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassUtils {

    public static List<Class<?>> getClasses(Plugin plugin, Class<? extends Annotation> annotation) {
        Reflections reflections = new Reflections(plugin.getClass().getPackage().getName(), new MethodAnnotationsScanner());
        Set<Method> classSet = reflections.getMethodsAnnotatedWith(annotation);

        return classSet.stream()
                .map(Method::getDeclaringClass)
                .collect(Collectors.toList());
    }
}
