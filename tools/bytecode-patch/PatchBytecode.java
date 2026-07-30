import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patches CreatureMove.handleNextPosition() and PlayerMove.updatePosition()
 * in server.jar to replace canMoveToTarget() calls with MovementPatch.canMoveToTarget().
 * 
 * MovementPatch checks Config.SISTEMA_PATHFINDING first - if false, returns true
 * (movement allowed) without calling GeoEngine.
 * 
 * The trick: MovementPatch.canMoveToTarget(Object, int*6) absorbs the GeoEngine
 * receiver object from the original INVOKEVIRTUAL, keeping the stack balanced.
 */
public class PatchBytecode {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: PatchBytecode <server.jar>");
            System.exit(1);
        }

        String jarPath = args[0];
        File jarFile = new File(jarPath);
        File tempJar = new File(jarPath + ".tmp");

        System.out.println("PatchBytecode: Patching " + jarPath);

        try (JarFile jar = new JarFile(jarFile);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar))) {

            java.util.Enumeration<JarEntry> entries = jar.entries();
            boolean patchedCM = false;
            boolean patchedPM = false;
            boolean addedPatch = false;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                InputStream is = jar.getInputStream(entry);
                String name = entry.getName();
                byte[] bytes = is.readAllBytes();
                is.close();

                if (name.equals("ext/mods/gameserver/model/actor/move/CreatureMove.class")) {
                    System.out.println("Patching CreatureMove.handleNextPosition()...");
                    bytes = patchMethod(bytes, "handleNextPosition");
                    patchedCM = true;
                    System.out.println("  -> CreatureMove patched successfully");
                } else if (name.equals("ext/mods/gameserver/model/actor/move/PlayerMove.class")) {
                    System.out.println("Patching PlayerMove.updatePosition()...");
                    bytes = patchMethod(bytes, "updatePosition");
                    patchedPM = true;
                    System.out.println("  -> PlayerMove patched successfully");
                }

                jos.putNextEntry(new JarEntry(name));
                jos.write(bytes);
                jos.closeEntry();

                if (!addedPatch) {
                    addedPatch = true;
                    byte[] patchClass = buildMovementPatch();
                    jos.putNextEntry(new JarEntry("MovementPatch.class"));
                    jos.write(patchClass);
                    jos.closeEntry();
                    System.out.println("Added MovementPatch.class to jar");
                }
            }

            if (!patchedCM) System.out.println("WARNING: CreatureMove.class not found in jar!");
            if (!patchedPM) System.out.println("WARNING: PlayerMove.class not found in jar!");
        }

        if (!jarFile.delete()) {
            System.err.println("Failed to delete original jar");
            System.exit(1);
        }
        if (!tempJar.renameTo(jarFile)) {
            System.err.println("Failed to rename patched jar");
            System.exit(1);
        }

        System.out.println("Done! server.jar patched successfully.");
    }

    static byte[] patchMethod(byte[] classBytes, String methodName) {
        ClassNode cn = new ClassNode();
        ClassReader cr = new ClassReader(classBytes);
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName)) {
                System.out.println("    Found method: " + mn.name + mn.desc);
                patchMethodNode(mn);
                break;
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };
        cn.accept(cw);
        return cw.toByteArray();
    }

    static void patchMethodNode(MethodNode mn) {
        if (mn.instructions == null) return;

        int patched = 0;
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if (min.name.equals("canMoveToTarget") && min.desc.equals("(IIIIII)Z")) {
                    System.out.println("    Replacing " + min.owner + ".canMoveToTarget(IIIIII)Z -> MovementPatch.canMoveToTarget(Object,IIIIII)Z");
                    // Replace INVOKEVIRTUAL GeoEngine.canMoveToTarget(IIIIII)Z
                    // with    INVOKESTATIC  MovementPatch.canMoveToTarget(Object,IIIIII)Z
                    //
                    // Stack effect is identical:
                    //   INVOKEVIRTUAL consumes [objectref, 6 ints] = 7 stack slots
                    //   INVOKESTATIC  consumes [Object, 6 ints]    = 7 stack slots (Object is the old objectref)
                    MethodInsnNode replacement = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "MovementPatch",
                        "canMoveToTarget",
                        "(Ljava/lang/Object;IIIIII)Z",
                        false
                    );
                    mn.instructions.set(min, replacement);
                    patched++;
                }
            }
            insn = insn.getNext();
        }
        System.out.println("    Patched " + patched + " canMoveToTarget call(s)");
    }

    static byte[] buildMovementPatch() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };

        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "MovementPatch", null, "java/lang/Object", null);
        cw.visitSource("MovementPatch.java", null);

        // Default constructor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // canMoveToTarget(Object, int, int, int, int, int, int) -> boolean
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "canMoveToTarget",
                "(Ljava/lang/Object;IIIIII)Z", null, null);
        mv.visitCode();

        // if (!Config.SISTEMA_PATHFINDING) return true;
        mv.visitFieldInsn(Opcodes.GETSTATIC, "ext/mods/Config", "SISTEMA_PATHFINDING", "Z");
        Label skipLabel = new Label();
        mv.visitJumpInsn(Opcodes.IFNE, skipLabel);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);

        // return ((GeoEngine) geoEngine).canMoveToTarget(x1, y1, z1, x2, y2, z2);
        mv.visitLabel(skipLabel);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "ext/mods/gameserver/geoengine/GeoEngine");
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitVarInsn(Opcodes.ILOAD, 6);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "ext/mods/gameserver/geoengine/GeoEngine",
                "canMoveToTarget", "(IIIIII)Z", false);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitMaxs(8, 7);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
