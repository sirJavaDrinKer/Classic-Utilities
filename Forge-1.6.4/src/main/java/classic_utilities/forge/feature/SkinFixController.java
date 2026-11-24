package classic_utilities.forge.feature;

import classic_utilities.forge.gui.GuiJavaWarning;
import classic_utilities.forge.rendering.RenderPlayerModern;
import classic_utilities.forge.skin.ModernSkinBuffer;
import classic_utilities.forge.skin.ModernSkins;
import classic_utilities.options.ClientOptions;
import classic_utilities.skin.SkinLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.TextureObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class SkinFixController {
    private static final class Applied {
        final String name;
        final String key;
        final ResourceLocation location;
        final String url;
        final ThreadDownloadImageData data;
        boolean verified;

        Applied(String name, String key, ResourceLocation location, String url, ThreadDownloadImageData data) {
            this.name = name;
            this.key = key;
            this.location = location;
            this.url = url;
            this.data = data;
        }
    }

    // What vanilla 1.6 shows until a skin has downloaded.
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/steve.png");

    private static final String CACHE_FILE_NAME = "classic_utilities_skin_cache.txt";

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private final Minecraft client;
    private final SkinLookup lookup = new SkinLookup(ClientOptions.siblingFile(CACHE_FILE_NAME));
    // Identity, not equals(): Entity.equals() compares entity ids, and a respawned or re-spawned player keeps its id.
    private final Map<EntityPlayer, Applied> applied = new IdentityHashMap<EntityPlayer, Applied>();
    // The texture manager keeps our registrations after a world is left, so remember them per skin location.
    // Joining again reuses the texture instead of downloading it a second time.
    private final Map<String, Applied> installed = new HashMap<String, Applied>();

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
            if(!installed.isEmpty()) revertAll();
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

    private void apply(EntityPlayer entity) {
        if(!(entity instanceof AbstractClientPlayer)) return;
        AbstractClientPlayer player = (AbstractClientPlayer) entity;

        String name = player.username;
        if(name == null) return;

        Applied current = applied.get(player);
        SkinLookup.Result result = lookup.get(name);

        if(current != null) {
            if(result == null || result.textureUrl.equals(current.url)) {
                confirm(current);
                return;
            }

            // The player changed skin, replace our texture with one for the new url.
            applied.put(player, install(player, name, result));
            return;
        }

        if(result == null) return;
        applied.put(player, install(player, name, result));
    }

    /**
     * Registers a downloader for the modern skin under the player's own skin location. Vanilla looks the skin up
     * by that location every frame, so no player field has to be touched (they are private in 1.6).
     */
    private Applied install(AbstractClientPlayer player, String name, SkinLookup.Result result) {
        ResourceLocation location = player.getLocationSkin();
        String key = ModernSkins.keyFor(player);

        Applied prior = installed.get(key);
        ThreadDownloadImageData replaced = null;
        if(prior != null && client.renderEngine.getTexture(location) == prior.data) {
            if(prior.url.equals(result.textureUrl)) return prior;
            replaced = prior.data;
        }

        // Until the new skin is converted the vanilla model is used.
        ModernSkins.forget(key);
        ThreadDownloadImageData data = new ThreadDownloadImageData(result.textureUrl, STEVE, new ModernSkinBuffer(key, result.slim));
        client.renderEngine.loadTexture(location, data);
        if(replaced != null) deleteGlTexture(replaced);

        Applied now = new Applied(name, key, location, result.textureUrl, data);
        installed.put(key, now);
        return now;
    }

    private void confirm(Applied a) {
        if(a.verified || ModernSkins.slimFor(a.key) == null) return;
        a.verified = true;
        lookup.confirm(a.name, a.url);
    }

    private void revertAll() {
        for(Applied a : installed.values()) {
            // Only undo what is still ours, something else may have replaced the texture since.
            TextureObject registered = client.renderEngine.getTexture(a.location);
            if(registered != a.data) continue;

            ModernSkins.forget(a.key);
            String vanillaUrl = String.format("http://skins.minecraft.net/MinecraftSkins/%s.png", StringUtils.stripControlCodes(a.name));
            client.renderEngine.loadTexture(a.location, new ThreadDownloadImageData(vanillaUrl, STEVE, new ImageBufferDownload()));
            deleteGlTexture(a.data);
        }
        installed.clear();
        applied.clear();
    }

    private static void deleteGlTexture(ThreadDownloadImageData data) {
        try {
            int id = data.getGlTextureId();
            if(id != -1) GL11.glDeleteTextures(id);
        } catch(Throwable t) {
            LOG.warning("[CUtil] could not free a replaced skin texture: " + t);
        }
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
            // getEntityClassRenderObject() caches what it found for subclasses (EntityOtherPlayerMP and friends),
            // those entries would keep pointing at the old renderer.
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