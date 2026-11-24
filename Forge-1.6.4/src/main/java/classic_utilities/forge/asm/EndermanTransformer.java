package classic_utilities.forge.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gates the enderman block pickup
 * carriableBlocks[blockId] ; after code insert EndermanGriefing.allowPickup(boolean)
 * rest of AI remains untouched

 * I don't really like working with Mixins but holy shit is this worse
 */
public class EndermanTransformer implements IClassTransformer {
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private static final String HOOK_OWNER = "classic_utilities/forge/options/EndermanGriefing";
    private static final String HOOK_NAME = "allowPickup";
    private static final String HOOK_DESC = "(Z)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if(bytes == null) return null;

        // Removed TARGET_PACKAGED and TARGET_FLAT when porting from 1.4.7 code.
        // Check if the transformed name is the enderman class
        if(!"net.minecraft.entity.monster.EntityEnderman".equals(transformedName)) return bytes;

        try {
            return patch(bytes);
        } catch(Throwable t) {
            LOG.log(Level.SEVERE, "[CUtil] failed to patch EntityEnderman, enderman griefing option will not work", t);
            return bytes;
        }
    }

    private static byte[] patch(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);

        // Find that carriableBlocks field, there's only one static boolean[]
        FieldNode table = null;
        int tables = 0;
        for(Object o : cn.fields) {
            FieldNode f = (FieldNode) o;
            if((f.access & Opcodes.ACC_STATIC) != 0 && "[Z".equals(f.desc)) {
                table = f;
                tables++;
            }
        }

        // No clue why this would happen, but it would probably fuck something up
        // checking if there's > 1 static boolean[]; expects 1
        if(tables != 1) {
            LOG.warning("[CUtil] expected exactly one static boolean[] in " + cn.name + " but found " + tables
                    + ", enderman griefing option will not work");
            return bytes;
        }

        int sites = 0;
        for(Object o : cn.methods) {
            sites += patchMethod(cn, (MethodNode) o, table);
        }
        if(sites == 0) {
            LOG.warning("[CUtil] could not find the pickup check in " + cn.name + ", enderman griefing option will not work");
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(writer);
        LOG.info("[CUtil] patched " + sites + " enderman pickup check(s) in " + cn.name);
        return writer.toByteArray();
    }

    private static int patchMethod(ClassNode cn, MethodNode mn, FieldNode table) {
        if(mn.instructions.size() == 0) return 0;

        Frame[] frames;
        try {
            Analyzer analyzer = new Analyzer(new SourceInterpreter());
            analyzer.analyze(cn.name, mn);
            frames = analyzer.getFrames();
        } catch(Exception e) {
            LOG.log(Level.WARNING, "[CUtil] could not analyse " + cn.name + "." + mn.name + mn.desc, e);
            return 0;
        }

        AbstractInsnNode[] insns = mn.instructions.toArray();
        List<AbstractInsnNode> sites = new ArrayList<AbstractInsnNode>();

        for(int i = 0; i < insns.length; i++) {
            if(insns[i].getOpcode() != Opcodes.BALOAD || frames[i] == null) continue;

            Frame frame = frames[i];
            SourceValue array = (SourceValue) frame.getStack(frame.getStackSize() - 2);
            for(Object source : array.insns) {
                AbstractInsnNode s = (AbstractInsnNode) source;
                if(s.getOpcode() != Opcodes.GETSTATIC) continue;

                FieldInsnNode field = (FieldInsnNode) s;
                if(field.owner.equals(cn.name) && field.name.equals(table.name) && field.desc.equals(table.desc)) {
                    sites.add(insns[i]);
                    break;
                }
            }
        }

        for(AbstractInsnNode baload : sites) {
            mn.instructions.insert(baload, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_NAME, HOOK_DESC));
        }
        return sites.size();
    }
}
