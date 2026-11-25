package classic_utilities.forge.gui;

import classic_utilities.forge.options.OptionsClient;
import classic_utilities.options.ClientOptions;
import classic_utilities.options.ClientWorldOptions;
import classic_utilities.options.WorldOptions;
import classic_utilities.update.UpdateInstaller;
import classic_utilities.util.JavaVersion;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiOptionsMenu extends GuiScreen {
    private static final int DONE_BUTTON_ID = 0;
    private static final int PREV_BUTTON_ID = 1;
    private static final int NEXT_BUTTON_ID = 2;
    private static final int WORLD_BUTTON_BASE = 100;
    private static final int CLIENT_BUTTON_BASE = 200;
    private static final int UPDATE_LINE_BOTTOM_MARGIN = 10;

    private static final int PAGE_FEATURES = 0;
    private static final int PAGE_GAMEPLAY = 1;
    private static final int PAGE_CLIENT = 2;
    private static final int PAGE_COUNT = 3;
    private static final String[] PAGE_TITLES = { "Allowed Features", "Gameplay Changes", "Client Options" };

    private static final int[] FEATURE_IDS = {
            WorldOptions.FREECAM,
            WorldOptions.FREECAM_COLLISION,
            WorldOptions.NOFOG
    };
    private static final int[] GAMEPLAY_IDS = {
            WorldOptions.ENDERMAN_GRIEFING,
            WorldOptions.ENCHANT_PREVIEW,
            WorldOptions.REMOVE_ANVIL_LIMIT
    };

    private static final int ROW_HEIGHT = 24;

    private final GuiScreen parentScreen;
    private final GuiButton[] worldButtons = new GuiButton[WorldOptions.COUNT];
    private final GuiButton[] clientButtons = new GuiButton[ClientOptions.COUNT];

    private int page = PAGE_FEATURES;

    private int pendingPageChange;

    public GuiOptionsMenu(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        java.util.Arrays.fill(worldButtons, null);
        java.util.Arrays.fill(clientButtons, null);

        int top = this.height / 6 + 24;
        if(page != PAGE_CLIENT) {
            int[] ids = worldIdsForPage();
            for(int row = 0; row < ids.length; row++) {
                int id = ids[row];
                worldButtons[id] = optionButton(WORLD_BUTTON_BASE + id, top + row * ROW_HEIGHT);
            }
        } else {
            for(int id = 0; id < ClientOptions.COUNT; id++) {
                clientButtons[id] = optionButton(CLIENT_BUTTON_BASE + id, top + id * ROW_HEIGHT);
            }
        }

        int bottom = this.height / 6 + 168;
        this.buttonList.add(new GuiButton(PREV_BUTTON_ID, this.width / 2 - 100 - 24, bottom, 20, 20, "<"));
        this.buttonList.add(new GuiButton(DONE_BUTTON_ID, this.width / 2 - 100, bottom, 200, 20, "Done"));
        this.buttonList.add(new GuiButton(NEXT_BUTTON_ID, this.width / 2 + 100 + 4, bottom, 20, 20, ">"));

        refreshButtons();

        if(this.mc.theWorld != null) OptionsClient.sendQuery();
    }

    private GuiButton optionButton(int id, int y) {
        GuiButton button = new GuiButton(id, this.width / 2 - 100, y, 200, 20, "");
        this.buttonList.add(button);
        return button;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if(pendingPageChange != 0) {
            int change = pendingPageChange;
            pendingPageChange = 0;
            setPage(page + change);
        }

        refreshButtons();
    }

    private int[] worldIdsForPage() {
        return page == PAGE_GAMEPLAY ? GAMEPLAY_IDS : FEATURE_IDS;
    }

    private boolean pageHasWorldId(int id) {
        if(page == PAGE_CLIENT) return false;
        for(int pageId : worldIdsForPage()) {
            if(pageId == id) return true;
        }
        return false;
    }

    private boolean worldEditable() {
        return this.mc.theWorld != null && this.mc.thePlayer != null && ClientWorldOptions.canEdit();
    }

    private void refreshButtons() {
        WorldOptions options = ClientWorldOptions.get();
        boolean editable = worldEditable();
        for(int id = 0; id < WorldOptions.COUNT; id++) {
            if(worldButtons[id] == null) continue;
            worldButtons[id].displayString = WorldOptions.label(id) + ": " + (options.get(id) ? "ON" : "OFF");
            worldButtons[id].enabled = editable;
        }

        for(int id = 0; id < ClientOptions.COUNT; id++) {
            if(clientButtons[id] == null) continue;
            clientButtons[id].displayString = ClientOptions.label(id) + ": " + (ClientOptions.get(id) ? "ON" : "OFF");
            clientButtons[id].enabled = true;
        }
    }

    private String disabledReason() {
        if(this.mc.theWorld == null) return "Join a world to change these options.";
        if(!ClientWorldOptions.isSynced()) return "Waiting for the server (CUtil may not be installed).";
        if(!ClientWorldOptions.canEdit()) return "Only the world host or an operator can change these.";
        return null;
    }

    private String pageNote() {
        if(page != PAGE_CLIENT) return disabledReason();
        return "These only affect your game, not the server.";
    }

    private int optionCount() {
        return page == PAGE_CLIENT ? ClientOptions.COUNT : worldIdsForPage().length;
    }

    private void setPage(int newPage) {
        page = (newPage % PAGE_COUNT + PAGE_COUNT) % PAGE_COUNT;
        initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == DONE_BUTTON_ID) {
            this.mc.displayGuiScreen(parentScreen);
            return;
        }
        if(button.id == PREV_BUTTON_ID) {
            pendingPageChange = -1;
            return;
        }
        if(button.id == NEXT_BUTTON_ID) {
            pendingPageChange = 1;
            return;
        }

        int worldId = button.id - WORLD_BUTTON_BASE;
        if(WorldOptions.isValidId(worldId) && pageHasWorldId(worldId)) {
            if(worldEditable()) OptionsClient.sendSet(worldId, !ClientWorldOptions.get().get(worldId));
            return;
        }

        int clientId = button.id - CLIENT_BUTTON_BASE;
        if(page == PAGE_CLIENT && ClientOptions.isValidId(clientId)) {
            if(clientId == ClientOptions.SKIN_FIX && !ClientOptions.isSkinFix() && JavaVersion.isTooOld()) {
                this.mc.displayGuiScreen(GuiJavaWarning.beforeEnabling(this));
                return;
            }
            ClientOptions.toggle(clientId);
            refreshButtons();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if(button == 0 && UpdateInstaller.isClickable() && overUpdateLine(mouseX, mouseY)) {
            if(UpdateInstaller.needsWarning()) {
                this.mc.displayGuiScreen(new GuiUpdateWarning(this));
            } else {
                UpdateInstaller.click();
            }
        }
    }

    private boolean overUpdateLine(int mouseX, int mouseY) {
        String text = UpdateInstaller.statusText();
        if(text == null) return false;
        int textWidth = this.fontRenderer.getStringWidth(text);
        int x = (this.width - textWidth) / 2;
        int y = this.height - UPDATE_LINE_BOTTOM_MARGIN;
        return mouseX >= x && mouseX <= x + textWidth && mouseY >= y - 1 && mouseY <= y + 9;
    }

    private void drawUpdateLine(int mouseX, int mouseY) {
        String text = UpdateInstaller.statusText();
        if(text == null) return;

        UpdateInstaller.State state = UpdateInstaller.getState();
        String style = "\u00a7e";
        if(state == UpdateInstaller.State.DOWNLOADING) style = "\u00a77";
        else if(state == UpdateInstaller.State.SUCCESS) style = "\u00a7a";
        else if(state == UpdateInstaller.State.FAILED) style = "\u00a7c";

        if(UpdateInstaller.isClickable() && overUpdateLine(mouseX, mouseY)) style += "\u00a7n";

        int x = (this.width - this.fontRenderer.getStringWidth(text)) / 2;
        this.drawString(this.fontRenderer, style + text, x, this.height - UPDATE_LINE_BOTTOM_MARGIN, 0xFFFFFF);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "CUtil Options", this.width / 2, 15, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer,
                PAGE_TITLES[page] + " (" + (page + 1) + "/" + PAGE_COUNT + ")",
                this.width / 2, this.height / 6 + 8, 0xFFFFA0);

        String note = pageNote();
        if(note != null) {
            this.drawCenteredString(this.fontRenderer, note, this.width / 2, this.height / 6 + 24 + optionCount() * ROW_HEIGHT + 4, 0xA0A0A0);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawUpdateLine(mouseX, mouseY);
    }
}