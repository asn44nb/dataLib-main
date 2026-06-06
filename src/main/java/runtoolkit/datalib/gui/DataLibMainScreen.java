package runtoolkit.datalib.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Main DataLib GUI screen — central hub for all datapack management.
 */
public class DataLibMainScreen extends Screen {

    private final Screen parent;

    public DataLibMainScreen(Screen parent) {
        super(Text.literal("§b§lDataLib §f— Datapack Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;
        int buttonWidth = 240;
        int buttonHeight = 20;
        int spacing = 26;

        // Create Datapack
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✚ §fCreate Datapack"),
                btn -> MinecraftClient.getInstance().setScreen(new CreateDatapackScreen(this))
        ).dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // Add DataLib Dependency
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e⚡ §fAdd DataLib Dependency"),
                btn -> MinecraftClient.getInstance().setScreen(new AddDependencyScreen(this))
        ).dimensions(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());

        // Add Module / mcfunction
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§b⚙ §fAdd Module / mcfunction"),
                btn -> MinecraftClient.getInstance().setScreen(new AddModuleScreen(this))
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // Edit Module
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§6✎ §fEdit Module"),
                btn -> MinecraftClient.getInstance().setScreen(new EditModuleScreen(this))
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight).build());

        // Delete Module / Dependency / Pack
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§c✖ §fDelete (Module / Dependency / Pack)"),
                btn -> MinecraftClient.getInstance().setScreen(new DeleteScreen(this))
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 4, buttonWidth, buttonHeight).build());

        // Config
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7⚙ §fConfiguration"),
                btn -> MinecraftClient.getInstance().setScreen(new ConfigScreen(this))
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 5, buttonWidth, buttonHeight).build());

        // Close
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cClose"),
                btn -> this.close()
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 6 + 10, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§b§lDataLib §7— §fDatapack Framework Manager"), this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7v6.0.0 for Minecraft 1.21.8"), this.width / 2, 34, 0x888888);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
