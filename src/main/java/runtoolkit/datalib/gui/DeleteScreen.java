package runtoolkit.datalib.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import runtoolkit.datalib.datapack.DatapackGenerator;
import runtoolkit.datalib.datapack.DatapackGenerator.DatapackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for deleting modules, dependencies, or entire datapacks.
 */
public class DeleteScreen extends Screen {

    private enum DeleteMode {
        MODULE("Delete Module"),
        DEPENDENCY("Remove Dependency"),
        DATAPACK("Delete Datapack");

        final String label;
        DeleteMode(String label) { this.label = label; }
    }

    private final Screen parent;
    private final ProgressOverlay progressOverlay = new ProgressOverlay();
    private boolean processing = false;

    private DeleteMode currentMode = DeleteMode.MODULE;
    private List<DatapackInfo> datapacks = new ArrayList<>();
    private List<String> modules = new ArrayList<>();
    private int selectedPackIndex = -1;
    private int selectedModuleIndex = -1;
    private int scrollOffset = 0;
    private int moduleScrollOffset = 0;
    private boolean confirmMode = false;

    public DeleteScreen(Screen parent) {
        super(Text.literal("Delete"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        loadDatapacks();

        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        // Mode tabs
        int tabY = 45;
        int tabWidth = 76;
        for (DeleteMode mode : DeleteMode.values()) {
            final DeleteMode m = mode;
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(mode.label),
                    btn -> {
                        currentMode = m;
                        selectedPackIndex = -1;
                        selectedModuleIndex = -1;
                        modules.clear();
                        confirmMode = false;
                    }
            ).dimensions(centerX - 120 + mode.ordinal() * (tabWidth + 4), tabY, tabWidth, 16).build());
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§c✖ Delete"),
                btn -> onDelete()
        ).dimensions(centerX + 5, bottomY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7Back"),
                btn -> this.close()
        ).dimensions(centerX - 105, bottomY, 100, 20).build());
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

    private void onDelete() {
        if (processing) return;
        if (selectedPackIndex < 0 || selectedPackIndex >= datapacks.size()) return;

        if (currentMode == DeleteMode.MODULE && (selectedModuleIndex < 0 || selectedModuleIndex >= modules.size())) {
            return;
        }

        if (!confirmMode) {
            confirmMode = true;
            return;
        }

        processing = true;
        progressOverlay.reset();
        confirmMode = false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return;

        String packName = datapacks.get(selectedPackIndex).name();

        Thread worker = new Thread(() -> {
            switch (currentMode) {
                case MODULE -> {
                    String moduleName = modules.get(selectedModuleIndex);
                    DatapackGenerator.removeModule(
                            client.getServer(), packName, moduleName,
                            (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
                    );
                }
                case DEPENDENCY -> {
                    DatapackGenerator.removeDataLibDependency(
                            client.getServer(), packName,
                            (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
                    );
                }
                case DATAPACK -> {
                    DatapackGenerator.deleteDatapack(
                            client.getServer(), packName,
                            (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
                    );
                }
            }
        }, "DataLib-Delete");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!processing) {
            int halfWidth = this.width / 2;
            int listY = 75;
            int entryHeight = 18;

            int maxPackVisible = Math.min(datapacks.size(), (this.height - 140) / entryHeight);
            for (int i = 0; i < maxPackVisible; i++) {
                int idx = i + scrollOffset;
                if (idx >= datapacks.size()) break;

                int entryY = listY + i * entryHeight;
                if (mouseX >= 20 && mouseX <= halfWidth - 10
                        && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    selectedPackIndex = idx;
                    confirmMode = false;
                    if (currentMode == DeleteMode.MODULE) {
                        loadModules();
                    }
                    return true;
                }
            }

            if (currentMode == DeleteMode.MODULE) {
                int moduleListX = halfWidth + 10;
                int maxModVisible = Math.min(modules.size(), (this.height - 140) / entryHeight);
                for (int i = 0; i < maxModVisible; i++) {
                    int idx = i + moduleScrollOffset;
                    if (idx >= modules.size()) break;

                    int entryY = listY + i * entryHeight;
                    if (mouseX >= moduleListX && mouseX <= this.width - 20
                            && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                        selectedModuleIndex = idx;
                        confirmMode = false;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int halfWidth = this.width / 2;
        int entryHeight = 18;
        int maxVisible = (this.height - 140) / entryHeight;

        if (mouseX < halfWidth) {
            if (verticalAmount > 0 && scrollOffset > 0) scrollOffset--;
            if (verticalAmount < 0 && scrollOffset < datapacks.size() - maxVisible) scrollOffset++;
        } else {
            if (verticalAmount > 0 && moduleScrollOffset > 0) moduleScrollOffset--;
            if (verticalAmount < 0 && moduleScrollOffset < modules.size() - maxVisible) moduleScrollOffset++;
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§c§lDelete — §f" + currentMode.label), this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Select target and confirm deletion"),
                this.width / 2, 30, 0x888888);

        int halfWidth = this.width / 2;
        int listY = 75;
        int entryHeight = 18;

        context.drawTextWithShadow(this.textRenderer, Text.literal("§eDatapacks"), 20, 65, 0xFFFF55);

        int maxPackVisible = Math.min(datapacks.size(), (this.height - 140) / entryHeight);
        for (int i = 0; i < maxPackVisible; i++) {
            int idx = i + scrollOffset;
            if (idx >= datapacks.size()) break;

            DatapackInfo pack = datapacks.get(idx);
            int entryY = listY + i * entryHeight;
            boolean selected = idx == selectedPackIndex;

            context.fill(20, entryY, halfWidth - 10, entryY + entryHeight - 2,
                    selected ? 0xFF993333 : 0xFF222222);

            String label = (pack.enabled() ? "§a● " : "§8○ ");
            if (pack.managed()) label += "§a[M] ";
            label += "§f" + pack.name();
            if (pack.hasDataLibDependency()) label += " §b[DL]";
            context.drawTextWithShadow(this.textRenderer, Text.literal(label), 24, entryY + 4, 0xFFFFFF);
        }

        if (currentMode == DeleteMode.MODULE) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("§bModules"), halfWidth + 10, 65, 0x55FFFF);

            int maxModVisible = Math.min(modules.size(), (this.height - 140) / entryHeight);
            for (int i = 0; i < maxModVisible; i++) {
                int idx = i + moduleScrollOffset;
                if (idx >= modules.size()) break;

                int entryY = listY + i * entryHeight;
                boolean selected = idx == selectedModuleIndex;

                context.fill(halfWidth + 10, entryY, this.width - 20, entryY + entryHeight - 2,
                        selected ? 0xFF993333 : 0xFF222222);
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal("§f" + modules.get(idx)), halfWidth + 14, entryY + 4, 0xFFFFFF);
            }

            if (modules.isEmpty() && selectedPackIndex >= 0) {
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal("§7No modules"), halfWidth + 14, listY + 4, 0x888888);
            }
        }

        if (confirmMode) {
            String target = "";
            if (currentMode == DeleteMode.MODULE && selectedModuleIndex >= 0 && selectedModuleIndex < modules.size()) {
                target = modules.get(selectedModuleIndex);
            } else if (selectedPackIndex >= 0 && selectedPackIndex < datapacks.size()) {
                target = datapacks.get(selectedPackIndex).name();
            }
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§c⚠ Click Delete again to confirm: §f" + target),
                    this.width / 2, this.height - 60, 0xFF5555);
        }

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
