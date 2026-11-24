package classic_utilities.forge.gui;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.GameSettings;

public class GuiOptions extends net.minecraft.src.GuiOptions {
    private static final int CUTIL_BUTTON_ID = 10001;
    private static final int CUTIL_BUTTON_MARGIN = 4;

    public GuiOptions(GuiScreen parentScreen, GameSettings gameSettings) {
        super(parentScreen, gameSettings);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.controlList.add(new GuiButtonSettings(
                CUTIL_BUTTON_ID,
                CUTIL_BUTTON_MARGIN,
                CUTIL_BUTTON_MARGIN
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == CUTIL_BUTTON_ID) {
            this.mc.displayGuiScreen(new GuiOptionsMenu(this));
            return;
        }

        super.actionPerformed(button);
    }
}
