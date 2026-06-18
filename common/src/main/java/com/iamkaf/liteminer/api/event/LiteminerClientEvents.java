package com.iamkaf.liteminer.api.event;

import com.iamkaf.amber.api.event.v1.Event;
import com.iamkaf.amber.api.event.v1.EventFactory;

/**
 * Client-side Liteminer events for HUD and presentation integrations.
 */
public final class LiteminerClientEvents {
    private LiteminerClientEvents() {
    }

    /**
     * Fired before Liteminer's default HUD lines are rendered.
     *
     * <p>Callbacks may hide the HUD, edit the rendered lines, or change basic text layout
     * through the provided {@link LiteminerHudContext}.</p>
     */
    public static final Event<ModifyHud> MODIFY_HUD = EventFactory.createArrayBacked(
            ModifyHud.class,
            callbacks -> context -> {
                for (ModifyHud callback : callbacks) {
                    callback.modifyHud(context);
                }
            }
    );

    /**
     * Callback for modifying Liteminer's HUD.
     */
    @FunctionalInterface
    public interface ModifyHud {
        /**
         * Modifies the HUD context before rendering.
         *
         * @param context mutable HUD context for the current frame
         */
        void modifyHud(LiteminerHudContext context);
    }
}
