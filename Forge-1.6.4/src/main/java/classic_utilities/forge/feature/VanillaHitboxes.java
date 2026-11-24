package classic_utilities.forge.feature;

import net.minecraft.client.renderer.entity.RenderManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;


public class VanillaHitboxes {
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private boolean searched;
    private Field flag;

    public void suppress() {
        if(!searched) {
            searched = true;
            flag = find();
        }
        if(flag == null) return;

        try {
            if(flag.getBoolean(null)) flag.setBoolean(null, false);
        } catch(Exception e) {
            LOG.warning("[CUtil] lost access to the vanilla hitbox flag: " + e);
            flag = null;
        }
    }

    private static Field find() {
        Field found = null;
        int candidates = 0;

        Field[] fields = RenderManager.class.getDeclaredFields();
        for(int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            int mods = f.getModifiers();
            if(Modifier.isStatic(mods) && !Modifier.isFinal(mods) && f.getType() == boolean.class) {
                found = f;
                candidates++;
            }
        }

        if(candidates != 1) {
            LOG.warning("[CUtil] expected one static boolean in RenderManager but found " + candidates
                    + "; the vanilla F3+B boxes will still draw underneath the better ones");
            return null;
        }

        try {
            found.setAccessible(true);
        } catch(SecurityException e) {
            LOG.warning("[CUtil] could not access the vanilla hitbox flag: " + e);
            return null;
        }
        LOG.info("[CUtil] using RenderManager." + found.getName() + " as the vanilla hitbox flag");
        return found;
    }
}
