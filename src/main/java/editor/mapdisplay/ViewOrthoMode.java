
package editor.mapdisplay;

import com.jogamp.opengl.GL2;
import editor.state.MapLayerState;
import math.vec.Vec3f;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import javax.swing.SwingUtilities;

/**
 * @author Trifindo
 */
public class ViewOrthoMode extends ViewMode {

    private static boolean isSelectionMode(MapDisplay.EditMode mode) {
        return mode == MapDisplay.EditMode.MODE_SELECT
                || mode == MapDisplay.EditMode.MODE_SELECT_LASSO
                || mode == MapDisplay.EditMode.MODE_SELECT_WAND
                || mode == MapDisplay.EditMode.MODE_MOVE_SELECT;
    }

    @Override
    public void mousePressed(MapDisplay d, MouseEvent e) {
        if (d.SHIFT_PRESSED) {
            //Shift + Click with the magic wand selects all matching tiles in the map
            if (SwingUtilities.isLeftMouseButton(e)
                    && d.editMode == MapDisplay.EditMode.MODE_SELECT_WAND) {
                d.setMapSelected(e);
                d.wandSelect(e, true);
                d.repaint();
            }
            //Shift + drag starts a rectangle selection from any tool,
            //adding to the current selection like the rectangle select tool does
            else if (SwingUtilities.isLeftMouseButton(e) && !d.isPasting()
                    && !d.isFloatingMove()) {
                d.setMapSelected(e);
                d.startSelection(e, true);
                d.repaint();
            }
        } else if (d.CTRL_PRESSED && !isSelectionMode(d.editMode)) {
            //Ctrl + drag moves the camera (selection tools keep Ctrl for
            //adding to / removing from the selection)
            if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                d.lastMouseX = e.getX();
                d.lastMouseY = e.getY();
            }
        } else {
            //Right click inside a selection opens the edit menu; everywhere
            //else right click keeps its normal behaviour (e.g. tile picker)
            boolean invertedSmartDrawing = SwingUtilities.isRightMouseButton(e)
                    && ((d.editMode == MapDisplay.EditMode.MODE_EDIT && d.canStartSmartStroke())
                    || ((d.editMode == MapDisplay.EditMode.MODE_LINE
                    || d.editMode == MapDisplay.EditMode.MODE_SHAPE_RECT
                    || d.editMode == MapDisplay.EditMode.MODE_SHAPE_ELLIPSE)
                    && d.canUseSmartTools()));
            if (SwingUtilities.isRightMouseButton(e) && !d.isPasting()
                    && !invertedSmartDrawing && d.isCursorInsideSelection(e)) {
                d.showSelectionPopup(e);
                return;
            }

            switch (d.editMode) {
                case MODE_EDIT:
                    if (d.handler.getTileset().size() > 0) {
                        d.setMapSelected(e);
                        d.handler.setLayerChanged(false);
                        boolean left = SwingUtilities.isLeftMouseButton(e);
                        boolean invertedSmart = SwingUtilities.isRightMouseButton(e)
                                && d.canStartSmartStroke();
                        if (left || invertedSmart) {
                            d.dragStart = d.getCoordsInSelectedMap(e);
                            boolean smartStroke = d.canStartSmartStroke();
                            d.handler.addMapState(new MapLayerState(
                                    smartStroke ? (invertedSmart ? "Inverted Smart Draw" : "Smart Draw")
                                            : "Draw Tile", d.handler));
                            if (smartStroke) {
                                d.startSmartStroke(e, invertedSmart);
                            } else {
                                d.setTileInGrid(e);
                            }
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        } else if (SwingUtilities.isMiddleMouseButton(e)) {
                            d.floodFillTileInGrid(e);
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            d.setTileIndexFromGrid(e);
                            d.repaint();
                            d.handler.getMainFrame().updateTileSelectedID();
                            d.handler.getMainFrame().repaintTileSelector();
                            d.handler.getMainFrame().updateTileSelectorScrollBar();
                            d.handler.getMainFrame().repaintTileDisplay();
                        }
                    }
                    break;

                case MODE_CLEAR:
                    if (d.handler.getTileset().size() > 0) {
                        d.setMapSelected(e);
                        d.handler.setLayerChanged(false);
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            d.handler.addMapState(new MapLayerState("Clear Tile", d.handler));
                            d.clearTileInGrid(e);
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        } else if (SwingUtilities.isMiddleMouseButton(e)) {
                            d.floodFillClearTileInGrid(e);
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        }
                    }
                    break;

                case MODE_SMART_PAINT:
                    if (d.handler.getTileset().size() > 0) {
                        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                            d.setMapSelected(e);
                            d.handler.setLayerChanged(false);
                            d.handler.addMapState(new MapLayerState("Smart Drawing Tile", d.handler));
                            d.smartFillTileInGrid(e, false);
                            //d.disableSmartGrid();
                            d.updateActiveMapLayerGL();
                            d.repaint();

                        } else if (SwingUtilities.isRightMouseButton(e)) {

                        }
                    }
                    break;

                case MODE_INV_SMART_PAINT:
                    if (d.handler.getTileset().size() > 0) {
                        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                            d.setMapSelected(e);
                            d.handler.setLayerChanged(false);
                            d.handler.addMapState(new MapLayerState("Smart Drawing Tile", d.handler));
                            d.smartFillTileInGrid(e, true);
                            //d.disableSmartGrid();
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        } else if (SwingUtilities.isRightMouseButton(e)) {

                        }
                    }
                    break;

                case MODE_SELECT:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (d.isPasting()) {
                            d.commitPaste(e);
                        } else {
                            d.setMapSelected(e);
                            d.startSelection(e, false);
                            d.repaint();
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        if (d.isPasting()) {
                            d.cancelPaste();
                        } else if (d.hasSelection()) {
                            d.showSelectionPopup(e);
                        }
                        d.repaint();
                    }
                    break;

                case MODE_SELECT_LASSO:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        d.setMapSelected(e);
                        d.startLasso(e);
                        d.repaint();
                    } else if (SwingUtilities.isRightMouseButton(e) && d.hasSelection()) {
                        d.showSelectionPopup(e);
                    }
                    break;

                case MODE_SELECT_WAND:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        d.setMapSelected(e);
                        d.wandSelect(e, false);
                        d.repaint();
                    } else if (SwingUtilities.isRightMouseButton(e) && d.hasSelection()) {
                        d.showSelectionPopup(e);
                    }
                    break;

                case MODE_MOVE_SELECT:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (d.canBeginMoveSelection(e)) {
                            d.beginMoveSelection(e);
                            d.repaint();
                        } else if (d.hasSelection()) {
                            //Clicking outside the selection deselects
                            d.clearSelection();
                            d.repaint();
                        }
                    } else if (SwingUtilities.isRightMouseButton(e) && d.hasSelection()) {
                        d.showSelectionPopup(e);
                    }
                    break;

                case MODE_BUCKET:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        d.bucketFill(e);
                    }
                    break;

                case MODE_PICKER:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        d.pickTile(e);
                    }
                    break;

                case MODE_LINE:
                case MODE_SHAPE_RECT:
                case MODE_SHAPE_ELLIPSE:
                    boolean left = SwingUtilities.isLeftMouseButton(e);
                    boolean invertedSmart = SwingUtilities.isRightMouseButton(e)
                            && d.canUseSmartTools();
                    if (left || invertedSmart) {
                        if (d.handler.getTileset().size() > 0) {
                            d.startShape(e, invertedSmart);
                            d.repaint();
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        d.resetShape();
                        d.repaint();
                    }
                    break;

                case MODE_MOVE:
                    if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                        d.lastMouseX = e.getX();
                        d.lastMouseY = e.getY();
                    }
                    break;

                case MODE_ZOOM:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        d.orthoScale *= 1.5;
                        d.repaint();
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        d.orthoScale /= 1.5;
                        d.repaint();
                    }
                    break;
            }
        }
    }

    @Override
    public void mouseReleased(MapDisplay d, MouseEvent e) {
        //Finish a Shift-started rectangle selection made outside the select tool
        if (d.selDragActive && d.editMode != MapDisplay.EditMode.MODE_SELECT
                && SwingUtilities.isLeftMouseButton(e)) {
            d.endSelectionDrag();
            d.repaint();
            return;
        }
        switch (d.editMode) {
            case MODE_EDIT:
                if (SwingUtilities.isLeftMouseButton(e)
                        || SwingUtilities.isRightMouseButton(e)) {
                    d.finishSmartStroke();
                }
                break;

            case MODE_CLEAR:
                d.handler.getMapMatrix().removeUnusedMaps();
                if (!d.handler.mapSelectedExists()) {
                    d.handler.setDefaultMapSelected();

                    d.handler.getMainFrame().getThumbnailLayerSelector().drawAllLayerThumbnails();
                    d.handler.getMainFrame().getThumbnailLayerSelector().repaint();
                }
                break;

            case MODE_SELECT:
                //A plain click without dragging deselects (Paint.net behavior),
                //but a Shift click adds the clicked tile instead
                if (SwingUtilities.isLeftMouseButton(e) && !d.isPasting()
                        && d.resizeHandle == 0 && !d.selDragMoved && !d.selStartedAdditive
                        && d.hasSelection()) {
                    d.clearSelection();
                    d.repaint();
                }
                d.endSelectionDrag();
                break;

            case MODE_SELECT_LASSO:
                if (SwingUtilities.isLeftMouseButton(e)) {
                    d.endLasso();
                    d.repaint();
                }
                break;

            case MODE_MOVE_SELECT:
                if (SwingUtilities.isLeftMouseButton(e) && d.isFloatingMove()) {
                    d.commitMoveSelection(e);
                }
                break;

            case MODE_LINE:
                if (SwingUtilities.isLeftMouseButton(e)
                        || (SwingUtilities.isRightMouseButton(e) && d.smartShapeInverted)) {
                    d.commitLine();
                }
                break;

            case MODE_SHAPE_RECT:
                if (SwingUtilities.isLeftMouseButton(e)
                        || (SwingUtilities.isRightMouseButton(e) && d.smartShapeInverted)) {
                    d.commitRectShape();
                }
                break;

            case MODE_SHAPE_ELLIPSE:
                if (SwingUtilities.isLeftMouseButton(e)
                        || (SwingUtilities.isRightMouseButton(e) && d.smartShapeInverted)) {
                    d.commitEllipseShape();
                }
                break;
        }
        d.handler.updateLayerThumbnail(d.handler.getActiveLayerIndex());
        d.handler.repaintThumbnailLayerSelector();

        d.editedMapCoords.add(d.handler.getMapSelected());
        d.handler.updateMapThumbnails(d.editedMapCoords);
        d.editedMapCoords = new HashSet<>();

        d.handler.getMainFrame().updateMapMatrixDisplay();

        d.handler.getMainFrame().updateViewGeometryCount();
    }

    @Override
    public void mouseDragged(MapDisplay d, MouseEvent e) {
        d.updateMousePostion(e);
        if (d.SHIFT_PRESSED) {
            //A Shift selection drag keeps extending the selection
            if (SwingUtilities.isLeftMouseButton(e)
                    && d.selDragActive && !d.isPasting()) {
                d.updateSelection(e);
                d.repaint();
            }
        } else if (d.CTRL_PRESSED && !isSelectionMode(d.editMode) && !d.selDragActive) {
            if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                d.moveCamera(e);
                d.repaint();
            }
        } else {
            //Keep extending a Shift-started selection even if Shift was released mid-drag
            if (SwingUtilities.isLeftMouseButton(e) && d.selDragActive
                    && d.editMode != MapDisplay.EditMode.MODE_SELECT && !d.isPasting()) {
                d.updateSelection(e);
                d.repaint();
                return;
            }
            switch (d.editMode) {
                case MODE_EDIT:
                    if (d.handler.getTileset().size() > 0) {
                        if (SwingUtilities.isLeftMouseButton(e)
                                || (SwingUtilities.isRightMouseButton(e)
                                && d.smartStrokeMap != null)) {
                            if (d.smartStrokeMap != null) {
                                d.editedMapCoords.add(d.smartStrokeMap);
                                d.extendSmartStroke(e);
                            } else {
                                d.setMapSelected(e);
                                d.editedMapCoords.add(d.getMapCoords(e));
                                d.dragTileInGrid(e);
                            }
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        }
                    }
                    break;

                case MODE_CLEAR:
                    if (d.handler.getTileset().size() > 0) {
                        d.setMapSelected(e);
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            //d.updateLastMapState();
                            d.editedMapCoords.add(d.getMapCoords(e));
                            d.clearTileInGrid(e);
                            d.updateActiveMapLayerGL();
                            d.repaint();
                        }
                    }
                    break;

                case MODE_SELECT:
                    if (SwingUtilities.isLeftMouseButton(e) && !d.isPasting()) {
                        d.updateSelection(e);
                        d.repaint();
                    }
                    break;

                case MODE_SELECT_LASSO:
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        d.updateLasso(e);
                        d.repaint();
                    }
                    break;

                case MODE_MOVE_SELECT:
                    if (SwingUtilities.isLeftMouseButton(e) && d.isFloatingMove()) {
                        d.repaint();
                    }
                    break;

                case MODE_LINE:
                case MODE_SHAPE_RECT:
                case MODE_SHAPE_ELLIPSE:
                    if (SwingUtilities.isLeftMouseButton(e)
                            || (SwingUtilities.isRightMouseButton(e) && d.smartShapeInverted)) {
                        d.updateShape(e);
                        d.repaint();
                    }
                    break;

                case MODE_MOVE:
                    if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                        d.moveCamera(e);
                        d.repaint();
                    }
                    break;
            }
        }
    }

    @Override
    public void mouseMoved(MapDisplay d, MouseEvent e) {
        d.updateMousePostion(e);
        d.updateCursorTileCoordsStatus(e);
        if (d.editMode == MapDisplay.EditMode.MODE_SELECT && !d.isPasting()) {
            d.updateSelectModeCursor(e);
        }
        d.repaint();
    }

    @Override
    public void keyPressed(MapDisplay d, KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                d.set3DView();
                d.repaint();
                break;
            case KeyEvent.VK_H:
                d.setHeightView();
                d.repaint();
                break;
            case KeyEvent.VK_C:
                if (!e.isControlDown()) {
                    d.toggleClearTile();
                    d.repaint();
                }
                break;
            case KeyEvent.VK_S:
                if (!e.isControlDown()) {
                    d.toggleSmartGrid();
                    d.repaint();
                }
                break;
            case KeyEvent.VK_RIGHT:
                d.setCameraAtNextMapAndSelect(new Point(1, 0));
                d.repaint();
                break;
            case KeyEvent.VK_LEFT:
                d.setCameraAtNextMapAndSelect(new Point(-1, 0));
                d.repaint();
                break;
            case KeyEvent.VK_UP:
                d.setCameraAtNextMapAndSelect(new Point(0, -1));
                d.repaint();
                break;
            case KeyEvent.VK_DOWN:
                d.setCameraAtNextMapAndSelect(new Point(0, 1));
                d.repaint();
                break;
        }
    }

    @Override
    public void keyReleased(MapDisplay d, KeyEvent e) {

    }

    @Override
    public void mouseWheelMoved(MapDisplay d, MouseWheelEvent e) {
        if (d.CTRL_PRESSED) {
            d.zoomCameraOrtho(e);
            d.repaint();
        } else {
            switch (d.editMode) {
                case MODE_EDIT:
                    int delta = e.getWheelRotation() > 0 ? 1 : -1;
                    d.handler.incrementTileSelected(delta);
                    d.handler.getMainFrame().updateTileSelectedID();
                    d.handler.getMainFrame().repaintTileSelector();
                    d.handler.getMainFrame().repaintTileDisplay();
                    d.repaint();
                    break;
                case MODE_MOVE:
                    d.zoomCameraOrtho(e);
                    d.repaint();
                    break;
                case MODE_ZOOM:
                    d.zoomCameraOrtho(e);
                    d.repaint();
                    break;
            }
        }
    }

    @Override
    public void paintComponent(MapDisplay d, Graphics g) {
        if (d.handler != null) {
            Graphics2D g2d = (Graphics2D) g;

            AffineTransform transform = g2d.getTransform();
            d.applyGraphicsTransform(g2d);

            if (d.backImageEnabled && d.backImage != null) {
                d.drawBackImage(g);
            }

            switch (d.editMode) {
                case MODE_EDIT:
                    //Shift means a selection is being made, not a draw:
                    //hide the tile preview and leave only the pointer
                    if (!d.SHIFT_PRESSED) {
                        d.drawTileThumbnail(g);
                    }
                    break;
                case MODE_CLEAR:
                    d.drawUnitTileBounds(g);
                    break;
                case MODE_SMART_PAINT:
                    d.drawUnitTileBounds(g);
                    break;
                case MODE_INV_SMART_PAINT:
                    d.drawUnitTileBounds(g);
                    break;
                case MODE_SELECT:
                    if (!d.isPasting()) {
                        d.drawUnitTileBounds(g);
                    }
                    break;
                case MODE_SELECT_LASSO:
                case MODE_SELECT_WAND:
                case MODE_BUCKET:
                case MODE_PICKER:
                    d.drawUnitTileBounds(g);
                    break;
                case MODE_LINE:
                case MODE_SHAPE_RECT:
                case MODE_SHAPE_ELLIPSE:
                    if (d.shapeMap == null) {
                        d.drawUnitTileBounds(g);
                    } else {
                        d.drawShapePreview(g);
                    }
                    break;
            }

            if (!d.isFloatingMove()) {
                d.drawSelectionOverlay(g);
            }
            if (d.isPasting()) {
                d.drawPastePreview(g);
            }
            if (d.isFloatingMove()) {
                d.drawFloatingMovePreview(g);
            }

            g.setColor(Color.white);
            d.drawAllMapBounds(g);

            if (d.drawAreasEnabled) {
                d.drawAllMapContours(g);
            }

            g.setColor(Color.white);
            d.drawBorderBounds(g,
                    d.handler.getMapSelected().x * d.cols * d.tileSize,
                    d.handler.getMapSelected().y * d.rows * d.tileSize, 1);

            g.setColor(Color.red);
            d.drawBorderBounds(g,
                    d.handler.getMapSelected().x * d.cols * d.tileSize,
                    d.handler.getMapSelected().y * d.rows * d.tileSize, 4);

            g2d.setTransform(transform);
        }
    }

    @Override
    public void applyCameraTransform(MapDisplay d, GL2 gl) {
        float v = (16.0f + d.borderSize) / d.orthoScale;
        gl.glOrtho(-v, v, -v, v, -100.0f, 100.0f);
        //TODO: Use this code for keeping the aspect ratio
        //float aspect = d.getAspectRatio();
        //gl.glOrtho(-v * aspect, v * aspect, -v, v, -100.0f, 100.0f);
    }

    @Override
    public void setCameraAtMap(MapDisplay d) {
        d.orthoScale = 1.0f;
    }

    @Override
    public ViewID getViewID() {
        return ViewID.VIEW_ORTHO;
    }

    @Override
    public float getZNear(MapDisplay d) {
        return -100.0f;
    }

    @Override
    public float getZFar(MapDisplay d) {
        return 100.0f;
    }

    public Vec3f[][] getFrustumPlanes(MapDisplay d) {
        Vec3f camAngles = new Vec3f(d.cameraRotX, d.cameraRotY, d.cameraRotZ);
        Vec3f tarPos = new Vec3f(d.cameraX, d.cameraY, 0.0f);
        Vec3f camDir = d.rotToDir_(camAngles);
        Vec3f camUp = d.rotToUp_(camAngles);
        Vec3f camRight = camDir.cross_(camUp);
        //Vec3f camPos = tarPos.add_(camDir.negate_().scale_(d.cameraZ));
        Vec3f camPos = tarPos.add_(camDir.negate_().scale_(40.0f));

        float zNear = getZNear(d);
        float zFar = getZFar(d);

        float v = (16.0f + d.borderSize) / d.orthoScale;
        float hNear = 2 * v;
        float wNear = hNear * d.getAspectRatio();

        float hFar = 2 * v;
        float wFar = hFar * d.getAspectRatio();

        //Far plane points
        Vec3f fc = camDir.scale_(zFar).add(camPos);
        Vec3f ftl = fc.add_(camUp.scale_(hFar / 2.0f)).sub(camRight.scale_(wFar / 2.0f));
        Vec3f ftr = fc.add_(camUp.scale_(hFar / 2.0f)).add(camRight.scale_(wFar / 2.0f));
        Vec3f fbl = fc.sub_(camUp.scale_(hFar / 2.0f)).sub(camRight.scale_(wFar / 2.0f));
        Vec3f fbr = fc.sub_(camUp.scale_(hFar / 2.0f)).add(camRight.scale_(wFar / 2.0f));

        //Near plane points
        Vec3f nc = camDir.scale_(zNear).add(camPos);
        Vec3f ntl = nc.add_(camUp.scale_(hNear / 2.0f)).sub(camRight.scale_(wNear / 2.0f));
        Vec3f ntr = nc.add_(camUp.scale_(hNear / 2.0f)).add(camRight.scale_(wNear / 2.0f));
        Vec3f nbl = nc.sub_(camUp.scale_(hNear / 2.0f)).sub(camRight.scale_(wNear / 2.0f));
        Vec3f nbr = nc.sub_(camUp.scale_(hNear / 2.0f)).add(camRight.scale_(wNear / 2.0f));

        //Return frustum planes defined by 3 points
        return new Vec3f[][]{
                {ntr, ntl, ftl},
                {nbl, nbr, fbr},
                {ntl, nbl, fbl},
                {nbr, ntr, fbr},
                {ntl, ntr, nbr},
                {ftr, ftl, fbl}
        };
    }
}
