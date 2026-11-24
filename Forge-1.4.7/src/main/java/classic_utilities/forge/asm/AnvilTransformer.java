package classic_utilities.forge.asm;

import cpw.mods.fml.relauncher.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnvilTransformer implements IClassTransformer {
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private static final String HOOK_OWNER = "classic_utilities/forge/options/AnvilLimit";
    private static final String HOOK_SERVER = "serverLimit";
    private static final String HOOK_CLIENT = "clientLimit";
    private static final String HOOK_DESC = "(I)I";

    private static final int VANILLA_LIMIT = 40;

    private static final String CONTAINER_PACKAGED = "net/minecraft/inventory/ContainerRepair";
    private static final String CONTAINER_FLAT = "net/minecraft/src/ContainerRepair";
    private static final String GUI_PACKAGED = "net/minecraft/client/gui/GuiRepair";
    private static final String GUI_FLAT = "net/minecraft/src/GuiRepair";

    public byte[] transform(String name, byte[] bytes) {
        if(bytes == null) return null;

        // Check internal name post deobfuscation by FML
        String internalName;
        try {
            internalName = new ClassReader(bytes).getClassName();
        } catch(Throwable t) {
            return bytes;
        }

        boolean container = CONTAINER_PACKAGED.equals(internalName) || CONTAINER_FLAT.equals(internalName);
        boolean gui = GUI_PACKAGED.equals(internalName) || GUI_FLAT.equals(internalName);
        if(!container && !gui) return bytes;

        return tryPatch(bytes, container);
    }

    // Crashing the game would be a bit rude
    private static byte[] tryPatch(byte[] bytes, boolean container) {
        try {
            return patch(bytes, container);
        } catch(Throwable t) {
            LOG.log(Level.SEVERE, "[CUtil] failed to patch anvil, the too expensive option will not work", t);
            return bytes;
        }
    }

    private static byte[] patch(byte[] bytes, boolean container) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);

        // The container owns the maximumCost field; the gui reads it from the container
        String hook = container ? HOOK_SERVER : HOOK_CLIENT;

        int sites = 0;
        for(Object o : cn.methods) {
            sites += patchMethod(cn, (MethodNode) o, container, hook);
        }
        if(sites == 0) {
            LOG.warning("[CUtil] could not find the cost check in " + cn.name + ", the too expensive option will not work");
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(writer);
        LOG.info("[CUtil] patched " + sites + " anvil cost check(s) in " + cn.name);
        return writer.toByteArray();
    }

    private static int patchMethod(ClassNode cn, MethodNode mn, boolean container, String hook) {
        List<AbstractInsnNode> sites = new ArrayList<AbstractInsnNode>();

        AbstractInsnNode[] insns = mn.instructions.toArray();
        for(AbstractInsnNode insn : insns) {
            if(insn.getOpcode() != Opcodes.BIPUSH || ((IntInsnNode) insn).operand != VANILLA_LIMIT) continue;

            AbstractInsnNode prev = previousReal(insn);
            AbstractInsnNode next = nextReal(insn);
            if(prev == null || next == null) continue;
            if(prev.getOpcode() != Opcodes.GETFIELD || next.getOpcode() != Opcodes.IF_ICMPLT) continue;

            FieldInsnNode field = (FieldInsnNode) prev;
            if(!"I".equals(field.desc)) continue;
            if(container && !field.owner.equals(cn.name)) continue;

            sites.add(insn);
        }

        for(AbstractInsnNode bipush : sites) {
            mn.instructions.insert(bipush, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, hook, HOOK_DESC));
        }
        return sites.size();
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode node) {
        AbstractInsnNode n = node.getPrevious();
        while(n != null && n.getOpcode() < 0) n = n.getPrevious();
        return n;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode node) {
        AbstractInsnNode n = node.getNext();
        while(n != null && n.getOpcode() < 0) n = n.getNext();
        return n;
    }
}
