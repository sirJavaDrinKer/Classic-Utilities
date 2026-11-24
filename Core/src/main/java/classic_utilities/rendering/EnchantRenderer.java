package classic_utilities.rendering;

import classic_utilities.enchant.ClientEnchantState;
import classic_utilities.enchant.EnchantPreview;

import java.util.ArrayList;
import java.util.List;

public abstract class EnchantRenderer {
    protected static final int ENCHANT_GUI_WIDTH = 176;
    protected static final int ENCHANT_GUI_HEIGHT = 166;
    protected static final int PREVIEW_MARGIN = 6;
    protected static final int PREVIEW_PADDING = 4;
    protected static final int PREVIEW_LINE_HEIGHT = 10;

    public final void render() {
        if(!tableOpen()) return;

        EnchantPreview preview = ClientEnchantState.get();
        if(!previewMatches(preview)) return;

        List<String> text = new ArrayList<String>();
        List<Integer> color = new ArrayList<Integer>();

        text.add("Enchant Preview");
        color.add(Integer.valueOf(0xFFAA00));

        for(int slot = 0; slot < EnchantPreview.SLOTS; slot++) {
            int cost = preview.costs[slot];
            if(cost <= 0) continue;

            boolean affordable = canAfford(cost);
            text.add("Slot " + (slot + 1) + " (level " + cost + ")");
            color.add(Integer.valueOf(affordable ? 0x55FF55 : 0xFF5555));

            int[] ids = preview.ids[slot];
            if(ids.length == 0) {
                text.add("  nothing");
                color.add(Integer.valueOf(0xAAAAAA));
            }
            for(int i = 0; i < ids.length; i++) {
                text.add("  " + enchantmentName(ids[i], preview.strengths[slot][i]));
                color.add(Integer.valueOf(0xFFFFFF));
            }
        }

        if(text.size() == 1) return;

        int widest = 0;
        for(String line : text) {
            widest = Math.max(widest, textWidth(line));
        }

        int panelWidth = widest + PREVIEW_PADDING * 2;
        int panelHeight = text.size() * PREVIEW_LINE_HEIGHT + PREVIEW_PADDING * 2 - 2;

        int screenWidth = screenWidth();
        int screenHeight = screenHeight();
        int guiLeft = (screenWidth - ENCHANT_GUI_WIDTH) / 2;
        int guiTop = (screenHeight - ENCHANT_GUI_HEIGHT) / 2;
        int x = guiLeft + ENCHANT_GUI_WIDTH + PREVIEW_MARGIN;
        if(x + panelWidth > screenWidth - 2) x = guiLeft - PREVIEW_MARGIN - panelWidth;
        if(x < 2) x = 2;

        begin();
        try {
            fillRect(x, guiTop, x + panelWidth, guiTop + panelHeight, 0.0F, 0.0F, 0.0F, 0.65F);

            for(int i = 0; i < text.size(); i++) {
                drawText(text.get(i), x + PREVIEW_PADDING,
                        guiTop + PREVIEW_PADDING + i * PREVIEW_LINE_HEIGHT, color.get(i).intValue());
            }
        } finally {
            end();
        }
    }

    private boolean previewMatches(EnchantPreview preview) {
        if(preview == null || preview.windowId != tableWindowId()) return false;

        for(int i = 0; i < EnchantPreview.SLOTS; i++) {
            if(preview.costs[i] != tableCost(i)) return false;
        }
        return true;
    }

    protected abstract boolean tableOpen();

    protected abstract int tableWindowId();

    protected abstract int tableCost(int slot);

    protected abstract boolean canAfford(int cost);

    protected abstract String enchantmentName(int id, int strength);

    protected abstract int textWidth(String text);

    protected abstract int screenWidth();

    protected abstract int screenHeight();

    protected abstract void begin();

    protected abstract void fillRect(int x0, int y0, int x1, int y1, float r, float g, float b, float a);

    protected abstract void drawText(String text, int x, int y, int color);

    protected abstract void end();
}

