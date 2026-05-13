package dev.yeyito.tenpacktravel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

final class WhistleCommandScreen extends Screen {
    private static final int PANEL_WIDTH = 246;
    private static final int PANEL_HEIGHT = 154;
    private static final int LINE = 12;

    WhistleCommandScreen() {
        super(Component.translatable("screen.tenpack_travel.whistle_commands.title"));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(18, (height - PANEL_HEIGHT) / 2);
        int y = top + 48;
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.whistle_commands.call"), button -> call())
                .bounds(left + 14, y, 66, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.follow"), button -> command(AnimalCommand.Mode.FOLLOW))
                .bounds(left + 90, y, 66, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.stay"), button -> command(AnimalCommand.Mode.STAY))
                .bounds(left + 166, y, 66, 20)
                .build());
        y += 26;
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.roam"), button -> command(AnimalCommand.Mode.ROAM))
                .bounds(left + 52, y, 66, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.free"), button -> command(AnimalCommand.Mode.FREE))
                .bounds(left + 128, y, 66, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 76, top + PANEL_HEIGHT - 28, 62, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(18, (height - PANEL_HEIGHT) / 2);
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        graphics.fill(left, top, right, bottom, 0xE61B1510);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xEE2A2118);
        graphics.fill(left + 8, top + 25, right - 8, top + 26, 0xFFD79B43);
        graphics.drawString(font, Component.translatable("screen.tenpack_travel.whistle_commands.title"), left + 12, top + 9, 0xFFFFD98A, false);
        drawWrapped(graphics, left + 12, top + 32,
                Component.translatable("screen.tenpack_travel.whistle_commands.description"),
                0xFFE6D3B2, PANEL_WIDTH - 24);
        graphics.drawString(font, Component.translatable("screen.tenpack_travel.whistle_commands.hint"), left + 12, bottom - 18, 0xFF8E8376, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void call() {
        PacketDistributor.sendToServer(WhistlePayload.INSTANCE);
        onClose();
    }

    private void command(AnimalCommand.Mode mode) {
        PacketDistributor.sendToServer(new WhistleCommandPayload(mode));
        onClose();
    }

    private int drawWrapped(GuiGraphics graphics, int x, int y, Component text, int color, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }
}
