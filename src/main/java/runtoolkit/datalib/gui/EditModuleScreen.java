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
 * Screen for editing existing modules in a datapack.
 * User selects a pack, selects a module, edits its content,
 * presses save, progress bar runs, then returns to main menu.
 */
public class EditModuleScreen extends Screen {

    private final Screen parent;
    private final ProgressOverlay progressOverlay = new ProgressOverlay();
    private boolean processing = false;

    private List<DatapackInfo> datapacks = new ArrayList<>();
    private List<String> modules = new ArrayList<>();
    private int selectedPackIndex = -1;
    private int selectedModuleIndex = -1;
    private TextFieldWidget contentField;
    private int packScrollOffset = 0;
    private int moduleScrollOffset = 0;

    public EditModuleScreen(Screen parent) {
        super(Text.literal("Edit Module"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        loadDatapacks();

        int centerX = this.width / 2;
        int inputY = this.height - 90;

        // Content field
        contentField = new TextFieldWidget(this.textRenderer, centerX - 150, inputY, 300, 20, Text.literal("Content"));
        contentField.setPlaceholder(Text.literal("§7New content..."));
        contentField.setMaxLength(2048);
        this.addDrawableChild(contentField);

        // Save button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✔ Save"),
                btn -> onSave()
        ).dimensions(centerX - 105, inputY + 30, 100, 20).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cCancel"),
                btn -> this.close()
        ).dimensions(centerX + 5, inputY + 30, 100, 20).build());
    }

    private void loadDatapacks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            datapacks = DatapackGenerator.listDatapacks(client.getServer());
        }
    }

    private void loadModules() {
        modules.clear();
        if (selectedPackIndex < 0 || selectedPackIndex >= datapacks.size()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            modules = DatapackGenerator.listModules(client.getServer(), datapacks.get(selectedPackIndex).name());
        }
        selectedModuleIndex = -1;
        moduleScrollOffset = 0;
    }

    private void onSave() {
        if (processing) return;
        if (selectedPackIndex < 0 || selectedModuleIndex < 0) return;
        if (selectedModuleIndex >= modules.size()) return;

        String content = contentField.getText().trim();
        if (content.isEmpty()) {
            contentField.setEditableColor(0xFF5555);
            return;
        }

        processing = true;
        progressOverlay.reset();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return;

        String packName = datapacks.get(selectedPackIndex).name();
        String moduleName = modules.get(selectedModuleIndex);

        Thread worker = new Thread(() -> {
            DatapackGenerator.editModule(
                    client.getServer(), packName, moduleName, content,
                    (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
            );
        }, "DataLib-EditModule");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!processing) {
            int halfWidth = this.width / 2;

            // Pack list (left side)
            int packListX = 20;
            int listY = 50;
            int entryHeight = 18;
            int maxPackVisible = Math.min(datapacks.size(), (this.height - 150) / entryHeight);

            for (int i = 0; i < maxPackVisible; i++) {
                int idx = i + packScrollOffset;
                if (idx >= datapacks.size()) break;

                int entryY = listY + i * entryHeight;
                if (mouseX >= packListX && mouseX <= packListX + halfWidth - 30
                        && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    selectedPackIndex = idx;
                    loadModules();
                    return true;
                }
            }

            // Module list (right side)
            int moduleListX = halfWidth + 10;
            int maxModVisible = Math.min(modules.size(), (this.height - 150) / entryHeight);

            for (int i = 0; i < maxModVisible; i++) {
                int idx = i + moduleScrollOffset;
                if (idx >= modules.size()) break;

                int entryY = listY + i * entryHeight;
                if (mouseX >= moduleListX && mouseX <= moduleListX + halfWidth - 30
                        && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    selectedModuleIndex = idx;

                    // Load content into field
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.getServer() != null && selectedPackIndex >= 0) {
                        String content = DatapackGenerator.readModule(client.getServer(),
                                datapacks.get(selectedPackIndex).name(), modules.get(idx));
                        if (content != null) {
                            contentField.setText(content);
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int halfWidth = this.width / 2;
        int entryHeight = 18;
        int maxVisible = (this.height - 150) / entryHeight;

        if (mouseX < halfWidth) {
            if (verticalAmount > 0 && packScrollOffset > 0) packScrollOffset--;
            if (verticalAmount < 0 && packScrollOffset < datapacks.size() - maxVisible) packScrollOffset++;
        } else {
            if (verticalAmount > 0 && moduleScrollOffset > 0) moduleScrollOffset--;
            if (verticalAmount < 0 && moduleScrollOffset < modules.size() - maxVisible) moduleScrollOffset++;
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§6§lEdit Module"), this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Select a pack, select a module, edit and save"),
                this.width / 2, 30, 0x888888);

        int halfWidth = this.width / 2;
        int listY = 50;
        int entryHeight = 18;

        // Left column header
        context.drawTextWithShadow(this.textRenderer, Text.literal("§eDatapacks"), 20, 40, 0xFFFF55);

        // Right column header
        context.drawTextWithShadow(this.textRenderer, Text.literal("§bModules"), halfWidth + 10, 40, 0x55FFFF);

        // Pack list
        int maxPackVisible = Math.min(datapacks.size(), (this.height - 150) / entryHeight);
        for (int i = 0; i < maxPackVisible; i++) {
            int idx = i + packScrollOffset;
            if (idx >= datapacks.size()) break;

            int entryY = listY + i * entryHeight;
            boolean selected = idx == selectedPackIndex;

            context.fill(20, entryY, halfWidth - 10, entryY + entryHeight - 2,
                    selected ? 0xFF335599 : 0xFF222222);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("§f" + datapacks.get(idx).name()), 24, entryY + 4, 0xFFFFFF);
        }

        // Module list
        int maxModVisible = Math.min(modules.size(), (this.height - 150) / entryHeight);
        for (int i = 0; i < maxModVisible; i++) {
            int idx = i + moduleScrollOffset;
            if (idx >= modules.size()) break;

            int entryY = listY + i * entryHeight;
            boolean selected = idx == selectedModuleIndex;

            context.fill(halfWidth + 10, entryY, this.width - 20, entryY + entryHeight - 2,
                    selected ? 0xFF335599 : 0xFF222222);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("§f" + modules.get(idx)), halfWidth + 14, entryY + 4, 0xFFFFFF);
        }

        if (modules.isEmpty() && selectedPackIndex >= 0) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("§7No modules"), halfWidth + 14, listY + 4, 0x888888);
        }

        // Content label
        int inputY = this.height - 90;
        context.drawTextWithShadow(this.textRenderer, Text.literal("§fContent:"), this.width / 2 - 150, inputY - 12, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);

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
