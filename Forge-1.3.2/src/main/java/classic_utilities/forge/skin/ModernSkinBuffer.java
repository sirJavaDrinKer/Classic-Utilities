package classic_utilities.forge.skin;

import classic_utilities.forge.skin.ModernSkinConverter;
import net.minecraft.src.ImageBufferDownload;

import java.awt.image.BufferedImage;


public class ModernSkinBuffer extends ImageBufferDownload {
    private final String url;
    private final boolean slim;

    public ModernSkinBuffer(String url, boolean slim) {
        this.url = url;
        this.slim = slim;
    }

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        BufferedImage converted = ModernSkinConverter.toModern(image);
        if(converted != null) ModernSkins.markReady(url, slim);
        return converted;
    }
}
