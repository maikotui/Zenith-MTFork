package safro.apotheosis.village;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import safro.apotheosis.village.fletching.FletchingScreen;
import safro.apotheosis.village.fletching.arrows.*;

public class VillageModuleClient {

    public static void init() {
        ScreenRegistry.register(VillageModule.FLETCHING_MENU, FletchingScreen::new);

        EntityRendererRegistry.register(VillageModule.BROADHEAD, BroadheadArrowRenderer::new);
        EntityRendererRegistry.register(VillageModule.OBSIDIAN, ObsidianArrowRenderer::new);
        EntityRendererRegistry.register(VillageModule.EXPLOSIVE, ExplosiveArrowRenderer::new);
        EntityRendererRegistry.register(VillageModule.MINING, MiningArrowRenderer::new);
    }
}
