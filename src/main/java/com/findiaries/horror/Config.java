package com.findiaries.horror;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MIN_SECONDS = B
            .comment("Minimum pause (seconds) between random horror events in the overworld.")
            .defineInRange("eventMinSeconds", 150, 5, 36000);
    public static final ForgeConfigSpec.IntValue MAX_SECONDS = B
            .comment("Maximum pause (seconds) between random horror events in the overworld.")
            .defineInRange("eventMaxSeconds", 480, 5, 72000);

    public static final ForgeConfigSpec SPEC = B.build();

    private Config() {
    }
}
