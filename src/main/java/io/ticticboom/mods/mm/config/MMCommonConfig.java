package io.ticticboom.mods.mm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MMCommonConfig {
    public final ModConfigSpec.BooleanValue debugTool;
    public final ModConfigSpec.BooleanValue splitRecipesJei;
    public final ModConfigSpec.BooleanValue portsAutoExtractByDefault;
    public final ModConfigSpec.IntValue structureValidationRate;
    public final ModConfigSpec.BooleanValue previewBlueprintScreen;
    public final ModConfigSpec.BooleanValue parallelProcessingDefault;
    public final ModConfigSpec.IntValue maxParallelRecipes;
    public final ModConfigSpec.BooleanValue showJeiMaxParallel;
    public final ModConfigSpec.IntValue portAutoIOInterval;
    public final ModConfigSpec.IntValue networkLinkOutputInterval;
    public final ModConfigSpec.BooleanValue networkLinkSendOnRemove;
    public final ModConfigSpec.BooleanValue networkLinkOpBypass;

    public MMCommonConfig(ModConfigSpec.Builder builder) {
        structureValidationRate = builder.comment("How often controller will check structure. 1 means every tick, 20 means every second. Default: 10")
                .defineInRange("structureValidationRate", 10, 1, 100);
        debugTool = builder.comment("Enables the Debug Tool Item's functionality (Disable when on server). Default: true")
                .define("debugTool", true);
        splitRecipesJei = builder.comment("Splits JEI recipe viewer categories by the structure they belong to. Default: true")
                .define("splitRecipesJei", true);
        portsAutoExtractByDefault = builder.comment("The default value of 'autoPush' (when not set) on ports that support automatic extract to nearby storages. Default: false")
                .define("portsAutoExtractByDefault", false);
        parallelProcessingDefault = builder.comment("The default value of 'parallelProcessing' (when not set) on structures that support parallel processing. Default: false")
                .define("parallelProcessingDefault", false);
        maxParallelRecipes = builder.comment("The max Parallel Recipes per controller. Default: 5")
                .defineInRange("maxParallelRecipes", 5, 1, 100);
        showJeiMaxParallel = builder.comment("Show 'Max Parallel Processing' line in JEI structure view. Default: true")
                .define("showJeiMaxParallel", true);
        portAutoIOInterval = builder.comment("How often ports with enabled auto push/pull sides transfer, in ticks. Default: 10")
                .defineInRange("portAutoIOInterval", 10, 1, 200);
        networkLinkOutputInterval = builder.comment("How often a machine linked to an AE2 network sends its outputs to it, in ticks. Default: 20")
                .defineInRange("networkLinkOutputInterval", 20, 1, 1200);
        networkLinkSendOnRemove = builder.comment("When a port of a linked machine is removed, send its contents to the AE2 network instead of dropping them. Default: true")
                .define("networkLinkSendOnRemove", true);
        networkLinkOpBypass = builder.comment("Operators may open and break machines linked to other players. Default: true")
                .define("networkLinkOpBypass", true);

        builder.comment("Preview features that are not yet stable or ready for use.")
                .push("preview_features");

        previewBlueprintScreen = builder.comment("Blueprint screen (JEI structure view with more fancy buttons). Default: false")
                .define("previewBlueprintScreen", false);

        builder.pop();
    }
}
