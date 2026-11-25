package classic_utilities.forge.gui;

import classic_utilities.update.UpdateConsent;
import classic_utilities.update.UpdateInstaller;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

public class GuiUpdateWarning extends GuiScreen {
    private static final int CONTINUE_BUTTON_ID = 0;
    private static final int CANCEL_BUTTON_ID = 1;

    private static final String MESSAGE = "When using the update feature CU will attempt to check Modrinth for new updates. "
            + "If any updates are found for your current version it will delete the current version of the mod "
            + "and download the newest version from Modrinth.";
    private static final String CHECKBOX_LABEL = "I understand this will modify my game files.";

    private static final int LINE_HEIGHT = 11;
    private static final int TITLE_HEIGHT = 18;
    private static final int MAX_TEXT_WIDTH = 300;
    private static final int SIDE_MARGIN = 20;

    private static final int CHECKBOX_GAP = 10;
    private static final int CHECKBOX_SIZE = 10;
    private static final int CHECKBOX_LABEL_GAP = 4;

    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int BOTTOM_MARGIN = 12;

    private final GuiScreen parentScreen;

    private final List<String> lines = new ArrayList<String>();
    private GuiButton continueButton;
    private boolean checked;
    private int top;
    private int checkboxX;
    private int checkboxY;
    private int checkboxWidth;

    public GuiUpdateWarning(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        buildLines();

        int buttonsY = this.height - BOTTOM_MARGIN - BUTTON_HEIGHT;
        int left = this.width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        continueButton = new GuiButton(CONTINUE_BUTTON_ID, left, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, "Continue");
        continueButton.enabled = checked;
        this.buttonList.add(continueButton);
        this.buttonList.add(new GuiButton(CANCEL_BUTTON_ID, left + BUTTON_WIDTH + BUTTON_GAP, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, "Cancel"));

        int blockHeight = TITLE_HEIGHT + lines.size() * LINE_HEIGHT + CHECKBOX_GAP + CHECKBOX_SIZE;
        top = Math.max(4, (buttonsY - blockHeight) / 2);
        checkboxY = top + TITLE_HEIGHT + lines.size() * LINE_HEIGHT + CHECKBOX_GAP;
        checkboxWidth = CHECKBOX_SIZE + CHECKBOX_LABEL_GAP + this.fontRenderer.getStringWidth(CHECKBOX_LABEL);
        checkboxX = (this.width - checkboxWidth) / 2;
    }

    private void buildLines() {
        lines.clear();
        int maxWidth = Math.max(100, Math.min(MAX_TEXT_WIDTH, this.width - SIDE_MARGIN * 2));
        String[] words = MESSAGE.split(" +");
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < words.length; i++) {
            String candidate = line.length() == 0 ? words[i] : line + " " + words[i];
            if(line.length() > 0 && this.fontRenderer.getStringWidth(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(words[i]);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if(line.length() > 0) lines.add(line.toString());
    }

    private boolean overCheckbox(int mouseX, int mouseY) {
        return mouseX >= checkboxX && mouseX < checkboxX + checkboxWidth
                && mouseY >= checkboxY - 1 && mouseY < checkboxY + CHECKBOX_SIZE + 1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if(button == 0 && overCheckbox(mouseX, mouseY)) {
            checked = !checked;
            continueButton.enabled = checked;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == CONTINUE_BUTTON_ID && checked) {
            UpdateConsent.accept();
            this.mc.displayGuiScreen(parentScreen);
            UpdateInstaller.click();
        } else if(button.id == CANCEL_BUTTON_ID) {
            this.mc.displayGuiScreen(parentScreen);
        }
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

        this.drawCenteredString(this.fontRenderer, "\u00a7cUpdate warning", this.width / 2, top, 0xFFFFFF);

        int y = top + TITLE_HEIGHT;
        for(int i = 0; i < lines.size(); i++) {
            this.drawCenteredString(this.fontRenderer, lines.get(i), this.width / 2, y, 0xE0E0E0);
            y += LINE_HEIGHT;
        }

        drawCheckbox(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCheckbox(int mouseX, int mouseY) {
        int x = checkboxX;
        int y = checkboxY;
        int size = CHECKBOX_SIZE;

        drawRect(x, y, x + size, y + size, overCheckbox(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFA0A0A0);
        drawRect(x + 1, y + 1, x + size - 1, y + size - 1, 0x80000000);

        if(checked) {
            int[][] tick = { { 2, 5 }, { 3, 6 }, { 4, 7 }, { 5, 6 }, { 6, 5 }, { 7, 4 }, { 8, 3 } };
            for(int i = 0; i < tick.length; i++) {
                drawRect(x + tick[i][0], y + tick[i][1], x + tick[i][0] + 1, y + tick[i][1] + 2, 0xFF55FF55);
            }
        }

        this.drawString(this.fontRenderer, CHECKBOX_LABEL, x + size + CHECKBOX_LABEL_GAP, y + 1, 0xE0E0E0);
    }
}