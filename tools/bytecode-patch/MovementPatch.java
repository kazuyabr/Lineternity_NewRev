/**
 * Helper class for movement patching.
 * Called instead of GeoEngine.canMoveToTarget() when geodata is not loaded.
 * The Object parameter absorbs the GeoEngine receiver from the original INVOKEVIRTUAL.
 */
public class MovementPatch {
    public static boolean canMoveToTarget(Object geoEngine, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (!ext.mods.Config.SISTEMA_PATHFINDING) {
            return true;
        }
        return ((ext.mods.gameserver.geoengine.GeoEngine) geoEngine).canMoveToTarget(x1, y1, z1, x2, y2, z2);
    }
}
