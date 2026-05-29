package net.reimaginedpixel.forcerender;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ForceRenderConfigScreen extends Screen {

    private static final int    WIDGET_WIDTH  = 200;
    private static final int    WIDGET_HEIGHT = 20;
    private static final int    ROW_SPACING   = 28;
    private static final int    BOTTOM_MARGIN = 29;
    private static final String COLOR_ON      = "§a";
    private static final String COLOR_OFF     = "§c";

    private final Screen parent;

    // Working copies — only committed to config when Done is pressed
    private boolean pendingEnabled;
    private int     pendingRange;

    private ButtonWidget toggleButton;

    public ForceRenderConfigScreen(Screen parent) {
        super(Text.literal("ForceRender Settings"));
        this.parent         = parent;
        this.pendingEnabled = ForceRenderConfig.enabled;
        this.pendingRange   = ForceRenderConfig.renderRange;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY  = height / 4;

        // ── Enable / Disable toggle ────────────────────────────────────────
        toggleButton = ButtonWidget.builder(buildToggleLabel(), btn -> {
            pendingEnabled = !pendingEnabled;
            toggleButton.setMessage(buildToggleLabel());
        })
        .dimensions(centerX - WIDGET_WIDTH / 2, startY, WIDGET_WIDTH, WIDGET_HEIGHT)
        .build();
        addDrawableChild(toggleButton);

        // ── Range slider ───────────────────────────────────────────────────
        addDrawableChild(new RangeSlider(
                centerX - WIDGET_WIDTH / 2,
                startY + ROW_SPACING,
                WIDGET_WIDTH,
                WIDGET_HEIGHT));

        // ── Done / Cancel ──────────────────────────────────────────────────
        int bottomY  = height - BOTTOM_MARGIN;
        int halfWidth = WIDGET_WIDTH / 2 - 2;

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), btn -> saveAndClose())
                .dimensions(centerX - WIDGET_WIDTH / 2, bottomY, halfWidth, WIDGET_HEIGHT)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> discardAndClose())
                .dimensions(centerX + 2, bottomY, halfWidth, WIDGET_HEIGHT)
                .build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // ModMenu already applies the blur shader for the parent screen; calling it
        // again on the same frame throws "Can only blur once per frame". Use the
        // plain darkening overlay instead.
        this.renderDarkening(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);

        // Label above the slider
        String rangeLabel = "Render Range: " + pendingRange + " block" + (pendingRange == 1 ? "" : "s");
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(rangeLabel),
                width / 2,
                height / 4 + ROW_SPACING - 11,
                0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        discardAndClose();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Text buildToggleLabel() {
        String state = pendingEnabled ? COLOR_ON + "ON" : COLOR_OFF + "OFF";
        return Text.literal("Force Render: " + state);
    }

    private void saveAndClose() {
        ForceRenderConfig.enabled     = pendingEnabled;
        ForceRenderConfig.renderRange = pendingRange;
        ForceRenderConfig.save();
        client.setScreen(parent);
    }

    private void discardAndClose() {
        client.setScreen(parent);
    }

    // ── Inner slider widget ────────────────────────────────────────────────

    private class RangeSlider extends SliderWidget {

        // Maps 0.0–1.0 slider value to 1–256 blocks
        private static final int MIN_RANGE = 1;
        private static final int MAX_RANGE = 256;

        RangeSlider(int x, int y, int width, int height) {
            super(x, y, width, height,
                  Text.empty(),
                  toSliderValue(ForceRenderConfigScreen.this.pendingRange));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int range = toBlockRange(this.value);
            setMessage(Text.literal("Render Range: " + range + " block" + (range == 1 ? "" : "s")));
        }

        @Override
        protected void applyValue() {
            ForceRenderConfigScreen.this.pendingRange = toBlockRange(this.value);
        }

        private static double toSliderValue(int blocks) {
            return (double) (blocks - MIN_RANGE) / (MAX_RANGE - MIN_RANGE);
        }

        private static int toBlockRange(double sliderValue) {
            return MIN_RANGE + (int) Math.round(sliderValue * (MAX_RANGE - MIN_RANGE));
        }
    }
}
