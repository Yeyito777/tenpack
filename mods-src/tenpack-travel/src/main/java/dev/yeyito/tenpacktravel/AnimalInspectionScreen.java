package dev.yeyito.tenpacktravel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

final class AnimalInspectionScreen extends Screen {
    private static final int PANEL_WIDTH = 278;
    private static final int LINE = 12;
    private final AnimalInspectionPayload report;

    AnimalInspectionScreen(AnimalInspectionPayload report) {
        super(Component.translatable("screen.tenpack_travel.animal_care.title"));
        this.report = report;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int bottom = Math.min(this.height - 30, 224);
        int commandY = bottom - 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.follow"), button -> sendCommand(AnimalCommand.Mode.FOLLOW))
                .bounds(left + 10, commandY, 58, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.stay"), button -> sendCommand(AnimalCommand.Mode.STAY))
                .bounds(left + 72, commandY, 48, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.roam"), button -> sendCommand(AnimalCommand.Mode.ROAM))
                .bounds(left + 124, commandY, 52, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.command.free"), button -> sendCommand(AnimalCommand.Mode.FREE))
                .bounds(left + 180, commandY, 46, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 74, bottom, 64, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = Math.max(18, (this.height - 228) / 2);
        int right = left + PANEL_WIDTH;
        int bottom = Math.min(this.height - 18, top + 224);

        graphics.fill(left, top, right, bottom, 0xE61B1510);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xEE2A2118);
        graphics.fill(left + 8, top + 25, right - 8, top + 26, 0xFF8B6B3F);

        int y = top + 9;
        graphics.drawString(this.font, report.name(), left + 12, y, 0xFFFFD98A, false);
        graphics.drawString(this.font, report.species(), left + 12, y + LINE, 0xFF9E9487, false);

        y = top + 34;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.health"), report.health());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.mood"), report.mood());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.care"), report.care());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.command"), report.command());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.bond"), report.bond());
        y += LINE;
        drawBondBar(graphics, left + 64, y + 2, Math.min(999, Math.max(0, report.bondXp())));
        y += LINE + 3;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.temper"), report.temperament());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.role"), report.role());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.speed"), report.speed());
        y += LINE;
        drawField(graphics, left, y, Component.translatable("screen.tenpack_travel.animal_care.field.jump"), report.jump());
        y += LINE + 4;

        if (!report.notes().isBlank()) {
            y = drawWrapped(graphics, left + 12, y, Component.translatable("screen.tenpack_travel.animal_care.notes", report.notes()), 0xFFE6D3B2, right - left - 24);
        }
        if (!report.debug().isBlank()) {
            drawWrapped(graphics, left + 12, y + 4, Component.translatable("screen.tenpack_travel.animal_care.debug", report.debug()), 0xFF7C746B, right - left - 24);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawField(GuiGraphics graphics, int left, int y, Component label, String value) {
        graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.field_with_colon", label), left + 12, y, 0xFF9E9487, false);
        graphics.drawString(this.font, value, left + 64, y, 0xFFF4E4C5, false);
    }

    private void sendCommand(AnimalCommand.Mode mode) {
        PacketDistributor.sendToServer(new AnimalCommandPayload(report.entityId(), mode));
        onClose();
    }

    private int drawWrapped(GuiGraphics graphics, int x, int y, Component text, int color, int width) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    private void drawBondBar(GuiGraphics graphics, int x, int y, int xp) {
        int width = 138;
        int fill = Math.min(width, Math.max(2, (int) Math.round(width * (xp / 220.0D))));
        graphics.fill(x, y, x + width, y + 5, 0xFF3C3328);
        graphics.fill(x + 1, y + 1, x + fill - 1, y + 4, 0xFFD79B43);
        graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.animal_care.bond_bar"), x + width + 6, y - 3, 0xFF9E9487, false);
    }
}
