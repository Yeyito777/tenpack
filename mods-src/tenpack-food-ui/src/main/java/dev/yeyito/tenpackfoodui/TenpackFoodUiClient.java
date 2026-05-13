package dev.yeyito.tenpackfoodui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public final class TenpackFoodUiClient {
    private static final int FRAME = 0xCC1B1714;
    private static final int TRACK = 0xCC2F2922;
    private static final int EMPTY = 0xCC4A1F1F;
    private static final int STAPLE = 0xFFE6C84D;
    private static final int PROTEIN = 0xFFE05A4F;
    private static final int PRODUCE = 0xFF69B85A;
    private static final int TEXT = 0xFFEADFC8;
    private static final int WIDTH = 43;
    private static final int HEIGHT = 13;
    private static final int BAR_WIDTH = 11;
    private static final int BAR_HEIGHT = 5;

    private static int staple;
    private static int protein;
    private static int produce;

    private TenpackFoodUiClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TenpackFoodUiClient::renderInventoryGauge);
    }

    public static void update(int staplePoints, int proteinPoints, int producePoints) {
        staple = clamp(staplePoints);
        protein = clamp(proteinPoints);
        produce = clamp(producePoints);
    }

    private static void renderInventoryGauge(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen inventory)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = inventory.getGuiLeft() + inventory.getXSize() - WIDTH - 7;
        int y = inventory.getGuiTop() + 7;

        drawGauge(graphics, x, y);

        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        if (mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT) {
            graphics.renderComponentTooltip(
                    minecraft.font,
                    List.of(
                            Component.translatable("tenpack_food_ui.tooltip.title"),
                            Component.translatable("tenpack_food_ui.tooltip.staple", staple),
                            Component.translatable("tenpack_food_ui.tooltip.protein", protein),
                            Component.translatable("tenpack_food_ui.tooltip.produce", produce)
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    private static void drawGauge(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, FRAME);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xCC231F1A);
        drawMiniBar(graphics, x + 4, y + 4, staple, STAPLE);
        drawMiniBar(graphics, x + 16, y + 4, protein, PROTEIN);
        drawMiniBar(graphics, x + 28, y + 4, produce, PRODUCE);
    }

    private static void drawMiniBar(GuiGraphics graphics, int x, int y, int value, int color) {
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xDD120F0D);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRACK);
        int filled = Math.round((BAR_WIDTH * clamp(value)) / (float) FoodBalanceValues.MAX_POINTS);
        if (filled <= 0) {
            graphics.fill(x, y, x + 1, y + BAR_HEIGHT, EMPTY);
            return;
        }
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, color);
        if (filled < BAR_WIDTH) {
            graphics.fill(x + filled, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xAA3A2F27);
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(FoodBalanceValues.MAX_POINTS, value));
    }
}
