package classic_utilities.forge.skin;

import net.minecraft.client.renderer.ImageBufferDownload;

import java.awt.image.BufferedImage;


public class ModernSkinBuffer extends ImageBufferDownload {
    private final String key;
    private final boolean slim;

    public ModernSkinBuffer(String key, boolean slim) {
        this.key = key;
        this.slim = slim;
    }

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        BufferedImage converted = ModernSkinConverter.toModern(image);
        if(converted != null) ModernSkins.markReady(key, slim);
        return converted;
    }
}
