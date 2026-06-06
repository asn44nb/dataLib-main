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
 * Screen for adding DataLib dependency to a datapack.
 * User selects a datapack from the list, confirms, then progress bar runs.
 * After completion, returns to main menu.
 */
public class AddDependencyScreen extends Screen {

    private final Screen parent;
    private final ProgressOverlay progressOverlay = new ProgressOverlay();
    private boolean processing = false;
    private List<DatapackInfo> datapacks = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean confirmMode = false;

    public AddDependencyScreen(Screen parent) {
        super(Text.literal("Add DataLib Dependency"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        loadDatapacks();

        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cBack"),
                btn -> this.close()
        ).dimensions(centerX - 105, bottomY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✔ Add Dependency"),
                btn -> onConfirm()
        ).dimensions(centerX + 5, bottomY, 100, 20).build());
    }

    private void loadDatapacks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            datapacks = DatapackGenerator.listDatapacks(client.getServer());
        }
    }

    private void onConfirm() {
        if (processing || selectedIndex < 0 || selectedIndex >= datapacks.size()) return;

        DatapackInfo selected = datapacks.get(selectedIndex);

        if (!confirmMode) {
            confirmMode = true;
            return;
        }

        processing = true;
        progressOverlay.reset();
        confirmMode = false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return;

        Thread worker = new Thread(() -> {
            DatapackGenerator.addDataLibDependency(
                    client.getServer(), selected.name(),
                    (progress, message) -> client.execute(() -> progressOverlay.update(progress, message))
            );
        }, "DataLib-AddDep");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!processing) {
            int listX = this.width / 2 - 120;
            int listY = 60;
            int entryHeight = 22;
            int maxVisible = Math.min(datapacks.size(), (this.height - 120) / entryHeight);

            for (int i = 0; i < maxVisible; i++) {
                int idx = i + scrollOffset;
                if (idx >= datapacks.size()) break;

                int entryY = listY + i * entryHeight;
                if (mouseX >= listX && mouseX <= listX + 240 && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    selectedIndex = idx;
                    confirmMode = false;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int entryHeight = 22;
        int maxVisible = (this.height - 120) / entryHeight;
        if (verticalAmount > 0 && scrollOffset > 0) scrollOffset--;
        if (verticalAmount < 0 && scrollOffset < datapacks.size() - maxVisible) scrollOffset++;
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§e§lAdd DataLib Dependency"), this.width / 2, 15, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Select a datapack and confirm to add datalib:engine dependency"),
                this.width / 2, 30, 0xFF888888);

        int listX = this.width / 2 - 120;
        int listY = 60;
        int entryHeight = 22;
        int maxVisible = Math.min(datapacks.size(), (this.height - 120) / entryHeight);

        if (datapacks.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§7No datapacks found. Create one first!"), this.width / 2, listY + 20, 0xFFAAAAAA);
        }

        for (int i = 0; i < maxVisible; i++) {
            int idx = i + scrollOffset;
            if (idx >= datapacks.size()) break;

            DatapackInfo pack = datapacks.get(idx);
            int entryY = listY + i * entryHeight;
            boolean selected = idx == selectedIndex;

            context.fill(listX, entryY, listX + 240, entryY + entryHeight - 2,
                    selected ? 0xFF335599 : 0xFF222222);

            String label = (pack.enabled() ? "§a● " : "§8○ ");
            label += (pack.managed() ? "§a[M] " : "§7[U] ") + "§f" + pack.name();
            if (pack.hasDataLibDependency()) label += " §b[DL]";
            context.drawTextWithShadow(this.textRenderer, Text.literal(label), listX + 4, entryY + 5, 0xFFFFFFFF);
        }

        if (confirmMode && selectedIndex >= 0 && selectedIndex < datapacks.size()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§eClick 'Add Dependency' again to confirm for: §b" + datapacks.get(selectedIndex).name()),
                    this.width / 2, this.height - 60, 0xFFFFFF55);
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
