package runtoolkit.datalib.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import runtoolkit.datalib.config.DataLibConfig;
import runtoolkit.datalib.core.DataLibCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-game configuration screen for DataLib.
 * Also used as the Mod Menu config screen.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;
    private List<Map.Entry<String, String>> entries = new ArrayList<>();
    private int selectedIndex = -1;
    private TextFieldWidget valueField;

    public ConfigScreen(Screen parent) {
        super(Text.literal("DataLib Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        DataLibConfig config = DataLibCore.getInstance().getConfig();
        entries = new ArrayList<>(config.getAll().entrySet());

        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        valueField = new TextFieldWidget(this.textRenderer, centerX - 100, bottomY - 50, 200, 20, Text.literal("Value"));
        valueField.setPlaceholder(Text.literal("§7Enter new value..."));
        valueField.setMaxLength(256);
        this.addDrawableChild(valueField);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✔ Save"),
                btn -> onSave()
        ).dimensions(centerX - 105, bottomY - 22, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cBack"),
                btn -> this.close()
        ).dimensions(centerX + 5, bottomY - 22, 100, 20).build());
    }

    private void onSave() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return;

        String key = entries.get(selectedIndex).getKey();
        String value = valueField.getText().trim();
        if (value.isEmpty()) return;

        DataLibConfig config = DataLibCore.getInstance().getConfig();
        config.set(key, value);

        entries = new ArrayList<>(config.getAll().entrySet());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = this.width / 2 - 150;
        int listY = 55;
        int entryHeight = 22;

        for (int i = 0; i < entries.size(); i++) {
            int entryY = listY + i * entryHeight;
            if (mouseX >= listX && mouseX <= listX + 300 && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                selectedIndex = i;
                valueField.setText(entries.get(i).getValue());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7§lDataLib Configuration"), this.width / 2, 15, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Click a setting to edit its value"), this.width / 2, 30, 0xFF888888);

        int listX = this.width / 2 - 150;
        int listY = 55;
        int entryHeight = 22;

        context.drawTextWithShadow(this.textRenderer, Text.literal("§eKey"), listX, 44, 0xFFFFFF55);
        context.drawTextWithShadow(this.textRenderer, Text.literal("§bValue"), listX + 160, 44, 0xFF55FFFF);

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, String> entry = entries.get(i);
            int entryY = listY + i * entryHeight;
            boolean selected = i == selectedIndex;

            context.fill(listX, entryY, listX + 300, entryY + entryHeight - 2,
                    selected ? 0xFF335599 : 0xFF222222);

            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("§f" + entry.getKey()), listX + 4, entryY + 5, 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("§b" + entry.getValue()), listX + 164, entryY + 5, 0xFF55FFFF);
        }

        context.drawTextWithShadow(this.textRenderer, Text.literal("§fNew Value:"),
                this.width / 2 - 100, this.height - 102, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
