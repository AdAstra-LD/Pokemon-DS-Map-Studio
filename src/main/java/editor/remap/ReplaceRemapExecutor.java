package editor.remap;

import java.util.function.BooleanSupplier;

public final class ReplaceRemapExecutor {

    private ReplaceRemapExecutor() {
    }

    public static ReplaceRemapState apply(ReplaceRemapPlan plan, RemapProjectAccess access,
                                          BooleanSupplier cancelled) {
        ReplaceRemapState before = ReplaceRemapState.capture("Replace and Remap", access,
                plan.getAffectedMaps(), plan.getAffectedTerrainLayers(),
                plan.getAffectedCollisionLayers());
        try {
            plan.apply(access, cancelled);
            return before;
        } catch (RuntimeException exception) {
            before.revertState();
            throw exception;
        }
    }
}
