package org.jboss.wildscribe.modeldumper;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Internal use only.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoader extends URLClassLoader {

    private final ClassLoader parent;
    private final Pattern childFirst;

    ChildFirstClassLoader(ClassLoader parent) throws IOException {
        this(parent, Pattern.compile("org.jboss.wildscribe.modeldumper.Main*"));
    }

    ChildFirstClassLoader(ClassLoader parent, Pattern childFirst) throws IOException {
        super(getParentURLs(parent), parent);
        assert childFirst != null : "Null child first";
        this.parent = parent;
        this.childFirst = childFirst;
        registerAsParallelCapable();
    }

    private static URL[] getParentURLs(ClassLoader parent) throws IOException {
        Enumeration<URL> urls = parent.getResources("");
        List<URL> urlList = new ArrayList<>();
        while (urls.hasMoreElements()) {
            urlList.add(urls.nextElement());
        }
        return urlList.toArray(new URL[0]);
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                if (childFirst.matcher(name).matches()) {
                    throw e;
                }
            }
            if (c == null) {
                c = parent.loadClass(name);
            }
            if (c == null) {
                findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (childFirst.matcher(name).matches()) {
            return super.findClass(name);
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    @Override
    public URL findResource(String name) {
        assert name != null;
        String toPath = name.replace('.', ',');
        return childFirst.matcher(toPath).matches() ? super.findResource(name) : null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        assert name != null;
        String toPath = name.replace('.', ',');
        return childFirst.matcher(toPath).matches() ? super.findResources(name) : new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public URL nextElement() {
                return null;
            }
        };
    }
}





