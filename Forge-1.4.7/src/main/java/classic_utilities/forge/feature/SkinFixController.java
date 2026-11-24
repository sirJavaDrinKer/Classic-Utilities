package classic_utilities.forge.feature;

import classic_utilities.forge.gui.GuiJavaWarning;
import classic_utilities.forge.rendering.RenderPlayerModern;
import classic_utilities.forge.skin.ModernSkinBuffer;
import classic_utilities.forge.skin.ModernSkins;
import classic_utilities.options.ClientOptions;
import classic_utilities.skin.SkinLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class SkinFixController {
    private static final class Applied {
        final String name;
        final String original;
        final String applied;
        boolean verified;

        Applied(String name, String original, String applied) {
            this.name = name;
            this.original = original;
            this.applied = applied;
        }
    }

    private static final String CACHE_FILE_NAME = "classic_utilities_skin_cache.txt";

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private final Minecraft client;
    private final SkinLookup lookup = new SkinLookup(ClientOptions.siblingFile(CACHE_FILE_NAME));
    // Identity, not equals(): Entity.equals() compares entity ids, and a respawned or re-spawned player keeps its id.
    private final Map<EntityPlayer, Applied> applied = new IdentityHashMap<EntityPlayer, Applied>();

    private World lastWorld;
    private boolean rendererChecked;
    private boolean rendererInstalled;
    private boolean tlsWarned;

    public SkinFixController(Minecraft client) {
        this.client = client;
    }

    public void tick() {
        World world = client.theWorld;
        if(world != lastWorld) {
            lastWorld = world;
            applied.clear();
        }
        if(world == null) return;

        installRenderer();

        if(!ClientOptions.isSkinFix() || !rendererInstalled) {
            if(!applied.isEmpty()) revertAll();
            return;
        }

        warnIfTlsBroken();

        Set<EntityPlayer> present = Collections.newSetFromMap(new IdentityHashMap<EntityPlayer, Boolean>());
        List players = world.playerEntities;
        for(int i = 0; i < players.size(); i++) {
            Object o = players.get(i);
            if(o instanceof EntityPlayer) {
                present.add((EntityPlayer) o);
                apply((EntityPlayer) o);
            }
        }
        applied.keySet().retainAll(present);
    }

    private void apply(EntityPlayer player) {
        String name = player.username;
        if(name == null) return;

        Applied current = applied.get(player);
        SkinLookup.Result result = lookup.get(name);

        if(current != null) {
            if(result == null || result.textureUrl.equals(current.applied)) {
                confirm(current);
                return;
            }

            RenderEngine engine = client.renderEngine;
            engine.obtainImageData(result.textureUrl, new ModernSkinBuffer(result.textureUrl, result.slim));
            player.skinUrl = result.textureUrl;
            engine.releaseImageData(current.applied);
            applied.put(player, new Applied(name, current.original, result.textureUrl));
            return;
        }

        if(result == null) return;

        String original = player.skinUrl;
        RenderEngine engine = client.renderEngine;

        engine.obtainImageData(result.textureUrl, new ModernSkinBuffer(result.textureUrl, result.slim));
        player.skinUrl = result.textureUrl;
        if(original != null) engine.releaseImageData(original);

        applied.put(player, new Applied(name, original, result.textureUrl));
    }

    private void confirm(Applied a) {
        if(a.verified || ModernSkins.slimFor(a.applied) == null) return;
        a.verified = true;
        lookup.confirm(a.name, a.applied);
    }

    private void revertAll() {
        for(Map.Entry<EntityPlayer, Applied> entry : applied.entrySet()) {
            EntityPlayer player = entry.getKey();
            Applied a = entry.getValue();
            if(player != null && a.applied.equals(player.skinUrl)) {
                ModernSkins.forget(a.applied);
                client.renderEngine.releaseImageData(a.applied);
                player.skinUrl = a.original;
                if(a.original != null) client.renderEngine.obtainImageData(a.original, new ImageBufferDownload());
            }
        }
        applied.clear();
    }

    private void warnIfTlsBroken() {
        if(tlsWarned || !lookup.hasTlsFailure() || client.currentScreen != null) return;
        tlsWarned = true;
        client.displayGuiScreen(GuiJavaWarning.lookupFailed());
    }

    private void installRenderer() {
        if(rendererChecked) return;
        rendererChecked = true;

        try {
            RenderManager manager = RenderManager.instance;
            Render current = manager.getEntityClassRenderObject(EntityPlayer.class);
            if(current instanceof RenderPlayerModern) {
                rendererInstalled = true;
                return;
            }
            if(current == null || current.getClass() != RenderPlayer.class) {
                LOG.warning("[CUtil] player renderer is not vanilla's (" + (current == null ? "null" : current.getClass().getName())
                        + "), modern skin rendering is disabled");
                return;
            }

            Map map = findRenderMap(manager);
            if(map == null || map.get(EntityPlayer.class) != current) {
                LOG.warning("[CUtil] could not find the entity renderer map, modern skin rendering is disabled");
                return;
            }

            RenderPlayerModern replacement = new RenderPlayerModern();
            replacement.setRenderManager(manager);
            map.put(EntityPlayer.class, replacement);
            for(Object o : map.entrySet()) {
                Map.Entry mapEntry = (Map.Entry) o;
                if(mapEntry.getValue() == current) mapEntry.setValue(replacement);
            }
            rendererInstalled = true;
            LOG.info("[CUtil] installed the modern player renderer");
        } catch(Throwable t) {
            LOG.warning("[CUtil] could not install the modern player renderer: " + t);
        }
    }

    private static Map findRenderMap(RenderManager manager) throws IllegalAccessException {
        Field found = null;
        Field[] fields = RenderManager.class.getDeclaredFields();
        for(int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if(!Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
                if(found != null) return null; // ambiguous
                found = f;
            }
        }
        if(found == null) return null;
        found.setAccessible(true);
        return (Map) found.get(manager);
    }
}