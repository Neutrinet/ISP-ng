/*
 * Made by Wannes 'W' De Smet
 * (c) 2011 Wannes De Smet
 * All rights reserved.
 * 
 */
package net.wgr.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.log4j.Logger;

/**
 * 
 * @created Jul 8, 2011
 * @author double-u
 */
public class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static Field[] getAllFields(Class clazz) {
        List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getAllFields(clazz.getSuperclass())));
        }
        return fields.toArray(new Field[]{});
    }

    public static Method[] getAllMethods(Class clazz) {
        ArrayList<Method> methods = new ArrayList<>();
        if (clazz != null) {
            methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        }
        Class parent = clazz.getSuperclass();
        while (parent != null) {
            methods.addAll(Arrays.asList(parent.getDeclaredMethods()));
            parent = parent.getSuperclass();
        }
        return methods.toArray(new Method[0]);
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static List<Class> getClasses(String packageName, Class sourceContext) throws ClassNotFoundException, IOException {
        CodeSource src = sourceContext.getProtectionDomain().getCodeSource();
        List<Class> classes = new ArrayList<>();

        if (src != null) {
            URL jar = src.getLocation();
            File f = null;

            try {
                f = new File(jar.toURI());
            } catch (URISyntaxException ex) {
                Logger.getLogger(ReflectionUtils.class).error("Illegal source path given by CodeSource", ex);
                return classes;
            }

            if (f.isDirectory()) {
                File target = new File(f, packageName.replace('.', '/'));
                classes = findClasses(target, packageName);
            } else {
                ZipInputStream zip = new ZipInputStream(jar.openStream());
                ZipEntry ze = null;
                String path = packageName.replace('.', '/');

                while ((ze = zip.getNextEntry()) != null) {
                    if (ze.getName().startsWith(path) && ze.getName().endsWith(".class")) {
                        // Remove leading slash
                        int start = path.length() + 1;
                        // Remove trailing .class extension
                        int end = ze.getName().length() - 6;
                        if (start > end) {
                            continue;
                        }

                        String className = ze.getName().substring(start, end);
                        if (!className.isEmpty() && !className.contains("$") && !className.contains("/")) {
                            classes.add(Class.forName(packageName + '.' + className));
                        }
                    }
                }
            }
        }

        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory.
     *
     * @source http://snippets.dzone.com/posts/show/4831
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                assert !fileName.contains(".");
                // Do not recurse ATM
                //classes.addAll(findClasses(file, packageName + "." + fileName));
            } else if (fileName.endsWith(".class") && !fileName.contains("$")) {
                Class clazz;
                try {
                    clazz = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6));
                } catch (ExceptionInInitializerError e) {
                    // happen, for example, in classes, which depend on 
                    // Spring to inject some beans, and which fail, 
                    // if dependency is not fulfilled
                    clazz = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6),
                            false, Thread.currentThread().getContextClassLoader());
                }
                classes.add(clazz);
            }
        }
        return classes;
    }

    /**
     * Returns difference between instances
     * @param <T> instance type
     * @param original 
     * @param changed 
     * @return <fieldName, value>
     */
    public static <T> Map<String, Object> diff(T original, T changed) {
        if (original == null && changed == null) {
            throw new IllegalArgumentException("Original and changed object were null");
        }

        boolean addAll = false;
        if (original == null) {
            addAll = true;
            original = changed;
        }

        if (changed == null) {
            addAll = true;
            changed = original;
        }

        HashMap<String, Object> result = new HashMap<>();
        for (Field f : getAllFields(original.getClass())) {

            if (Modifier.isTransient(f.getModifiers())) {
                continue;
            }

            f.setAccessible(true);
            try {
                Object origVal = f.get(original);
                Object newVal = f.get(changed);

                if (addAll || (origVal == null && newVal != null) || (origVal != null && !origVal.equals(newVal))) {
                    result.put(f.getName(), newVal);
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ReflectionUtils.class).error("Failed to reflect properly", ex);
            }
        }
        return result;
    }
}
