package classic_utilities.forge.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class GuiOptionsScreenHook {
    private static Field parentScreenField;
    private static boolean parentFieldChecked;
    private static boolean warningPrinted;

    private GuiOptionsScreenHook() {
    }

    public static void update(Minecraft client) {
        GuiScreen currentScreen = client.currentScreen;
        if(currentScreen == null || currentScreen.getClass() != net.minecraft.client.gui.GuiOptions.class) {
            return;
        }

        Field field = getParentScreenField();
        if(field == null) {
            if(!warningPrinted) {
                System.err.println("[CUtil] Could not find the parent screen field on GuiOptions; leaving the vanilla Options screen unchanged.");
                warningPrinted = true;
            }
            return;
        }

        try {
            GuiScreen parent = (GuiScreen) field.get(currentScreen);
            client.displayGuiScreen(new GuiOptions(parent, client.gameSettings));
        } catch(IllegalAccessException e) {
            if(!warningPrinted) {
                System.err.println("[CUtil] Could not read the parent screen from GuiOptions; leaving the vanilla Options screen unchanged.");
                e.printStackTrace();
                warningPrinted = true;
            }
        }
    }

    private static Field getParentScreenField() {
        if(parentFieldChecked) {
            return parentScreenField;
        }
        parentFieldChecked = true;

        Class<?> type = net.minecraft.client.gui.GuiOptions.class;
        while(type != null) {
            Field[] fields = type.getDeclaredFields();
            for(int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if(field.getType() == GuiScreen.class && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    parentScreenField = field;
                    return parentScreenField;
                }
            }
            type = type.getSuperclass();
        }

        return null;
    }
}
