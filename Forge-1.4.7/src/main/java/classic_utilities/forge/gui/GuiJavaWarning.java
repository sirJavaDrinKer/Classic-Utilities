package classic_utilities.forge.gui;

import classic_utilities.options.ClientOptions;
import classic_utilities.util.JavaVersion;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

public class GuiJavaWarning extends GuiScreen {
    private static final int PRIMARY_BUTTON_ID = 0;
    private static final int SECONDARY_BUTTON_ID = 1;

    private static final String HINT_STYLE = "\u00a78\u00a7o";
    private static final int LINE_HEIGHT = 11;
    private static final int TITLE_HEIGHT = 18;
    private static final int BUTTON_GAP = 8;
    private static final int MAX_TEXT_WIDTH = 300;
    private static final int SIDE_MARGIN = 20;

    private final GuiScreen parentScreen;
    private final boolean lookupFailure;

    private final List<String> lines = new ArrayList<String>();
    private int top;

    private GuiJavaWarning(GuiScreen parentScreen, boolean lookupFailure) {
        this.parentScreen = parentScreen;
        this.lookupFailure = lookupFailure;
    }

    public static GuiJavaWarning beforeEnabling(GuiScreen parent) {
        return new GuiJavaWarning(parent, false);
    }

    public static GuiJavaWarning lookupFailed() {
        return new GuiJavaWarning(null, true);
    }

    @Override
    public void initGui() {
        this.controlList.clear();
        buildLines();

        int textHeight = TITLE_HEIGHT + lines.size() * LINE_HEIGHT;
        int total = textHeight + BUTTON_GAP + 20 + 4 + 20;
        top = Math.max(4, (this.height - total) / 2);

        int buttonsTop = top + textHeight + BUTTON_GAP;
        String primary = lookupFailure ? "Turn Off Skin Fix" : "Enable Anyway";
        String secondary = lookupFailure ? "Dismiss" : "Cancel";
        this.controlList.add(new GuiButton(PRIMARY_BUTTON_ID, this.width / 2 - 100, buttonsTop, 200, 20, primary));
        this.controlList.add(new GuiButton(SECONDARY_BUTTON_ID, this.width / 2 - 100, buttonsTop + 24, 200, 20, secondary));
    }

    private void buildLines() {
        lines.clear();
        int maxWidth = Math.max(100, Math.min(MAX_TEXT_WIDTH, this.width - SIDE_MARGIN * 2));

        if(lookupFailure) {
            addParagraph("Sorry to interrupt! It looks like we couldn't securely connect to Mojang's modern skin servers.", "", maxWidth);
            addParagraph("This typically means that your Java version (" + JavaVersion.current() + ") is too old...", "", maxWidth);
        } else {
            addParagraph("Sorry to interrupt! It looks like you're running Java version " + JavaVersion.current() + "...", "", maxWidth);
            addParagraph("SkinFix needs at least Java " + JavaVersion.MIN_MAJOR + " to reach Mojang's modern skin servers.", "", maxWidth);
        }
        lines.add("");
        addParagraph("If you wish to use SkinFix, install a newer version of Java 8 and pick it in your launcher's instance settings, then restart the game.", "", maxWidth);
        lines.add("");
        addParagraph((lookupFailure ? "" : "Psst... ") + "Prism Launcher may automatically select a version of Java 8 that's just slightly too old...", HINT_STYLE, maxWidth);
        addParagraph("Edit > Settings > Java > Java Executable > Detect", HINT_STYLE, maxWidth);
    }

    /** Word-wraps {@code text} to {@code maxWidth} pixels, putting {@code style} at the start of every line. */
    private void addParagraph(String text, String style, int maxWidth) {
        String[] words = text.trim().split(" +");
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < words.length; i++) {
            String candidate = line.length() == 0 ? words[i] : line + " " + words[i];
            if(line.length() > 0 && this.fontRenderer.getStringWidth(style + candidate) > maxWidth) {
                lines.add(style + line);
                line.setLength(0);
                line.append(words[i]);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if(line.length() > 0) lines.add(style + line);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == PRIMARY_BUTTON_ID) {
            ClientOptions.set(ClientOptions.SKIN_FIX, !lookupFailure);
        } else if(button.id != SECONDARY_BUTTON_ID) {
            return;
        }
        this.mc.displayGuiScreen(parentScreen);
    }

    @Override
    protected void keyTyped(char typed, int keyCode) {
        if(keyCode == 1) this.mc.displayGuiScreen(parentScreen);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        String title = lookupFailure ? "\u00a7cOops! SkinFix could not connect..." : "\u00a7cJava version too old";
        this.drawCenteredString(this.fontRenderer, title, this.width / 2, top, 0xFFFFFF);

        int y = top + TITLE_HEIGHT;
        for(int i = 0; i < lines.size(); i++) {
            this.drawCenteredString(this.fontRenderer, lines.get(i), this.width / 2, y, 0xE0E0E0);
            y += LINE_HEIGHT;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}