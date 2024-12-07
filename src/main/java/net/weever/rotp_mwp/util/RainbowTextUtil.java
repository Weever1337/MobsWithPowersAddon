package net.weever.rotp_mwp.util;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class RainbowTextUtil { // wtf bro
    private static final TextFormatting[] RAINBOW_COLORS = {
            TextFormatting.RED,
            TextFormatting.GOLD,
            TextFormatting.YELLOW,
            TextFormatting.GREEN,
            TextFormatting.AQUA,
            TextFormatting.BLUE,
            TextFormatting.LIGHT_PURPLE
    };

    public static IFormattableTextComponent getRainbowText(String text, boolean bold) {
        long millis = System.currentTimeMillis();
        int colorIndex = (int) ((millis / 1000) % RAINBOW_COLORS.length);

        IFormattableTextComponent textComponent = new StringTextComponent(text)
                .withStyle(RAINBOW_COLORS[colorIndex]);

        if (bold) {
            textComponent = textComponent.withStyle(TextFormatting.BOLD);
        }

        return textComponent;
    }
}
