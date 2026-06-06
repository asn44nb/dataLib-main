package runtoolkit.datalib.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import runtoolkit.datalib.gui.DataLibMainScreen;

/**
 * Mod Menu integration for DataLib.
 * Provides the DataLib main screen as the config screen in Mod Menu.
 */
public class DataLibModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new DataLibMainScreen(parent);
    }
}
