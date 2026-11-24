package classic_utilities.forge.asm;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class EndermanCorePlugin implements IFMLLoadingPlugin {
    public String[] getLibraryRequestClass() {
        return null;
    }

    public String[] getASMTransformerClass() {
        return new String[] {"classic_utilities.forge.asm.EndermanTransformer"};
    }

    public String getModContainerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> data) {
    }
}
