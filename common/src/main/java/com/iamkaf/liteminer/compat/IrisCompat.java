package com.iamkaf.liteminer.compat;

import com.iamkaf.amber.api.platform.v1.Platform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

/**
 * Optional Iris integration for Liteminer's custom world-rendering pipelines.
 */
public final class IrisCompat {
    private IrisCompat() {
    }

    public static void assignLinesPipeline(RenderPipeline pipeline) {
        if (Platform.isModLoaded("iris")) {
            IrisBridge.assignLinesPipeline(pipeline);
        }
    }

    /**
     * Kept behind a nested class so Iris API types are never resolved when Iris is absent.
     */
    private static final class IrisBridge {
        private IrisBridge() {
        }

        private static void assignLinesPipeline(RenderPipeline pipeline) {
            IrisApi.getInstance().assignPipeline(pipeline, IrisProgram.LINES);
        }
    }
}
