package dev.yeyito.tenpacktravel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

final class HitchingPostScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int LINE = 12;
    private final HitchingPostPayload report;

    HitchingPostScreen(HitchingPostPayload report) {
        super(Component.translatable("screen.tenpack_travel.hitching_post.title"));
        this.report = report;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int bottom = Math.min(this.height - 30, 244);
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.hitching_post.set_stay"), button -> setPostMode(AnimalCommand.Mode.STAY))
                .bounds(left + 10, bottom - 24, 72, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.hitching_post.set_roam"), button -> setPostMode(AnimalCommand.Mode.ROAM))
                .bounds(left + 88, bottom - 24, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tenpack_travel.hitching_post.forget_missing"), button -> forgetMissing())
                .bounds(left + 170, bottom - 24, 106, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 74, bottom, 64, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = Math.max(14, (this.height - 248) / 2);
        int right = left + PANEL_WIDTH;
        int bottom = Math.min(this.height - 14, top + 244);

        graphics.fill(left, top, right, bottom, 0xE61B1510);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xEE261F18);
        graphics.fill(left + 8, top + 25, right - 8, top + 26, 0xFF7A5731);

        int y = top + 9;
        graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.title"), left + 12, y, 0xFFFFD98A, false);
        graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.stable_board"), right - 84, y, 0xFF8E8376, false);
        y += 18;
        graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.post_mode", report.postMode(), report.postRadius()), left + 12, y, 0xFFE6D3B2, false);
        y += 14;

        if (report.animals().isEmpty()) {
            drawWrapped(graphics, left + 12, y + 12,
                    Component.translatable("screen.tenpack_travel.hitching_post.empty"),
                    0xFFE6D3B2, PANEL_WIDTH - 24);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.count", report.animals().size()), left + 12, y, 0xFFE6D3B2, false);
        y += 16;

        int rowIndex = 0;
        for (HitchingPostPayload.Row row : report.animals()) {
            int rowTop = y;
            int rowBottom = y + 38;
            int bg = rowIndex % 2 == 0 ? 0x552F261D : 0x55382C20;
            graphics.fill(left + 10, rowTop - 2, right - 10, rowBottom, bg);
            graphics.drawString(this.font, row.name(), left + 14, rowTop, 0xFFF4E4C5, false);
            graphics.drawString(this.font, row.proximity(), right - 70, rowTop, 0xFF9E9487, false);
            graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.row_species_role", row.species(), row.role()), left + 14, rowTop + LINE, 0xFF8E8376, false);
            graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.row_mood_command", row.mood(), row.command()), left + 14, rowTop + LINE * 2, 0xFFD8C19B, false);
            graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.row_bond_health", row.bond(), row.health()), left + 166, rowTop + LINE, 0xFFD8C19B, false);
            graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.row_care", row.care()), left + 166, rowTop + LINE * 2, 0xFFD8C19B, false);
            y += 42;
            rowIndex++;
            if (y > bottom - 38) {
                graphics.drawString(this.font, Component.translatable("screen.tenpack_travel.hitching_post.overflow"), left + 14, y, 0xFFB59A72, false);
                break;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int drawWrapped(GuiGraphics graphics, int x, int y, Component text, int color, int width) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    private void setPostMode(AnimalCommand.Mode mode) {
        PacketDistributor.sendToServer(new HitchingPostCommandPayload(report.pos(), mode));
        onClose();
    }

    private void forgetMissing() {
        PacketDistributor.sendToServer(new HitchingPostForgetPayload(report.pos()));
        onClose();
    }
}
