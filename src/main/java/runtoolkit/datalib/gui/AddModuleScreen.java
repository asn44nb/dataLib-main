package runtoolkit.datalib.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import runtoolkit.datalib.datapack.DatapackGenerator;
import runtoolkit.datalib.datapack.DatapackGenerator.DatapackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for adding modules (mcfunction files) to a datapack.
 */
public class AddModuleScreen extends Screen {

    private final Screen parent;
    private final ProgressOverlay progressOverlay = new ProgressOverlay();
    private boolean processing = false;

    private List<DatapackInfo> datapacks = new ArrayList<>();
    private int selectedPackIndex = -1;
    private TextFieldWidget moduleNameField;
    private TextFieldWidget moduleContentField;
    private int scrollOffset = 0;

    public AddModuleScreen(Screen parent) {
        super(Text.literal("Add Module / mcfunction"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        loadDatapacks();

        int centerX = this.width / 2;
        int inputY = this.height / 2 + 20;

        moduleNameField = new TextFieldWidget(this.textRenderer, centerX - 100, inputY, 200, 20, Text.literal("Module Name"));
        moduleNameField.setPlaceholder(Text.literal("§7module_name (e.g., combat/attack)"));
        moduleNameField.setMaxLength(128);
        this.addDrawableChild(moduleNameField);

        moduleContentField = new TextFieldWidget(this.textRenderer, centerX - 100, inputY + 30, 200, 20, Text.literal("Content"));
        moduleContentField.setPlaceholder(Text.literal("§7say Hello from module!"));
        moduleContentField.setMaxLength(2048);
        this.addDrawableChild(moduleContentField);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✔ Add Module"),
                btn -> onAdd()
        ).dimensions(centerX - 100, inputY + 60, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cCancel"),
                btn -> this.close()
        ).dimensions(centerX + 5, inputY + 60, 95, 20).build());
    }

    private void loadDatapacks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            datapacks = DatapackGenerator.listDatapacks(client.getServer());
        }
    }

    private void onAdd() {
        if (processing) return;
        if (selectedPackIndex < 0 || selectedPackIndex >= datapacks.size()) return;

        String moduleName = moduleNameField.getText().trim();
        String content = moduleContentField.getText().trim();

        if (moduleName.isEmpty()) {
            moduleNameField.setEditableColor(0xFF5555);
            return;
        }

        moduleName = moduleName.toLowerCase().replaceAll("[^a-z0-9_/]", "_");

        processing = true;
        progressOverlay.reset();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return;

        String packName = datapacks.get(selectedPackIndex).name();
        String finalModuleName = moduleName;
        String finalContent = content.isEmpty() ? "# Empty module" : content;

        Thread worker = new Thread(() -> {
            DatapackGenerator.addModule(
                    client.getServer(), packName, finalModuleName, finalContent,
                    (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
            );
        }, "DataLib-AddModule");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!processing) {
            int listX = this.width / 2 - 120;
            int listY = 50;
            int entryHeight = 18;
            int maxVisible = Math.min(datapacks.size(), (this.height / 2 - 40) / entryHeight);

            for (int i = 0; i < maxVisible; i++) {
                int idx = i + scrollOffset;
                if (idx >= datapacks.size()) break;

                int entryY = listY + i * entryHeight;
                if (mouseX >= listX && mouseX <= listX + 240 && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    selectedPackIndex = idx;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int entryHeight = 18;
        int maxVisible = (this.height / 2 - 40) / entryHeight;
        if (verticalAmount > 0 && scrollOffset > 0) scrollOffset--;
        if (verticalAmount < 0 && scrollOffset < datapacks.size() - maxVisible) scrollOffset++;
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§b§lAdd Module / mcfunction"), this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Select a datapack, enter module name and content"),
                this.width / 2, 30, 0x888888);

        int listX = this.width / 2 - 120;
        int listY = 50;
        int entryHeight = 18;
        int maxVisible = Math.min(datapacks.size(), (this.height / 2 - 40) / entryHeight);

        for (int i = 0; i < maxVisible; i++) {
            int idx = i + scrollOffset;
            if (idx >= datapacks.size()) break;

            DatapackInfo pack = datapacks.get(idx);
            int entryY = listY + i * entryHeight;
            boolean selected = idx == selectedPackIndex;

            context.fill(listX, entryY, listX + 240, entryY + entryHeight - 2,
                    selected ? 0xFF335599 : 0xFF222222);

            String label = "§f" + pack.name();
            context.drawTextWithShadow(this.textRenderer, Text.literal(label), listX + 4, entryY + 4, 0xFFFFFF);
        }

        int inputY = this.height / 2 + 20;
        context.drawTextWithShadow(this.textRenderer, Text.literal("§fModule Name:"), this.width / 2 - 100, inputY - 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("§fContent (mcfunction):"), this.width / 2 - 100, inputY + 18, 0xFFFFFF);

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
