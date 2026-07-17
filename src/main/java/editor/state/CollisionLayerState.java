
package editor.state;

import formats.collisions.CollisionHandler;

/**
 * @author Trifindo
 */
public class CollisionLayerState extends State {

    private CollisionHandler collisionHandler;
    private int layerIndex;
    private byte[][] layer;

    public CollisionLayerState(String name, CollisionHandler collisionHandler) {
        this(name, collisionHandler, collisionHandler.getIndexLayerSelected());
    }

    public CollisionLayerState(String name, CollisionHandler collisionHandler, int layerIndex) {
        super(name);
        this.collisionHandler = collisionHandler;

        this.layerIndex = layerIndex;
        layer = collisionHandler.cloneLayer(layerIndex);
    }

    @Override
    public void revertState() {
        collisionHandler.setLayer(layerIndex, layer);
    }

    public int getLayerIndex() {
        return layerIndex;
    }


}
