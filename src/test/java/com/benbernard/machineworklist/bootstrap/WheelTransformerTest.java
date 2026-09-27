package com.benbernard.machineworklist.bootstrap;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

public class WheelTransformerTest {

    private static InputStream fixture(String name) throws Exception {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(System.getProperty("worklist.wheelFixture"));
            InputStream input = jar.getInputStream(jar.getJarEntry(name))) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new java.io.ByteArrayInputStream(output.toByteArray());
        }
    }

    private static ClassNode mouse() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = fixture("org/lwjglx/input/Mouse.class")) {
            assertNotNull("Pinned lwjgl3ify 2.1.16 must be on the test classpath", input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void targetsExcludedMouseClassAndRespectsOptOut() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        mouse().accept(writer);
        ClassNodeHandle handle = new ClassNodeHandle(writer.toByteArray());
        for (RfbClassTransformer.Context context : RfbClassTransformer.Context.values()) {
            assertTrue(
                new WheelTransformer(true).shouldTransformClass(null, context, null, WheelTransformer.TARGET, handle));
            assertFalse(
                new WheelTransformer(false).shouldTransformClass(null, context, null, WheelTransformer.TARGET, handle));
            assertFalse(
                new WheelTransformer(true)
                    .shouldTransformClass(null, context, null, "org.lwjglx.input.Keyboard", handle));
        }
    }

    @Test
    public void patchIsIdempotentAndRejectsUnknownLayout() throws Exception {
        ClassNode node = mouse();
        assertTrue(WheelTransformer.patch(node));
        assertFalse(WheelTransformer.patch(node));
        node = mouse();
        node.methods.removeIf(method -> "addWheelEvent".equals(method.name));
        assertFalse(WheelTransformer.patch(node));
    }

    @Test
    public void realMouseMethodProducesOneStepPerEvent() throws Exception {
        Harness fixed = new Harness(true);
        for (double delta : new double[] { 0.01, 0.1, 0.4, 1, 3, -0.01, -0.4, -3, 0.1, -0.1 }) {
            assertEquals((int) Math.signum(delta), fixed.scroll(delta));
        }
        assertEquals(0, fixed.scroll(0));
        fixed.config.getField("INPUT_INVERT_WHEEL")
            .setBoolean(null, true);
        fixed.config.getField("INPUT_SCROLL_SPEED")
            .setDouble(null, 5);
        assertEquals(-1, fixed.scroll(0.01));
        assertEquals(1, fixed.scroll(-3));
    }

    @Test
    public void fixtureReproducesOriginalMissedNotch() throws Exception {
        Harness original = new Harness(false);
        assertEquals(0, original.scroll(0.1));
        assertEquals(0, original.scroll(0.1));
        assertEquals(1, original.scroll(0.9));
    }

    /** Execute the shipped wheel method and EventQueue without opening a native window. */
    private static final class Harness extends ClassLoader {

        final Class<?> config;
        final Class<?> mouse;
        final Method addWheel;
        final Field wheel;

        Harness(boolean patch) throws Exception {
            super(WheelTransformerTest.class.getClassLoader());
            ClassNode configNode = new ClassNode();
            configNode.version = Opcodes.V1_8;
            configNode.access = Opcodes.ACC_PUBLIC;
            configNode.name = "me/eigenraven/lwjgl3ify/core/Config";
            configNode.superName = "java/lang/Object";
            configNode.fields
                .add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "INPUT_INVERT_WHEEL", "Z", null, null));
            configNode.fields
                .add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "INPUT_SCROLL_SPEED", "D", null, null));
            config = define(configNode);
            config.getField("INPUT_SCROLL_SPEED")
                .setDouble(null, 1);
            ClassNode queueNode = new ClassNode();
            try (InputStream input = fixture("org/lwjglx/input/EventQueue.class")) {
                assertNotNull(input);
                new ClassReader(input).accept(queueNode, 0);
            }
            queueNode.version = Opcodes.V1_8;
            Class<?> queue = define(queueNode);
            ClassNode node = mouse();
            if (patch) assertTrue(WheelTransformer.patch(node));
            // Retain the real wheel method; omit window initialization and unrelated native methods.
            node.version = Opcodes.V1_8;
            node.methods.removeIf(method -> !"addWheelEvent".equals(method.name));
            node.fields
                .removeIf(field -> field.desc.startsWith("L") && !"Lorg/lwjglx/input/EventQueue;".equals(field.desc));
            for (FieldNode field : node.fields) field.access &= ~Opcodes.ACC_FINAL;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if ("org/lwjglx/Sys".equals(call.owner) && "getNanoTime".equals(call.name)) {
                            call.owner = "java/lang/System";
                            call.name = "nanoTime";
                        }
                    }
                }
            }
            mouse = define(node);
            for (Field field : mouse.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType()
                    .isArray()) {
                    field.set(
                        null,
                        Array.newInstance(
                            field.getType()
                                .getComponentType(),
                            64));
                } else if (field.getType() == queue) {
                    java.lang.reflect.Constructor<?> constructor = queue.getDeclaredConstructor(int.class);
                    constructor.setAccessible(true);
                    field.set(null, constructor.newInstance(64));
                }
            }
            addWheel = mouse.getMethod("addWheelEvent", double.class);
            wheel = mouse.getDeclaredField("dwheel");
            wheel.setAccessible(true);
        }

        private Class<?> define(ClassNode node) {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            byte[] bytes = writer.toByteArray();
            return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length);
        }

        int scroll(double delta) throws Exception {
            int before = wheel.getInt(null);
            addWheel.invoke(null, delta);
            return wheel.getInt(null) - before;
        }
    }
}
