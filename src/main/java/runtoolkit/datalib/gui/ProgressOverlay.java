package runtoolkit.datalib.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Progress bar overlay that is shown during datapack operations.
 * After completion, automatically returns to the parent menu.
 */
public class ProgressOverlay {

    private float progress = 0.0f;
    private String message = "";
    private boolean completed = false;
    private boolean error = false;
    private long completionTime = 0;
    private static final long RETURN_DELAY_MS = 1200;

    public void update(float progress, String message) {
        if (progress < 0) {
            this.error = true;
            this.completed = true;
            this.completionTime = System.currentTimeMillis();
            this.message = message;
            this.progress = 1.0f;
        } else {
            this.progress = Math.min(progress, 1.0f);
            this.message = message;
            if (progress >= 1.0f && !completed) {
                this.completed = true;
                this.completionTime = System.currentTimeMillis();
            }
        }
    }

    public boolean shouldReturnToParent() {
        return completed && (System.currentTimeMillis() - completionTime > RETURN_DELAY_MS);
    }

    public boolean isActive() {
        return progress > 0.0f || !message.isEmpty();
    }

    public boolean isError() {
        return error;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        int barWidth = 300;
        int barHeight = 20;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight / 2 - 20;

        // Semi-transparent background
        context.fill(0, 0, screenWidth, screenHeight, 0xA0000000);

        // Title
        String title = error ? "§cError!" : (completed ? "§aCompleted!" : "§eProcessing...");
        context.drawCenteredTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                Text.literal(title), screenWidth / 2, y - 20, 0xFFFFFF
        );

        // Progress bar background
        context.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFFAAAAAA);
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

        // Progress bar fill
        int fillWidth = (int) (barWidth * progress);
        int fillColor = error ? 0xFFFF3333 : (completed ? 0xFF33FF33 : 0xFF3399FF);
        context.fill(x, y, x + fillWidth, y + barHeight, fillColor);

        // Percentage text
        String percent = String.format("%.0f%%", progress * 100);
        context.drawCenteredTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                Text.literal(percent), screenWidth / 2, y + 4, 0xFFFFFF
        );

        // Message
        if (message != null && !message.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                    Text.literal(message), screenWidth / 2, y + barHeight + 8, 0xCCCCCC
            );
        }
    }

    public void reset() {
        progress = 0.0f;
        message = "";
        completed = false;
        error = false;
        completionTime = 0;
    }
}
