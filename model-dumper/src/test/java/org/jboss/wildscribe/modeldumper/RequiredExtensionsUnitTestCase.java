package org.jboss.wildscribe.modeldumper;

import static org.jboss.wildscribe.modeldumper.Main.REQUIRED_EXTENSIONS_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

public class RequiredExtensionsUnitTestCase {

    private static final String ONE = "one";
    private static final String TWO = "two";

    @Test
    public void testDefault() {
        List<String> required = getRequiredExtensions();
        assertNotNull(required);
        assertEquals(required.toString(), 0, required.size());
    }

    @Test
    public void testEmpty() {
        System.setProperty(REQUIRED_EXTENSIONS_PROPERTY, "");
        try {
            List<String> required = getRequiredExtensions();
            assertNotNull(required);
            assertEquals(required.toString(), 0, required.size());
        } finally {
            System.clearProperty(REQUIRED_EXTENSIONS_PROPERTY);
        }
    }

    @Test
    public void testOne() {
        System.setProperty(REQUIRED_EXTENSIONS_PROPERTY, ONE);
        try {
            List<String> required = getRequiredExtensions();
            assertNotNull(required);
            assertEquals(required.toString(), 1, required.size());
            assertTrue(required.toString(), required.contains(ONE));
        } finally {
            System.clearProperty(REQUIRED_EXTENSIONS_PROPERTY);
        }
    }

    @Test
    public void testTwo() {
        System.setProperty(REQUIRED_EXTENSIONS_PROPERTY, ONE + "," + TWO);
        try {
            List<String> required = getRequiredExtensions();
            assertNotNull(required);
            assertEquals(required.toString(), 2, required.size());
            assertTrue(required.toString(), required.contains(ONE));
            assertTrue(required.toString(), required.contains(TWO));
        } finally {
            System.clearProperty(REQUIRED_EXTENSIONS_PROPERTY);
        }
    }

    @Test
    public void testTrim() {
        System.setProperty(REQUIRED_EXTENSIONS_PROPERTY, ONE + " , " + TWO);
        try {
            List<String> required = getRequiredExtensions();
            assertNotNull(required);
            assertEquals(required.toString(), 2, required.size());
            assertTrue(required.toString(), required.contains(ONE));
            assertTrue(required.toString(), required.contains(TWO));
        } finally {
            System.clearProperty(REQUIRED_EXTENSIONS_PROPERTY);
        }
    }

    private static List<String> getRequiredExtensions() {

        try {
            ChildFirstClassLoader cfcl = new ChildFirstClassLoader(RequiredExtensionsUnitTestCase.class.getClassLoader());
            Class<?> clazz = cfcl.loadClass("org.jboss.wildscribe.modeldumper.Main", true);
            assertEquals(cfcl, clazz.getClassLoader());
            Field field = clazz.getDeclaredField("REQUIRED_EXTENSIONS");
            field.setAccessible(true);
            //noinspection unchecked
            return (List<String>) field.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new AssertionError(e.toString());
        }
    }
}
