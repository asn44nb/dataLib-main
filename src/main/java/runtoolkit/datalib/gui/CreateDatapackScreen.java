package runtoolkit.datalib.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import runtoolkit.datalib.datapack.DatapackGenerator;

/**
 * Screen for creating a new datapack in-game.
 * Shows a progress bar during creation, then returns to main menu.
 */
public class CreateDatapackScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget descField;
    private final ProgressOverlay progressOverlay = new ProgressOverlay();
    private boolean processing = false;

    public CreateDatapackScreen(Screen parent) {
        super(Text.literal("Create Datapack"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 200, 20, Text.literal("Pack Name"));
        nameField.setPlaceholder(Text.literal("§7my_datapack"));
        nameField.setMaxLength(64);
        this.addDrawableChild(nameField);

        descField = new TextFieldWidget(this.textRenderer, centerX - 100, startY + 40, 200, 20, Text.literal("Description"));
        descField.setPlaceholder(Text.literal("§7A DataLib datapack"));
        descField.setMaxLength(256);
        this.addDrawableChild(descField);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✔ Create"),
                btn -> onCreate()
        ).dimensions(centerX - 100, startY + 80, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cCancel"),
                btn -> this.close()
        ).dimensions(centerX + 5, startY + 80, 95, 20).build());
    }

    private void onCreate() {
        if (processing) return;
        String name = nameField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty()) {
            nameField.setEditableColor(0xFF5555);
            return;
        }
        name = name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (desc.isEmpty()) desc = "A DataLib managed datapack";

        processing = true;
        progressOverlay.reset();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return;

        String finalName = name;
        String finalDesc = desc;

        Thread worker = new Thread(() -> {
            DatapackGenerator.createDatapack(
                    client.getServer(), finalName, finalDesc,
                    (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
            );
        }, "DataLib-CreatePack");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§a§lCreate Datapack"), this.width / 2, 20, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = this.height / 4;
        context.drawTextWithShadow(this.textRenderer, Text.literal("§fPack Name:"), centerX - 100, startY - 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("§fDescription:"), centerX - 100, startY + 28, 0xFFFFFF);

        if (processing) {
            progressOverlay.render(context, this.width, this.height);
            if (progressOverlay.shouldReturnToParent()) {
                MinecraftClient.getInstance().setScreen(parent);
            }
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
