package com.benbernard.machineworklist.bootstrap;

import java.util.Locale;
import java.util.jar.Manifest;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.ExtensibleClassLoader;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

/** Backport of lwjgl3ify PR 251: normalize wheel input after inversion and scaling. */
public final class WheelTransformer implements RfbClassTransformer {

    static final String TARGET = "org.lwjglx.input.Mouse";
    private final boolean enabled;

    public WheelTransformer() {
        this(
            Boolean.parseBoolean(
                System.getProperty(
                    "machineworklist.discreteScrolling",
                    Boolean.toString(
                        System.getProperty("os.name", "")
                            .toLowerCase(Locale.ROOT)
                            .startsWith("mac")))));
    }

    WheelTransformer(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String id() {
        return "discrete-wheel";
    }

    @Override
    public boolean shouldTransformClass(ExtensibleClassLoader loader, Context context, Manifest manifest, String name,
        ClassNodeHandle handle) {
        return enabled && TARGET.equals(name) && handle.isPresent();
    }

    @Override
    public void transformClass(ExtensibleClassLoader loader, Context context, Manifest manifest, String name,
        ClassNodeHandle handle) {
        if (!shouldTransformClass(loader, context, manifest, name, handle)) return;
        if (patch(handle.getNode())) {
            System.out.println("[Machine Worklist] Enabled discrete wheel scrolling (lwjgl3ify backport).");
        } else {
            System.out.println("[Machine Worklist] Wheel backport skipped: existing fix or unsupported mouse code.");
        }
    }

    static boolean patch(ClassNode node) {
        if (node == null || !"org/lwjglx/input/Mouse".equals(node.name)) return false;
        for (MethodNode method : node.methods) {
            if (!"addWheelEvent".equals(method.name) || !"(D)V".equals(method.desc)
                || (method.access & Opcodes.ACC_STATIC) == 0) continue;
            AbstractInsnNode anchor = null;
            int matches = 0;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    // Do not override the native setting in newer lwjgl3ify, or patch twice.
                    if ("java/lang/Math".equals(call.owner) && "signum".equals(call.name) && "(D)D".equals(call.desc))
                        return false;
                }
                if (!(insn instanceof FieldInsnNode)) continue;
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.getOpcode() != Opcodes.GETSTATIC || !"me/eigenraven/lwjgl3ify/core/Config".equals(field.owner)
                    || !"INPUT_SCROLL_SPEED".equals(field.name)
                    || !"D".equals(field.desc)) continue;
                AbstractInsnNode multiply = nextCode(insn);
                AbstractInsnNode store = multiply == null ? null : nextCode(multiply);
                if (multiply != null && multiply.getOpcode() == Opcodes.DMUL
                    && store instanceof VarInsnNode
                    && store.getOpcode() == Opcodes.DSTORE
                    && ((VarInsnNode) store).var == 0) {
                    anchor = store;
                    matches++;
                }
            }
            if (matches != 1) return false;
            InsnList patch = new InsnList();
            patch.add(new VarInsnNode(Opcodes.DLOAD, 0));
            patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "signum", "(D)D", false));
            patch.add(new VarInsnNode(Opcodes.DSTORE, 0));
            method.instructions.insert(anchor, patch);
            // No new branches or locals; the existing method already needs a double on its stack.
            return true;
        }
        return false;
    }

    private static AbstractInsnNode nextCode(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node != null && node.getOpcode() < 0);
        return node;
    }
}
