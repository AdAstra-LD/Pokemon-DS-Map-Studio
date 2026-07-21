
package editor.mapdisplay;

import com.jogamp.common.nio.Buffers;
import editor.handler.MapEditorHandler;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FRONT_AND_BACK;
import static com.jogamp.opengl.GL.GL_GREATER;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LESS;
import static com.jogamp.opengl.GL.GL_LINEAR_MIPMAP_LINEAR;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_NOTEQUAL;
import static com.jogamp.opengl.GL.GL_NO_ERROR;
import static com.jogamp.opengl.GL.GL_ONE;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_DST_ALPHA;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_REPEAT;
import static com.jogamp.opengl.GL.GL_RGBA;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;

import com.jogamp.opengl.GL2;

import static com.jogamp.opengl.GL2ES1.GL_ALPHA_TEST;
import static com.jogamp.opengl.GL2ES3.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES3.GL_QUADS;
import static com.jogamp.opengl.GL2GL3.GL_FILL;
import static com.jogamp.opengl.GL2GL3.GL_LINE;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import editor.bordermap.BorderMapsGrid;
import editor.grid.GeometryGL;
import editor.grid.MapGrid;
import editor.grid.MapLayerGL;
import editor.handler.MapData;
import editor.smartdrawing.SmartGrid;
import editor.state.MapLayerState;
import formats.collisions.CollisionDefaultsApplier;
import geometry.Generator;
import graphicslib3D.Matrix3D;
import graphicslib3D.Vector3D;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

import math.mat.Mat4f;
import math.transf.TransfMat;
import math.vec.Vec3f;
import math.vec.Vec4f;
import tileset.Tile;
import utils.ImageTiler;
import utils.Utils;

/**
 * @author Trifindo
 */
public class MapDisplay extends GLJPanel implements GLEventListener, MouseListener, MouseMotionListener, KeyListener, MouseWheelListener {

    //Editor Handler
    private boolean mouseWheelEnabled = true;
    protected MapEditorHandler handler;

    //Grid
    protected final int cols = 32;
    protected final int rows = 32;
    protected final int tileSize = 16;
    protected final int borderSize = 1;
    protected final int width = (cols + borderSize * 2) * tileSize;
    protected final int height = (rows + borderSize * 2) * tileSize;
    protected final float gridTileSize = 1.0f;
    protected float orthoScale = 1.0f;

    //OpenGL
    protected GLU glu;
    protected float[] grid;
    protected FloatBuffer gridBuffer;
    protected float[] axis;
    protected final float[] axisColors = {
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};

    //Scene
    protected float cameraX, cameraY, cameraZ;
    protected float targetX, targetY, targetZ;
    protected float cameraRotX, cameraRotY, cameraRotZ;
    protected static final float defaultCamRotX = 40.0f, defaultCamRotY = 0.0f, defaultCamRotZ = 0.0f;
    protected Mat4f camModelView = new Mat4f();
    protected Mat4f camProj = new Mat4f();
    protected Mat4f camMVP = new Mat4f();
    protected final Vec4f[] cornerDeltas = new Vec4f[]{
            new Vec4f(-MapGrid.cols / 2, -MapGrid.rows / 2, 0.0f, 0.0f),
            new Vec4f(+MapGrid.cols / 2, -MapGrid.rows / 2, 0.0f, 0.0f),
            new Vec4f(+MapGrid.cols / 2, +MapGrid.rows / 2, 0.0f, 0.0f),
            new Vec4f(-MapGrid.cols / 2, +MapGrid.rows / 2, 0.0f, 0.0f)
    };
    protected final float fovDeg = 60.0f;
    //protected HashMap<Point, MapData> filteredMaps;
    //protected Vec3f[][] frustum;
    //protected final float zNear = 1.0f;
    //protected final float zFar = 1000.0f;

    //Scene displays
    protected boolean drawGridEnabled = true;
    protected boolean drawWireframeEnabled = false;
    protected boolean drawAreasEnabled = true;
    protected boolean drawGridBorderMaps = true;

    //Mouse events
    protected int lastMouseX, lastMouseY;
    protected int xMouse, yMouse;
    protected Point dragStart = new Point();
    protected Set<Point> editedMapCoords = new HashSet<>();

    //Keyboard events
    protected boolean CONTROL_PRESSED = false;
    protected boolean SHIFT_PRESSED = false;
    protected boolean CTRL_PRESSED = false;

    //Height map
    protected float heightMapOpacity = 1.0f;

    //Update
    protected boolean updateRequested = false;

    //Screenshot
    protected BufferedImage screenshot;
    protected boolean screenshotRequested = false;

    //Background Image
    protected BufferedImage backImage = null;
    protected boolean backImageEnabled = false;
    protected float backImageAlpha = 0.5f;

    //View Modes
    protected ViewMode viewMode = ViewMode.VIEW_ORTHO_MODE;

    //Edit Modes
    public static enum EditMode {

        MODE_EDIT(new Cursor(Cursor.DEFAULT_CURSOR)),
        MODE_MOVE(new Cursor(Cursor.MOVE_CURSOR)),
        MODE_ZOOM(Utils.loadCursor("/cursors/zoomCursor.png")),
        MODE_CLEAR(Utils.loadCursor("/cursors/clearTileCursor.png")),
        MODE_SMART_PAINT(Utils.loadCursor("/cursors/smartGridCursor.png")),
        MODE_INV_SMART_PAINT(Utils.loadCursor("/cursors/smartGridInvertedCursor.png")),
        MODE_SELECT(new Cursor(Cursor.CROSSHAIR_CURSOR)),
        MODE_SELECT_LASSO(new Cursor(Cursor.CROSSHAIR_CURSOR)),
        MODE_SELECT_WAND(new Cursor(Cursor.CROSSHAIR_CURSOR)),
        MODE_MOVE_SELECT(new Cursor(Cursor.MOVE_CURSOR)),
        MODE_BUCKET(Utils.loadCursor("/cursors/floodFillIcon.png")),
        MODE_PICKER(Utils.loadCursor("/cursors/grabColorCursor.png")),
        MODE_LINE(new Cursor(Cursor.CROSSHAIR_CURSOR)),
        MODE_SHAPE_RECT(new Cursor(Cursor.CROSSHAIR_CURSOR)),
        MODE_SHAPE_ELLIPSE(new Cursor(Cursor.CROSSHAIR_CURSOR));

        public final Cursor cursor;

        private EditMode(Cursor cursor) {
            this.cursor = cursor;
        }

    }

    ;
    protected EditMode editMode = EditMode.MODE_EDIT;

    //Region selection (select / lasso / wand / move tools)
    protected MapSelection selection = null;
    protected boolean pasting = false;

    //Rectangle selection drag / resize state
    protected Point selDragStart = null;
    protected boolean selDragMoved = false;
    protected boolean selDragActive = false;
    protected boolean selAdditive = false;
    protected boolean selStartedAdditive = false;
    protected boolean[][] selAddBase = null;
    protected static final int HANDLE_LEFT = 1, HANDLE_RIGHT = 2, HANDLE_BOTTOM = 4, HANDLE_TOP = 8;
    protected int resizeHandle = 0;
    protected Rectangle resizeBaseBounds = null;

    //Lasso state
    protected Point lassoFirst = null;
    protected Point lassoLast = null;

    //Floating region (move selection tool)
    protected boolean floatingMove = false;
    protected int[][] floatTiles = null;
    protected int[][] floatHeights = null;
    protected boolean[][] floatMask = null;
    protected Point floatGrabOffset = null;   //x: cells right of region left edge, y: cells below region top edge
    protected Point floatHomeMap = null;
    protected Point floatHomeTopLeft = null;  //grid coords (x = left col, y = top row)

    //Line / rectangle shape tools
    protected Point shapeStart = null;
    protected Point shapeEnd = null;
    protected Point shapeMap = null;
    protected Point smartStrokeMap = null;
    protected Point smartStrokeLast = null;
    protected ArrayList<Point> smartStrokeCells = null;
    protected int[][] smartStrokeBaseLayer = null;
    protected int smartStrokeLayer = -1;
    protected int smartStrokeStartTile = -1;
    protected boolean smartStrokeInverted = false;
    protected boolean smartShapeInverted = false;
    protected boolean smartToolsEnabled = false;
    protected boolean autoCollisionEnabled = false;

    //Selection context menu
    protected javax.swing.JPopupMenu selectionPopupMenu = null;

    public MapDisplay() {
        //Set default display size
        setPreferredSize(new Dimension(width, height));
        setSize(new Dimension(width, height));

        //Add listeners
        addGLEventListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        addMouseWheelListener(this);

        //Set focusable for keyListener
        setFocusable(true);


        //Create custom cursors
        //smartGridCursor = Utils.loadCursor("/cursors/smartGridCursor.png");
        //smartGridInvertedCursor = Utils.loadCursor("/cursors/smartGridInvertedCursor.png");
        //clearTileCursor = Utils.loadCursor("/cursors/clearTileCursor.png");
    }

    public boolean isMouseWheelEnabled() {
        return mouseWheelEnabled;
    }

    public void setMouseWheelEnabled(boolean mouseWheelEnabled) {
        this.mouseWheelEnabled = mouseWheelEnabled;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        glu = new GLU();

        grid = Generator.generateCenteredGrid(cols, rows, gridTileSize, 0.02f);
        gridBuffer = Buffers.newDirectFloatBuffer(grid);
        axis = Generator.generateAxis(100.0f);

        //Load textures into OpenGL
        handler.getTileset().loadTexturesGL();
        handler.getBorderMapsTileset().loadTexturesGL();

        drawable.getGL().getGL2().glClearColor(0.0f, 0.5f, 0.5f, 1.0f);

        //Scene
        cameraX = 0.0f;
        cameraY = 0.0f;
        cameraZ = 32.0f;

        cameraRotX = 0.0f;
        cameraRotY = 0.0f;
        cameraRotZ = 0.0f;

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("Dispose!! ");
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        renderMapScene(drawable.getGL().getGL2());
    }

    private void renderMapScene(GL2 gl) {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        //gl.glLoadIdentity();
        //lighting(gl);

        applyCameraTransform(gl);

        if (updateRequested) {

            //Reload textures without leaving the old GL objects allocated.
            handler.getTileset().updateTextures(gl);
            handler.getBorderMapsTileset().updateTextures(gl);

            updateMapLayersGL();

            updateRequested = false;
        }

        try {
            //Update view frustum and filter maps
            Vec3f[][] frustum = viewMode.getFrustumPlanes(this);
            HashMap<Point, MapData> filteredMaps = getMapsInsideFrustum(frustum);

            //gl.glEnable(GL2.GL_LIGHTING);

            //Draw opaque tiles
            if (handler.getTileset().size() > 0) {
                drawOpaqueMaps(gl, filteredMaps);
            }

            //Draw semitransparent tiles
            if (handler.getTileset().size() > 0) {
                drawTransparentMaps(gl, filteredMaps);
            }

            //gl.glDisable(GL2.GL_LIGHTING);

            //Draw grid
            if (drawGridEnabled) {
                drawGridMaps(gl, filteredMaps);
            }

            //Draw axis
            drawAxis();

            if (drawWireframeEnabled) {
                if (handler.getTileset().size() > 0) {
                    drawWireframeMaps(gl, filteredMaps);
                }
            }

            if (drawGridBorderMaps) {
                HashSet<Point> filteredGridBorderMaps = getGridBorderMapsInsideFrustum(frustum);
                drawGridBorderMaps(gl, filteredGridBorderMaps);
            }

            if (drawAreasEnabled) {
                drawAllContourLines(gl);
            }

            //Screenshot
            if (screenshotRequested) {
                drawScreenshot(gl);
                screenshotRequested = false;
            }

        } catch (GLException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        viewMode.mousePressed(this, e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        viewMode.mouseReleased(this, e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        viewMode.mouseDragged(this, e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        viewMode.mouseMoved(this, e);

        if (!hasFocus()) {
            requestFocusInWindow();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        viewMode.keyPressed(this, e);

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                if (floatingMove) {
                    cancelFloatingMove();
                } else if (pasting) {
                    cancelPaste();
                } else if (hasSelection()) {
                    clearSelection();
                } else {
                    setEditMode(EditMode.MODE_EDIT);
                    handler.getMainFrame().getJtbModeEdit().setSelected(true);
                }
                repaint();
                break;
            case KeyEvent.VK_M:
                if (isOrthoView()) {
                    toggleSelectMode();
                    repaint();
                }
                break;
            case KeyEvent.VK_SHIFT:
                SHIFT_PRESSED = true;
                break;
            case KeyEvent.VK_CONTROL:
                CTRL_PRESSED = true;
                break;
            case KeyEvent.VK_E:
                setEditMode(EditMode.MODE_EDIT);
                break;
            case KeyEvent.VK_G:
                toggleGridView();
                handler.getMainFrame().getJtbViewGrid().setSelected(drawGridEnabled);
                repaint();
                break;
            case KeyEvent.VK_W:
                drawWireframeEnabled = !drawWireframeEnabled;
                handler.getMainFrame().getJtbViewWireframe().setSelected(drawWireframeEnabled);
                repaint();
                break;
            case KeyEvent.VK_A:
                if (!e.isControlDown()) {
                    drawAreasEnabled = !drawAreasEnabled;
                    handler.getMainFrame().getJcbViewAreas().setSelected(drawAreasEnabled);
                    repaint();
                }
                break;
            case KeyEvent.VK_F:
                if (!e.isControlDown()) { //Ctrl+F is the Fill Selection accelerator
                    setCameraAtSelectedMap();
                    repaint();
                }
                break;
            case KeyEvent.VK_Q://TODO: DELETE THIS
                BufferedImage img = Utils.getImageFromClipboard();
                if (img != null) {
                    handler.addMapState(new MapLayerState("Import map as image", handler));
                    handler.getGrid().setTileLayer(handler.getActiveLayerIndex(),
                            ImageTiler.imageToTileLayer(img, handler.getTileset(), cols, rows, tileSize)
                    );
                    updateActiveMapLayerGL();
                    repaint();
                    //setBackImage(img);
                    //backImageEnabled = true;
                    //repaint();
                }
                break;
            case KeyEvent.VK_Z:
                if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                    handler.getMainFrame().undoMapState();
                }
                break;
            case KeyEvent.VK_Y:
                if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                    handler.getMainFrame().redoMapState();
                }
                break;
            case KeyEvent.VK_B:
                drawGridBorderMaps = !drawGridBorderMaps;
                repaint();
                break;
            case KeyEvent.VK_1:
                changeLayerWithNumKey(e, 0);
                repaint();
                break;
            case KeyEvent.VK_2:
                changeLayerWithNumKey(e, 1);
                repaint();
                break;
            case KeyEvent.VK_3:
                changeLayerWithNumKey(e, 2);
                repaint();
                break;
            case KeyEvent.VK_4:
                changeLayerWithNumKey(e, 3);
                repaint();
                break;
            case KeyEvent.VK_5:
                changeLayerWithNumKey(e, 4);
                repaint();
                break;
            case KeyEvent.VK_6:
                changeLayerWithNumKey(e, 5);
                repaint();
                break;
            case KeyEvent.VK_7:
                changeLayerWithNumKey(e, 6);
                repaint();
                break;
            case KeyEvent.VK_8:
                changeLayerWithNumKey(e, 7);
                repaint();
                break;
            case KeyEvent.VK_9:
                changeLayerWithNumKey(e, 8);
                repaint();
                break;
            case KeyEvent.VK_BACK_SLASH:
                handler.setAllLayersState(true);
                handler.getMainFrame().getThumbnailLayerSelector().repaint();
                repaint();
                break;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            SHIFT_PRESSED = false;
            //disableCameraMove();
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            CTRL_PRESSED = false;
            repaint();
        }

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (mouseWheelEnabled)
            viewMode.mouseWheelMoved(this, e);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (handler != null) {
            viewMode.paintComponent(this, g);

            //if (backImageEnabled) {
            //    drawBackImage(g);
            //}
        }

    }

    protected void applyGraphicsTransform(Graphics2D g2d) {
        g2d.setRenderingHints(new RenderingHints(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR));
        /*
                g2d.setRenderingHints(new RenderingHints(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR));*/


        float xScaleWindows = (float) getWidth() / width;
        float yScaleWindows = (float) getHeight() / height;
        g2d.scale(xScaleWindows, yScaleWindows);

        //TODO: Use this code for keeping the aspect ratio
        //g2d.scale(yScaleWindows, yScaleWindows);
        //float aspect = getAspectRatio();
        //g2d.translate((aspect - 1.0f) * width / 2, 1.0f);


        float xScaleFactor = orthoScale;
        float yScaleFactor = orthoScale;

        float xTranslation = (getWidth() * (1.0f - xScaleFactor) / 2f);
        float yTranslation = (getHeight() * (1.0f - yScaleFactor) / 2f);

        g2d.translate(xTranslation / xScaleWindows, yTranslation / yScaleWindows);
        //g2d.translate(xTranslation / yScaleWindows, yTranslation / yScaleWindows);
        g2d.scale(xScaleFactor, yScaleFactor);

        g2d.translate(-cameraX * tileSize, cameraY * tileSize);
    }

    protected void drawTileThumbnail(Graphics g) {
        if (handler.getTileset().size() > 0) {
            Tile tile = handler.getTileset().get(handler.getTileIndexSelected());
            int x = Math.floorDiv(xMouse, tileSize) * tileSize;
            int y = (Math.floorDiv(yMouse, tileSize) - (tile.getHeight() - 1)) * tileSize;

            g.drawImage(tile.getThumbnail(), x, y, null);

            g.setColor(Color.red);
            g.drawRect(x, y, tile.getWidth() * tileSize, tile.getHeight() * tileSize);
        }
    }

    protected void drawUnitTileBounds(Graphics g) {
        int x = Math.floorDiv(xMouse, tileSize) * tileSize;
        int y = Math.floorDiv(yMouse, tileSize) * tileSize;
        g.setColor(Color.red);
        g.drawRect(x, y, tileSize, tileSize);
    }

    protected void drawAllHeightMaps(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setComposite(AlphaComposite.SrcOver.derive(heightMapOpacity));

        for (HashMap.Entry<Point, MapData> map : handler.getMapMatrix().getMatrix().entrySet()) {
            drawHeightMap(g, map.getValue().getGrid().heightLayers[handler.getActiveLayerIndex()],
                    map.getKey().x * cols * tileSize, map.getKey().y * rows * tileSize);
        }
    }

    protected void drawHeightMap(Graphics g, int[][] heightGrid, int xOffset, int yOffset) {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                int x = (i + borderSize) * tileSize + xOffset;
                int y = (rows - 1 - j + borderSize) * tileSize + yOffset;
                g.drawImage(handler.getHeightImageByValue(heightGrid[i][j]), x, y, null);
            }
        }
    }

    protected void drawActiveHeightMap(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setComposite(AlphaComposite.SrcOver.derive(heightMapOpacity));

        Point map = handler.getMapSelected();
        drawHeightMap(g, handler.getActiveHeightLayer(), map.x * cols * tileSize, map.y * rows * tileSize);
    }

    protected void drawHeightMapsBorder(Graphics g, int borderSize) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setComposite(AlphaComposite.SrcOver.derive(heightMapOpacity));

        Point centerMap = handler.getMapSelected();
        for (int i = -borderSize; i <= borderSize; i++) {
            for (int j = -borderSize; j <= borderSize; j++) {
                Point map = new Point(centerMap.x + i, centerMap.y + j);
                MapData mapData = handler.getMapMatrix().getMatrix().get(map);
                if (mapData != null) {
                    drawHeightMap(g, mapData.getGrid().heightLayers[handler.getActiveLayerIndex()],
                            map.x * cols * tileSize, map.y * rows * tileSize);
                }
            }
        }
        //drawHeightMap(g, handler.getActiveHeightLayer(), centerMap.x * cols * tileSize, centerMap.y * rows * tileSize);
    }

    protected void drawAllMapBounds(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));

        Set<Point> maps = handler.getMapMatrix().getMatrix().keySet();
        for (Point map : maps) {
            drawBorderBounds(g2, map.x * cols * tileSize, map.y * rows * tileSize, 0);
        }
    }

    protected void drawAllMapContours(Graphics g) {
        HashMap<Integer, ArrayList<Point>> allContourPoints = handler.getMapMatrix().getContourPoints();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(4));

        AffineTransform transf = g2d.getTransform();

        g2d.translate(tileSize, tileSize);

        for (HashMap.Entry<Integer, ArrayList<Point>> entry : allContourPoints.entrySet()) {
            ArrayList<Point> contourPoints = entry.getValue();
            for (int i = 0; i < contourPoints.size(); i += 2) {
                try {
                    g.setColor(handler.getMapMatrix().getAreaColors().get(entry.getKey()));
                } catch (Exception ex) {
                    g.setColor(Color.blue);
                }
                Point p1 = contourPoints.get(i);
                Point p2 = contourPoints.get(i + 1);
                g.drawLine(
                        p1.x * cols * tileSize,
                        p1.y * rows * tileSize,
                        p2.x * cols * tileSize,
                        p2.y * rows * tileSize);
            }
        }

        g2d.setTransform(transf);
    }

    protected void drawBorderBounds(Graphics g, int xOffset, int yOffset, int outOffset) {
        g.drawRect(borderSize * tileSize + xOffset - outOffset, borderSize * tileSize + yOffset - outOffset,
                tileSize * cols + 2 * outOffset, tileSize * rows + 2 * outOffset);
    }

    protected void drawGrid(GL2 gl, float x, float y, float z, float r, float g, float b, float a) {
        //applyCameraTransform(gl);

        gl.glPushMatrix();

        gl.glTranslatef(x, y, z);

        //Adjust OpenGL settings and draw model
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glLineWidth(1);

        //gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glDisable(GL_TEXTURE_2D);
        gl.glColor4f(r, g, b, a);

        drawLines(gl, gridBuffer);

        gl.glColor4f(1, 1, 1, 1);

        gl.glPopMatrix();
    }

    protected void drawGridBorderMaps(GL2 gl, HashSet<Point> gridBorderMaps) {
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        for (Point borderMap : gridBorderMaps) {
            drawGrid(gl, borderMap.x * cols, -borderMap.y * rows, 0, 1.0f, 1.0f, 1.0f, 0.2f);
        }
    }

    protected void drawGridMaps(GL2 gl, HashMap<Point, MapData> maps) {
        Point mapSelected = handler.getMapSelected();
        for (Point map : maps.keySet()) {
            if (!map.equals(mapSelected)) {
                drawGrid(gl, map.x * cols, -map.y * rows, 0, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        drawGrid(gl, mapSelected.x * cols, -mapSelected.y * rows, 0, 1.0f, 0.9f, 0.9f, 1.0f);
    }

    protected void drawAxis() {
        GL2 gl = (GL2) GLContext.getCurrentGL();

        //applyCameraTransform(gl);

        gl.glDisable(GL_TEXTURE_2D);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glLineWidth(1f);

        gl.glBegin(GL_LINES);
        for (int i = 0; i < axis.length; i += 3) {
            gl.glColor3fv(axisColors, i);
            gl.glVertex3fv(axis, i);
        }

        gl.glEnd();
    }

    protected void drawOpaqueMaps(GL2 gl, HashMap<Point, MapData> maps) {
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_DST_ALPHA);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LESS); //Less instead of equal for drawing the grid

        gl.glEnable(GL_ALPHA_TEST);
        gl.glAlphaFunc(GL_GREATER, 0.9f);

        //long before = System.nanoTime();
        drawAllMaps(gl, maps, (gl2, geometryGL, textures) -> {
            drawGeometryGL(gl2, geometryGL, textures);
        });
        //System.out.println("Elapsed: " + (System.nanoTime() - before));
    }

    protected void drawTransparentMaps(GL2 gl, HashMap<Point, MapData> maps) {
        gl.glEnable(GL_BLEND);

        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        //gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LESS); //Less instead of equal for drawing the grid

        gl.glEnable(GL_ALPHA_TEST);
        gl.glAlphaFunc(GL_NOTEQUAL, 0.0f);

        drawAllMaps(gl, maps, (gl2, geometryGL, textures) -> {
            drawGeometryGL(gl2, geometryGL, textures);
        });
    }

    protected void drawWireframeMaps(GL2 gl, HashMap<Point, MapData> maps) {
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glColor3f(0.0f, 0.0f, 0.0f);

        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        gl.glDisable(GL_TEXTURE_2D);
        gl.glLineWidth(1.5f);

        drawAllMaps(gl, maps, (gl2, geometryGL, textures) -> {
            drawWireframeGeometryGL(gl2, geometryGL, textures);
        });

        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    protected void drawAllMaps(GL2 gl, HashMap<Point, MapData> maps, DrawGeometryGLFunction drawFunction) {

        for (HashMap.Entry<Point, MapData> map : maps.entrySet()) {

            drawAllMapLayersGL(gl, drawFunction, map.getValue().getGrid().mapLayersGL,
                    map.getKey().x * cols, -map.getKey().y * rows, 0);


            /*
            //Simple optimization culling map corners
            for(Vec4f cornerDelta : cornerDeltas){
                Vec3f mapCenter = new Vec4f(
                        map.getKey().x * MapGrid.cols,
                        -map.getKey().y * MapGrid.rows,
                        0.0f,
                        1.0f).add(cornerDelta).mul(camMVP).toVec3f();

                gl.glPushMatrix();
                gl.glLoadIdentity();

                gl.glPointSize(5.0f);

                gl.glBegin(GL2.GL_POINTS);
                gl.glVertex2f(mapCenter.x, mapCenter.y);
                gl.glEnd();

                gl.glPopMatrix();

                if (mapCenter.x > -1.0f && mapCenter.x < 1.0f && mapCenter.y > -1.0f && mapCenter.y < 1.0f && mapCenter.z > 0.0f) {
                    drawAllMapLayersGL(gl, drawFunction, map.getValue().getGrid().mapLayersGL,
                            map.getKey().x * cols, -map.getKey().y * rows, 0);
                    break;
                }
            }
            */


            /*
            //Simple optimization culling map centers
            Vec3f mapCenter = new Vec4f(
                    map.getKey().x * MapGrid.cols,
                    -map.getKey().y * MapGrid.rows,
                    0.0f,
                    1.0f).mul(camMVP).toVec3f();



            gl.glPushMatrix();
            gl.glLoadIdentity();

            gl.glPointSize(5.0f);

            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex2f(mapCenter.x, mapCenter.y);
            gl.glEnd();

            gl.glPopMatrix();


            if (mapCenter.x > -1.0f && mapCenter.x < 1.0f && mapCenter.y > -1.0f && mapCenter.y < 1.0f && mapCenter.z > 0.0f) {
                drawAllMapLayersGL(gl, drawFunction, map.getValue().getGrid().mapLayersGL,
                        map.getKey().x * cols, -map.getKey().y * rows, 0);
            }
            */

            /*
            drawAllMapLayersGL(gl, drawFunction, map.getValue().getGrid().mapLayersGL,
                    map.getKey().x * cols, -map.getKey().y * rows, 0);
            */
        }
    }

    protected void drawAllMapLayersGL(GL2 gl, DrawGeometryGLFunction drawFunction, MapLayerGL[] mapLayersGL, float x, float y, float z) {
        for (int i = 0; i < mapLayersGL.length; i++) {
            if (handler.renderLayers[i]) {
                if (mapLayersGL[i] != null) {
                    drawMapLayerGL(gl, drawFunction, mapLayersGL[i], x, y, z);
                }
            }
        }
    }

    protected void drawMapLayerGL(GL2 gl, DrawGeometryGLFunction drawFunction, MapLayerGL mapLayerGL, float x, float y, float z) {
        //applyCameraTransform(gl);
        gl.glPushMatrix();

        gl.glTranslatef(x, y, z);

        for (GeometryGL geometryGL : mapLayerGL.getGeometryGL().values()) {
            drawFunction.draw(gl, geometryGL, handler.getTileset().getTextures());
            //drawGeometryGL(gl, geometryGL, handler.getTileset().getTextures());
        }

        gl.glPopMatrix();
    }

    protected void drawGeometryGL(GL2 gl, GeometryGL geometryGL, ArrayList<Texture> textures) {
        try {
            gl.glBindTexture(GL_TEXTURE_2D, textures.get(geometryGL.textureID).getTextureObject());
            gl.glEnable(GL_TEXTURE_2D);

            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            //gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            //gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            //gl.glGenerateMipmap(GL_TEXTURE_2D);
            //Draw Tris
            if (geometryGL.hasTriBufferData()) {
                try {
                    gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                    gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
                    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                    //gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

                    gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, geometryGL.tCoordsTriBuffer);
                    gl.glColorPointer(3, GL2.GL_FLOAT, 0, geometryGL.colorsTriBuffer);
                    gl.glVertexPointer(3, GL2.GL_FLOAT, 0, geometryGL.vCoordsTriBuffer);
                    //gl.glNormalPointer(GL2.GL_FLOAT, 0, geometryGL.nCoordsTriBuffer);

                    gl.glDrawArrays(GL2.GL_TRIANGLES, 0, geometryGL.vCoordsTri.length / 3);

                    gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
                    gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
                    //gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);

                } catch (Exception ex) {
                    gl.glBegin(GL_TRIANGLES);
                    for (int i = 0, numVertices = geometryGL.vCoordsTri.length / 3; i < numVertices; i++) {
                        gl.glTexCoord2fv(geometryGL.tCoordsTri, i * 2);
                        gl.glColor3fv(geometryGL.colorsTri, i * 3);
                        gl.glVertex3fv(geometryGL.vCoordsTri, i * 3);
                    }
                    gl.glEnd();
                }
            }

            //Draw Quads
            if (geometryGL.hasQuadBufferData()) {
                try {
                    gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                    gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
                    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                    //gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

                    gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, geometryGL.tCoordsQuadBuffer);
                    gl.glColorPointer(3, GL2.GL_FLOAT, 0, geometryGL.colorsQuadBuffer);
                    gl.glVertexPointer(3, GL2.GL_FLOAT, 0, geometryGL.vCoordsQuadBuffer);
                    //gl.glNormalPointer(GL2.GL_FLOAT, 0, geometryGL.nCoordsQuadBuffer);

                    gl.glDrawArrays(GL2.GL_QUADS, 0, geometryGL.vCoordsQuad.length / 3);

                    gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
                    gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
                    //gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);

                } catch (Exception ex) {
                    gl.glBegin(GL_QUADS);
                    for (int i = 0, numVertices = geometryGL.vCoordsQuad.length / 3; i < numVertices; i++) {
                        gl.glTexCoord2fv(geometryGL.tCoordsQuad, i * 2);
                        gl.glColor3fv(geometryGL.colorsQuad, i * 3);
                        gl.glVertex3fv(geometryGL.vCoordsQuad, i * 3);
                    }
                    gl.glEnd();
                }
            }

            gl.glDisable(GL_TEXTURE_2D);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void drawWireframeGeometryGL(GL2 gl, GeometryGL geometryGL, ArrayList<Texture> textures) {
        try {
            //Draw Tris
            if (geometryGL.hasTriBufferData()) {
                try {
                    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);

                    gl.glVertexPointer(3, GL2.GL_FLOAT, 0, geometryGL.vCoordsTriBuffer);

                    gl.glDrawArrays(GL2.GL_TRIANGLES, 0, geometryGL.vCoordsTri.length / 3);

                    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);

                } catch (Exception ex) {
                    gl.glBegin(GL_TRIANGLES);
                    for (int i = 0, numVertices = geometryGL.vCoordsTri.length / 3; i < numVertices; i++) {
                        gl.glVertex3fv(geometryGL.vCoordsTri, i * 3);
                    }
                    gl.glEnd();
                }
            }

            //Draw Quads
            if (geometryGL.hasQuadBufferData()) {
                try {
                    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);

                    gl.glVertexPointer(3, GL2.GL_FLOAT, 0, geometryGL.vCoordsQuadBuffer);

                    gl.glDrawArrays(GL2.GL_QUADS, 0, geometryGL.vCoordsQuad.length / 3);

                    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);

                } catch (Exception ex) {
                    gl.glBegin(GL_QUADS);
                    for (int i = 0, numVertices = geometryGL.vCoordsQuad.length / 3; i < numVertices; i++) {
                        gl.glVertex3fv(geometryGL.vCoordsQuad, i * 3);
                    }
                    gl.glEnd();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void drawAllContourLines(GL2 gl) {
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDisable(GL_TEXTURE_2D);
        gl.glLineWidth(3f);

        //applyCameraTransform(gl);
        gl.glPushMatrix();

        gl.glTranslatef(-cols / 2, rows / 2, 0.025f);
        gl.glScalef(cols, -rows, 1.0f);

        for (HashMap.Entry<Integer, FloatBuffer> entry : handler.getMapMatrix().getContourPointsGL().entrySet()) {
            try {
                Color c = handler.getMapMatrix().getAreaColors().get(entry.getKey());
                gl.glColor3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            } catch (Exception ex) {
                gl.glColor3f(0.0f, 0.0f, 1.0f);
            }

            FloatBuffer contourPoints = entry.getValue();
            drawLines(gl, contourPoints);
        }

        gl.glLineWidth(1f);

        gl.glPopMatrix();
    }

    protected void drawLines(GL2 gl, FloatBuffer vCoordsPoints) {
        try {
            if (vCoordsPoints != null) {
                gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vCoordsPoints);
                gl.glDrawArrays(GL2.GL_LINES, 0, vCoordsPoints.limit() / 3);
                gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void rotToDir(Vec3f rot, Vec3f dir) {
        dir.mul(TransfMat.eulerDegToMat_(rot));
    }

    public static Vec3f rotToDir_(Vec3f angles) {
        Vec3f dir = new Vec3f(0.0f, 0.0f, -1.0f);
        rotToDir(angles, dir);
        return dir;
    }

    public static void rotToUp(Vec3f rot, Vec3f dst){
        dst.set(0.0f, 1.0f, 0.0f);
        dst.mul(TransfMat.eulerDegToMat_(rot));
    }

    public static Vec3f rotToUp_(Vec3f rot){
        Vec3f dst = new Vec3f();
        rotToUp(rot, dst);
        return dst;
    }

    public static float distPointPlaneSigned(Vec3f point, Vec3f[] plane){
        Vec3f normal = plane[1].sub_(plane[0]).cross(plane[2].sub_(plane[0])).normalize();
        //Vec3f normal = plane[2].sub_(plane[1]).cross(plane[0].sub_(plane[1])).normalize();
        return normal.dot(point) -normal.dot(plane[0]);
    }

    protected void applyCameraTransform(GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        viewMode.applyCameraTransform(this, gl);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(
                0.0f, 0.0f, cameraZ,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        gl.glRotatef(-cameraRotX, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(-cameraRotY, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(-cameraRotZ, 0.0f, 0.0f, 1.0f);

        gl.glTranslatef(-cameraX, -cameraY, 0.0f);


        Mat4f rx = TransfMat.rotationDeg_(-cameraRotX, new Vec3f(1.0f, 0.0f, 0.0f));
        Mat4f ry = TransfMat.rotationDeg_(-cameraRotY, new Vec3f(0.0f, 1.0f, 0.0f));
        Mat4f rz = TransfMat.rotationDeg_(-cameraRotZ, new Vec3f(0.0f, 0.0f, 1.0f));
        Vec3f tarPos = new Vec3f(cameraX, cameraY, 0.0f);
        Vec3f camDir = rotToDir_(new Vec3f(cameraRotX, cameraRotY, cameraRotZ));
        Vec3f camPos = tarPos.add_(camDir.negate_().scale_(cameraZ));
        Mat4f t = TransfMat.translation_(camPos.negate_());
        camModelView = rx.mul_(ry).mul(rz).mul(t);
        camProj = TransfMat.perspective_(60.0f, (float) getWidth() / getHeight(), 1.0f, 1000.0f);
        camMVP = camProj.mul_(camModelView);

        /*
        System.out.println("MODELVIEW: ");
        camModelView.print();
        System.out.println("PROJECTION: ");
        camProj.print();*/

        //new Vec3f(cameraX, cameraY, cameraZ).print();
        //rotToDir_(new Vec3f(cameraRotX, cameraRotY, cameraRotZ)).print();

    }

    protected boolean isPointInsideGrid(int col, int row) {
        return new Rectangle(cols, rows).contains(col, row);
    }

    protected boolean canUseDragging(int col, int row, int tileWidth, int tileHeight) {
        return (col % tileWidth) == (dragStart.getX() % tileWidth) && (row % tileHeight) == (dragStart.getY() % tileHeight);
    }

    private void clearAreaUnderTile(int[][] tileGrid, int col, int row, int tileWidth, int tileHeight) {
        for (int i = 0; i < tileWidth; i++) {
            for (int j = 0; j < tileHeight; j++) {
                if (isPointInsideGrid(col + i, row + j)) {
                    tileGrid[col + i][row + j] = -1;
                }
            }
        }
    }

    public void updateMapLayerGL(int layerIndex) {
        MapLayerGL newLayer = new MapLayerGL(
                handler.getTileLayer(layerIndex),
                handler.getHeightLayer(layerIndex),
                handler.getTileset(),
                handler.useRealTimePostProcessing(),
                handler.getGame().getMaxTileableSize());
        handler.getGrid().mapLayersGL[layerIndex] = newLayer;
    }

    public void updateActiveMapLayerGL() {
        updateMapLayerGL(handler.getActiveLayerIndex());
    }

    public void updateMapLayersGL() {
        for (int i = 0; i < handler.getGrid().mapLayersGL.length; i++) {
            updateMapLayerGL(i);
        }
    }

    protected void updateCursorTileCoordsStatus(MouseEvent e) {
        Point map = getMapCoords(e);
        handler.getMainFrame().updateCursorTileCoords(getCoordsInMap(e, map));
    }

    protected void updateMousePostion(MouseEvent e) {
        xMouse = (int) ((((float) e.getX() / getWidth() - (1.0f - orthoScale) / 2) * width) / orthoScale + cameraX * tileSize);
        yMouse = (int) ((((float) e.getY() / getHeight() - (1.0f - orthoScale) / 2) * height) / orthoScale - cameraY * tileSize);
    }

    protected void zoomCameraOrtho(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {
            orthoScale /= 1.1;
        } else {
            orthoScale *= 1.1;
        }
    }

    protected Point getCoordsInMap(MouseEvent e) {
        float x = (float) e.getX() / getWidth(); //Normalize
        x -= (1.0f - orthoScale) / 2; //Move ortho scale offset
        x *= (cols + 2 * borderSize) / orthoScale; //Apply grid size and ortho scale
        x -= borderSize; //Move border size
        x += cameraX; //Move camera

        float y = (float) e.getY() / getHeight();
        y -= (1.0f - orthoScale) / 2;
        y *= (rows + 2 * borderSize) / orthoScale;
        y = rows - y + borderSize;
        y += cameraY;

        return new Point((int) x, (int) y);
    }

    protected Point getCoordsInSelectedMap(MouseEvent e) {
        float x = (float) e.getX() / getWidth(); //Normalize
        x -= (1.0f - orthoScale) / 2; //Move ortho scale offset
        x *= (cols + 2 * borderSize) / orthoScale; //Apply grid size and ortho scale
        x -= borderSize; //Move border size
        x += cameraX; //Move camera

        float y = (float) e.getY() / getHeight();
        y -= (1.0f - orthoScale) / 2;
        y *= (rows + 2 * borderSize) / orthoScale;
        y -= borderSize;
        y -= cameraY;

        Point mapCoords = getMapCoords(e);

        int xInt = (int) x;
        int yInt = (int) y;

        if (x < 0) {
            xInt--;
        }

        if (y < 0) {
            yInt--;
        }

        xInt -= mapCoords.x * cols;
        yInt -= mapCoords.y * rows;

        xInt = Math.max(0, Math.min(cols - 1, xInt));
        yInt = rows - 1 - Math.max(0, Math.min(rows - 1, yInt));

        return new Point(xInt, yInt);
    }

    public Point getMapCoords(MouseEvent e) {
        float x = (float) e.getX() / getWidth(); //Normalize
        x -= (1.0f - orthoScale) / 2; //Move ortho scale offset
        x *= (cols + 2 * borderSize) / orthoScale; //Apply grid size and ortho scale
        x -= borderSize; //Move border size
        x += cameraX; //Move camera

        float y = (float) e.getY() / getHeight();
        y -= (1.0f - orthoScale) / 2;
        y *= (rows + 2 * borderSize) / orthoScale;
        y -= borderSize;
        y -= cameraY;

        return new Point(Math.floorDiv((int) Math.floor(x), cols), Math.floorDiv((int) Math.floor(y), rows));
    }

    public void setMapSelected(MouseEvent e) {
        Point selectedMap = getMapCoords(e);
        if (!selectedMap.equals(handler.getMapSelected())) {
            handler.setMapSelected(selectedMap);
        }
    }

    public void setMapSelectedIfExists(MouseEvent e) {
        Point selectedMap = getMapCoords(e);
        if (!selectedMap.equals(handler.getMapSelected()) && handler.mapExists(selectedMap)) {
            handler.setMapSelected(selectedMap);
        }
    }

    protected void floodFillClearTileInGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            if (isPointInsideGrid(p.x, p.y) && handler.getActiveTileLayer()[p.x][p.y] != -1) {
                handler.getGrid().floodFillTileGrid(p.x, p.y, -1, 1, 1);
                //updateMapThumbnail(e);
            }
        }
    }

    protected void floodFillTileInGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            if (isPointInsideGrid(p.x, p.y) && handler.getActiveTileLayer()[p.x][p.y] != handler.getTileIndexSelected()) {
                Tile tile = handler.getTileSelected();
                handler.getGrid().floodFillTileGrid(p.x, p.y, handler.getTileIndexSelected(), tile.getWidth(), tile.getHeight());
                applyAutoCollision(handler.getMapSelected());
                //updateMapThumbnail(e);
            }
        }
    }

    protected void floodFillHeightInGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            if (isPointInsideGrid(p.x, p.y) && handler.getActiveHeightLayer()[p.x][p.y] != handler.getHeightSelected()) {
                handler.getGrid().floodFillHeightGrid(p.x, p.y, handler.getHeightSelected());
                //updateMapThumbnail(e);
            }
        }
    }

    protected void smartFillTileInGrid(MouseEvent e, boolean invert) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            if (isPointInsideGrid(p.x, p.y)) {
                handler.getSmartGridSelected().useSmartFill(handler, p.x, p.y, invert);
                applyAutoCollision(handler.getMapSelected());
                //updateMapThumbnail(e);
            }
        }
    }

    protected void clearTileInGrid(MouseEvent e) {
        Point p = getCoordsInSelectedMap(e);
        int[][] tileGrid = handler.getActiveTileLayer();
        if (isPointInsideGrid(p.x, p.y) && tileGrid[p.x][p.y] != -1) {
            tileGrid[p.x][p.y] = -1;
            //updateMapThumbnail(e);
        }
    }

    protected void setTileInGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            int[][] tileGrid = handler.getActiveTileLayer();
            Tile tile = handler.getTileSelected();
            if (isPointInsideGrid(p.x, p.y) && tileGrid[p.x][p.y] != handler.getTileIndexSelected()) {
                System.out.println("Xg: " + p.x + " Yg: " + p.y);
                clearAreaUnderTile(tileGrid, p.x, p.y, tile.getWidth(), tile.getHeight());
                tileGrid[p.x][p.y] = handler.getTileIndexSelected();
                applyAutoCollision(handler.getMapSelected());
                //updateMapThumbnail(e);
            }
        }
    }

    protected void dragTileInGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            int[][] tileGrid = handler.getActiveTileLayer();
            Tile tile = handler.getTileSelected();
            p.x = ((p.x - dragStart.x % tile.getWidth()) / tile.getWidth()) * tile.getWidth() + dragStart.x % tile.getWidth();
            p.y = ((p.y - dragStart.y % tile.getHeight()) / tile.getHeight()) * tile.getHeight() + dragStart.y % tile.getHeight();
            //p.y -= (p.y % tile.getHeight()- dragStart.y % tile.getHeight());
            if (isPointInsideGrid(p.x, p.y) /*&& canUseDragging(p.x, p.y, tile.getWidth(), tile.getHeight())*/) {
                clearAreaUnderTile(tileGrid, p.x, p.y, tile.getWidth(), tile.getHeight());
                tileGrid[p.x][p.y] = handler.getTileIndexSelected();
                applyAutoCollision(handler.getMapSelected());
            }
        }
    }

    protected void setTileIndexFromGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            if (isPointInsideGrid(p.x, p.y)) {
                int index = handler.getActiveTileLayer()[p.x][p.y];
                if (index != -1) {
                    handler.setIndexTileSelected(index);
                }
            }
        }
    }

    protected void setHeightIndexFromGrid(MouseEvent e) {
        if (handler.getTileset().size() > 0) {
            Point p = getCoordsInSelectedMap(e);
            if (isPointInsideGrid(p.x, p.y)) {
                int index = handler.getActiveHeightLayer()[p.x][p.y];

                handler.setHeightSelected(index);
            }
        }
    }

    protected void setHeightInGrid(MouseEvent e, int value) {
        Point p = getCoordsInSelectedMap(e);
        int[][] heightGrid = handler.getActiveHeightLayer();
        if (isPointInsideGrid(p.x, p.y)) {
            if (heightGrid[p.x][p.y] != value) {
                heightGrid[p.x][p.y] = value;
                //updateMapThumbnail(e);
            }
        }
    }

    public boolean hasSelection() {
        return selection != null && !selection.isEmpty();
    }

    public boolean isPasting() {
        return pasting;
    }

    public boolean isFloatingMove() {
        return floatingMove;
    }

    public Rectangle getSelectionBounds() {
        return selection == null ? null : selection.getBounds();
    }

    public boolean isOrthoView() {
        return viewMode.getViewID() == ViewMode.ViewID.VIEW_ORTHO;
    }

    public void clearSelection() {
        selection = null;
        selDragStart = null;
        selDragActive = false;
        selAdditive = false;
        selAddBase = null;
        resizeHandle = 0;
        lassoFirst = null;
        lassoLast = null;
        notifySelectionChanged();
    }

    /** Deselects everything and stops any pending paste / floating move. */
    public void deselect() {
        if (floatingMove) {
            cancelFloatingMove();
        }
        pasting = false;
        clearSelection();
        repaint();
    }

    /* -------------------- Rectangle selection + resize handles -------------------- */

    /**
     * Starts a rectangle selection drag. Additive (Shift) keeps the existing
     * selection and unions the new rectangle into it.
     */
    protected void startSelection(MouseEvent e, boolean additive) {
        Point map = getMapCoords(e);
        Point cell = getCoordsInMap(e, map);

        //Grab a resize handle when clicking the border of an existing rectangular selection
        if (!additive && hasSelection() && selection.isRectangular()
                && map.equals(selection.getMapCoords())) {
            int handle = getResizeHandleAt(cell);
            if (handle != 0) {
                resizeHandle = handle;
                resizeBaseBounds = selection.getBounds();
                selDragActive = true;
                return;
            }
        }

        resizeHandle = 0;
        selDragMoved = false;
        selDragActive = true;
        selStartedAdditive = additive;
        selAdditive = additive && hasSelection() && map.equals(selection.getMapCoords());
        if (selAdditive) {
            boolean[][] mask = selection.getMask();
            selAddBase = new boolean[cols][rows];
            for (int i = 0; i < cols; i++) {
                selAddBase[i] = mask[i].clone();
            }
        } else {
            selAddBase = null;
            selection = new MapSelection(map);
        }
        selDragStart = cell;
        applySelectionRect(cell);
    }

    private void applySelectionRect(Point cell) {
        selection.setRect(selDragStart, cell);
        if (selAdditive && selAddBase != null) {
            selection.applyRegion(selAddBase, false);
        }
    }

    protected void updateSelection(MouseEvent e) {
        if (selection == null) {
            return;
        }
        Point cell = getCoordsInMap(e, selection.getMapCoords());
        if (resizeHandle != 0 && resizeBaseBounds != null) {
            int minX = resizeBaseBounds.x;
            int maxX = resizeBaseBounds.x + resizeBaseBounds.width - 1;
            int minY = resizeBaseBounds.y;
            int maxY = resizeBaseBounds.y + resizeBaseBounds.height - 1;
            if ((resizeHandle & HANDLE_LEFT) != 0) {
                minX = Math.min(cell.x, maxX);
            }
            if ((resizeHandle & HANDLE_RIGHT) != 0) {
                maxX = Math.max(cell.x, minX);
            }
            if ((resizeHandle & HANDLE_BOTTOM) != 0) {
                minY = Math.min(cell.y, maxY);
            }
            if ((resizeHandle & HANDLE_TOP) != 0) {
                maxY = Math.max(cell.y, minY);
            }
            selection.setRect(new Point(minX, minY), new Point(maxX, maxY));
            selDragMoved = true;
        } else if (selDragStart != null) {
            if (!cell.equals(selDragStart)) {
                selDragMoved = true;
            }
            applySelectionRect(cell);
        }
    }

    protected void endSelectionDrag() {
        resizeHandle = 0;
        resizeBaseBounds = null;
        selDragActive = false;
        selAdditive = false;
        selStartedAdditive = false;
        selAddBase = null;
        notifySelectionChanged();
    }
    protected int getResizeHandleAt(Point cell) {
        Rectangle r = selection.getBounds();
        if (r == null) {
            return 0;
        }
        int minX = r.x, maxX = r.x + r.width - 1;
        int minY = r.y, maxY = r.y + r.height - 1;
        if (cell.x < minX || cell.x > maxX || cell.y < minY || cell.y > maxY) {
            return 0;
        }
        int handle = 0;
        if (cell.x == minX) {
            handle |= HANDLE_LEFT;
        }
        if (cell.x == maxX) {
            handle |= HANDLE_RIGHT;
        }
        if (cell.y == minY) {
            handle |= HANDLE_BOTTOM;
        }
        if (cell.y == maxY) {
            handle |= HANDLE_TOP;
        }
        return handle;
    }

    /** Updates the mouse cursor with resize arrows when hovering the selection border. */
    protected void updateSelectModeCursor(MouseEvent e) {
        int cursorType = Cursor.CROSSHAIR_CURSOR;
        if (hasSelection() && selection.isRectangular()
                && getMapCoords(e).equals(selection.getMapCoords())) {
            int handle = getResizeHandleAt(getCoordsInMap(e, selection.getMapCoords()));
            //Note: grid y is flipped, HANDLE_TOP is the upper border on screen
            switch (handle) {
                case HANDLE_LEFT: cursorType = Cursor.W_RESIZE_CURSOR; break;
                case HANDLE_RIGHT: cursorType = Cursor.E_RESIZE_CURSOR; break;
                case HANDLE_TOP: cursorType = Cursor.N_RESIZE_CURSOR; break;
                case HANDLE_BOTTOM: cursorType = Cursor.S_RESIZE_CURSOR; break;
                case HANDLE_TOP | HANDLE_LEFT: cursorType = Cursor.NW_RESIZE_CURSOR; break;
                case HANDLE_TOP | HANDLE_RIGHT: cursorType = Cursor.NE_RESIZE_CURSOR; break;
                case HANDLE_BOTTOM | HANDLE_LEFT: cursorType = Cursor.SW_RESIZE_CURSOR; break;
                case HANDLE_BOTTOM | HANDLE_RIGHT: cursorType = Cursor.SE_RESIZE_CURSOR; break;
            }
        }
        setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    /* -------------------- Lasso selection -------------------- */

    protected void startLasso(MouseEvent e) {
        Point map = getMapCoords(e);
        Point cell = getCoordsInMap(e, map);
        boolean add = e.isControlDown() && hasSelection() && map.equals(selection.getMapCoords());
        if (!add) {
            selection = new MapSelection(map);
        }
        lassoFirst = cell;
        lassoLast = cell;
        selection.addCell(cell.x, cell.y);
    }

    protected void updateLasso(MouseEvent e) {
        if (selection == null || lassoLast == null) {
            return;
        }
        Point cell = getCoordsInMap(e, selection.getMapCoords());
        selection.addLine(lassoLast, cell);
        lassoLast = cell;
    }

    protected void endLasso() {
        if (selection != null && lassoFirst != null && lassoLast != null) {
            selection.closeAndFill(lassoFirst, lassoLast);
        }
        lassoFirst = null;
        lassoLast = null;
        notifySelectionChanged();
    }

    /* -------------------- Magic wand -------------------- */

    /**
     * Magic wand click. Global selects every matching tile of the map
     * (Shift), otherwise only the connected area. With Ctrl the region is
     * added to the selection, or removed when the clicked cell was already
     * selected. Global clicks also add, so consecutive Shift clicks keep
     * accumulating whole tile types.
     */
    protected void wandSelect(MouseEvent e, boolean global) {
        Point map = getMapCoords(e);
        Point cell = getCoordsInMap(e, map);
        int[][] tileLayer = handler.getMapMatrix().getMapAndCreate(map)
                .getGrid().tileLayers[handler.getActiveLayerIndex()];
        boolean[][] region;
        if (canUseSmartTools()) {
            region = MapSelection.computeWandRegion(tileLayer, cell.x, cell.y, !global,
                    handler.getSmartGridSelected().getTileIndices());
        } else {
            region = MapSelection.computeWandRegion(tileLayer, cell.x, cell.y, !global);
        }

        boolean combine = (e.isControlDown() || global)
                && hasSelection() && map.equals(selection.getMapCoords());
        if (combine) {
            //Only Ctrl removes; Shift always adds
            boolean remove = e.isControlDown() && selection.contains(cell.x, cell.y);
            selection.applyRegion(region, remove);
            if (selection.isEmpty()) {
                clearSelection();
            }
        } else {
            selection = new MapSelection(map);
            selection.applyRegion(region, false);
        }
        notifySelectionChanged();
    }

    /**
     * Returns the tile coords of the mouse position relative to the given map,
     * clamped to the map bounds. Uses the same grid convention as
     * getCoordsInSelectedMap (y = 0 is the bottom row).
     */
    protected Point getCoordsInMap(MouseEvent e, Point mapCoords) {
        float x = (float) e.getX() / getWidth();
        x -= (1.0f - orthoScale) / 2;
        x *= (cols + 2 * borderSize) / orthoScale;
        x -= borderSize;
        x += cameraX;

        float y = (float) e.getY() / getHeight();
        y -= (1.0f - orthoScale) / 2;
        y *= (rows + 2 * borderSize) / orthoScale;
        y -= borderSize;
        y -= cameraY;

        int xInt = (int) Math.floor(x) - mapCoords.x * cols;
        int yInt = (int) Math.floor(y) - mapCoords.y * rows;

        xInt = Math.max(0, Math.min(cols - 1, xInt));
        yInt = rows - 1 - Math.max(0, Math.min(rows - 1, yInt));
        return new Point(xInt, yInt);
    }

    public void selectAllInSelectedMap() {
        if (!isOrthoView()) {
            return;
        }
        setEditMode(EditMode.MODE_SELECT);
        handler.getMainFrame().getJtbModeSelect().setSelected(true);
        selection = new MapSelection(handler.getMapSelected());
        selection.setRect(new Point(0, 0), new Point(cols - 1, rows - 1));
        notifySelectionChanged();
        repaint();
    }

    /* -------------------- Copy / cut / delete / fill -------------------- */

    public boolean copySelection() {
        if (!hasSelection()) {
            return false;
        }
        Rectangle r = getSelectionBounds();
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(selection.getMapCoords()).getGrid();
        int layer = handler.getActiveLayerIndex();
        int[][] tiles = new int[r.width][r.height];
        int[][] heights = new int[r.width][r.height];
        boolean[][] mask = new boolean[r.width][r.height];
        boolean[][] selMask = selection.getMask();
        for (int i = 0; i < r.width; i++) {
            for (int j = 0; j < r.height; j++) {
                tiles[i][j] = grid.tileLayers[layer][r.x + i][r.y + j];
                heights[i][j] = grid.heightLayers[layer][r.x + i][r.y + j];
                mask[i][j] = selMask[r.x + i][r.y + j];
            }
        }
        handler.setRegionClipboard(tiles, heights, mask);
        notifySelectionChanged();
        return true;
    }

    public void cutSelection() {
        if (copySelection()) {
            deleteSelection("Cut Selection");
        }
    }

    public void deleteSelection() {
        deleteSelection("Delete Selection");
    }

    private void deleteSelection(String stateName) {
        if (!hasSelection()) {
            return;
        }
        handler.addMapState(new MapLayerState(stateName, handler));
        clearSelectedCells();
        refreshMapLayer(selection.getMapCoords());
    }

    private void clearSelectedCells() {
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(selection.getMapCoords()).getGrid();
        int layer = handler.getActiveLayerIndex();
        boolean[][] selMask = selection.getMask();
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (selMask[i][j]) {
                    grid.tileLayers[layer][i][j] = -1;
                    grid.heightLayers[layer][i][j] = 0;
                }
            }
        }
    }

    public void fillSelection() {
        if (!hasSelection() || handler.getTileset().size() == 0) {
            return;
        }
        handler.addMapState(new MapLayerState("Fill Selection", handler));
        Rectangle r = getSelectionBounds();
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(selection.getMapCoords()).getGrid();
        int layer = handler.getActiveLayerIndex();
        boolean[][] selMask = selection.getMask();
        Tile tile = handler.getTileSelected();
        int w = tile.getWidth();
        int h = tile.getHeight();
        for (int i = r.x; i + w <= r.x + r.width; i += w) {
            for (int j = r.y; j + h <= r.y + r.height; j += h) {
                if (selMask[i][j]) {
                    clearAreaUnderTile(grid.tileLayers[layer], i, j, w, h);
                    grid.tileLayers[layer][i][j] = handler.getTileIndexSelected();
                }
            }
        }
        applyAutoCollision(selection.getMapCoords());
        refreshMapLayer(selection.getMapCoords());
    }

    /* -------------------- Paste (stamp) -------------------- */

    public void startPaste() {
        if (!handler.hasRegionClipboard() || !isOrthoView()) {
            return;
        }
        pasting = true;
        if (editMode != EditMode.MODE_SELECT) {
            setEditMode(EditMode.MODE_SELECT);
        }
        handler.getMainFrame().getJtbModeSelect().setSelected(true);
        repaint();
    }

    public void cancelPaste() {
        pasting = false;
        repaint();
    }

    protected void commitPaste(MouseEvent e) {
        if (!pasting || !handler.hasRegionClipboard()) {
            return;
        }
        int[][] tiles = handler.getTileRegionClipboard();
        int[][] heights = handler.getHeightRegionClipboard();
        boolean[][] mask = handler.getRegionClipboardMask();

        setMapSelected(e);
        updateMousePostion(e);
        Point anchor = getGlobalCellFromMouse(); //Top-left cell of the pasted region (global coords)

        handler.addMapState(new MapLayerState("Paste Selection", handler));
        Set<Point> touched = writeRegionGlobal(anchor.x, anchor.y, tiles, heights, mask);
        applyAutoCollision(touched);
        setSelectionFromGlobalRegion(anchor.x, anchor.y, mask);
        refreshMaps(touched);
        pasting = false;
        notifySelectionChanged();
    }

    /* -------------------- Move selection (floating region) -------------------- */

    protected boolean canBeginMoveSelection(MouseEvent e) {
        return isCursorInsideSelection(e);
    }

    /** True when the mouse event lies on a selected cell. */
    protected boolean isCursorInsideSelection(MouseEvent e) {
        if (!hasSelection()) {
            return false;
        }
        Point map = getMapCoords(e);
        if (!map.equals(selection.getMapCoords())) {
            return false;
        }
        Point cell = getCoordsInMap(e, map);
        return selection.contains(cell.x, cell.y);
    }

    protected void notifySelectionChanged() {
        handler.getMainFrame().updateSelectionActionButtons();
    }

    protected void beginMoveSelection(MouseEvent e) {
        Rectangle r = getSelectionBounds();
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(selection.getMapCoords()).getGrid();
        int layer = handler.getActiveLayerIndex();

        handler.addMapState(new MapLayerState("Move Selection", handler));

        floatTiles = new int[r.width][r.height];
        floatHeights = new int[r.width][r.height];
        floatMask = new boolean[r.width][r.height];
        boolean[][] selMask = selection.getMask();
        for (int i = 0; i < r.width; i++) {
            for (int j = 0; j < r.height; j++) {
                floatTiles[i][j] = grid.tileLayers[layer][r.x + i][r.y + j];
                floatHeights[i][j] = grid.heightLayers[layer][r.x + i][r.y + j];
                floatMask[i][j] = selMask[r.x + i][r.y + j];
            }
        }
        clearSelectedCells();

        int topY = r.y + r.height - 1;
        Point cell = getCoordsInMap(e, selection.getMapCoords());
        floatGrabOffset = new Point(cell.x - r.x, topY - cell.y);
        floatHomeMap = new Point(selection.getMapCoords());
        floatHomeTopLeft = new Point(r.x, topY);
        floatingMove = true;

        refreshMapLayer(floatHomeMap);
    }

    protected void commitMoveSelection(MouseEvent e) {
        if (!floatingMove) {
            return;
        }
        updateMousePostion(e);
        Point mouseCell = getGlobalCellFromMouse();
        int anchorLeft = mouseCell.x - floatGrabOffset.x;
        int anchorTop = mouseCell.y - floatGrabOffset.y;

        Set<Point> touched = writeRegionGlobal(anchorLeft, anchorTop, floatTiles, floatHeights, floatMask);
        setSelectionFromGlobalRegion(anchorLeft, anchorTop, floatMask);

        floatingMove = false;
        floatTiles = null;
        floatHeights = null;
        floatMask = null;
        refreshMaps(touched);
    }

    public void cancelFloatingMove() {
        if (!floatingMove) {
            return;
        }
        //Put the region back where it was lifted from
        int homeLeftGlobal = floatHomeMap.x * cols + floatHomeTopLeft.x;
        int homeTopGlobal = floatHomeMap.y * rows + (rows - 1 - floatHomeTopLeft.y);
        Set<Point> touched = writeRegionGlobal(homeLeftGlobal, homeTopGlobal, floatTiles, floatHeights, floatMask);

        floatingMove = false;
        floatTiles = null;
        floatHeights = null;
        floatMask = null;
        refreshMaps(touched);
    }

    /* -------------------- Rotate / flip selection -------------------- */

    public void rotateSelection90() {
        transformSelection(0);
    }

    public void flipSelectionHorizontal() {
        transformSelection(1);
    }

    public void flipSelectionVertical() {
        transformSelection(2);
    }

    /**
     * Transforms the selected region in place (op 0: rotate 90 degrees
     * clockwise, 1: flip horizontal, 2: flip vertical). The region keeps its
     * top left corner. Tile graphics themselves are not rotated, only their
     * placement.
     */
    private void transformSelection(int op) {
        if (!hasSelection() || floatingMove) {
            return;
        }
        Rectangle r = getSelectionBounds();
        Point map = selection.getMapCoords();
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(map).getGrid();
        int layer = handler.getActiveLayerIndex();
        int w = r.width;
        int h = r.height;

        int[][] tiles = new int[w][h];
        int[][] heights = new int[w][h];
        boolean[][] mask = new boolean[w][h];
        boolean[][] smartMask = new boolean[w][h];
        boolean[][] selMask = selection.getMask();
        Set<Integer> smartTileIndices = canUseSmartTools()
                ? handler.getSmartGridSelected().getTileIndices()
                : java.util.Collections.emptySet();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                tiles[i][j] = grid.tileLayers[layer][r.x + i][r.y + j];
                heights[i][j] = grid.heightLayers[layer][r.x + i][r.y + j];
                mask[i][j] = selMask[r.x + i][r.y + j];
                smartMask[i][j] = mask[i][j] && smartTileIndices.contains(tiles[i][j]);
            }
        }

        String[] names = {"Rotate Selection", "Flip Selection", "Flip Selection"};
        handler.addMapState(new MapLayerState(names[op], handler));
        clearSelectedCells();

        int nw = op == 0 ? h : w;
        int nh = op == 0 ? w : h;
        int[][] newTiles = new int[nw][nh];
        int[][] newHeights = new int[nw][nh];
        boolean[][] newMask = new boolean[nw][nh];
        boolean[][] newSmartMask = new boolean[nw][nh];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int ni, nj;
                if (op == 0) {          //Rotate 90 degrees clockwise on screen
                    ni = j;
                    nj = w - 1 - i;
                } else if (op == 1) {   //Flip horizontal
                    ni = w - 1 - i;
                    nj = j;
                } else {                //Flip vertical
                    ni = i;
                    nj = h - 1 - j;
                }
                newTiles[ni][nj] = tiles[i][j];
                newHeights[ni][nj] = heights[i][j];
                newMask[ni][nj] = mask[i][j];
                newSmartMask[ni][nj] = smartMask[i][j];
            }
        }

        if (!smartTileIndices.isEmpty() && hasCells(newSmartMask)) {
            int[][] resolved = handler.getSmartGridSelected().resolveMask(newSmartMask, false);
            for (int i = 0; i < nw; i++) {
                for (int j = 0; j < nh; j++) {
                    if (newSmartMask[i][j]) {
                        newTiles[i][j] = resolved[i][j];
                    }
                }
            }
        }

        int leftGlobal = map.x * cols + r.x;
        int topGlobal = map.y * rows + (rows - 1 - (r.y + r.height - 1));
        Set<Point> touched = writeRegionGlobal(leftGlobal, topGlobal, newTiles, newHeights, newMask);
        touched.add(map);
        setSelectionFromGlobalRegion(leftGlobal, topGlobal, newMask);
        refreshMaps(touched);
    }

    /* -------------------- Global grid write helpers -------------------- */

    /**
     * Global cell under the mouse: x is the global column and y the global
     * row counted downwards on screen. Cell (0, 0) is the top left cell of
     * map (0, 0).
     */
    protected Point getGlobalCellFromMouse() {
        return new Point(
                Math.floorDiv(xMouse, tileSize) - borderSize,
                Math.floorDiv(yMouse, tileSize) - borderSize);
    }

    /**
     * Writes a region (tiles + heights in grid row order, j = 0 bottom row)
     * into the world at the given global position. Cells outside the mask are
     * skipped. Returns the set of map coords that were modified.
     *
     * @param leftGlobalX global column of the left edge
     * @param topGlobalY  global row (downwards) of the top edge
     */
    protected Set<Point> writeRegionGlobal(int leftGlobalX, int topGlobalY,
                                           int[][] tiles, int[][] heights, boolean[][] mask) {
        Set<Point> touched = new HashSet<>();
        int w = tiles.length;
        int h = tiles[0].length;
        int layer = handler.getActiveLayerIndex();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (mask != null && !mask[i][j]) {
                    continue;
                }
                int gx = leftGlobalX + i;
                int gyDown = topGlobalY + (h - 1 - j);
                Point map = new Point(Math.floorDiv(gx, cols), Math.floorDiv(gyDown, rows));
                int cx = gx - map.x * cols;
                int cy = rows - 1 - (gyDown - map.y * rows);
                MapGrid grid = handler.getMapMatrix().getMapAndCreate(map).getGrid();
                grid.tileLayers[layer][cx][cy] = tiles[i][j];
                grid.heightLayers[layer][cx][cy] = heights[i][j];
                touched.add(map);
            }
        }
        return touched;
    }

    /**
     * Places the selection marquee over a region that was written at the
     * given global position. The marquee lives on the map that contains the
     * region's top left cell; cells falling on other maps are not selected.
     */
    protected void setSelectionFromGlobalRegion(int leftGlobalX, int topGlobalY, boolean[][] mask) {
        if (mask == null) {
            clearSelection();
            return;
        }
        int w = mask.length;
        int h = mask[0].length;
        Point map = new Point(Math.floorDiv(leftGlobalX, cols), Math.floorDiv(topGlobalY, rows));
        selection = new MapSelection(map);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (!mask[i][j]) {
                    continue;
                }
                int gx = leftGlobalX + i;
                int gyDown = topGlobalY + (h - 1 - j);
                if (Math.floorDiv(gx, cols) == map.x && Math.floorDiv(gyDown, rows) == map.y) {
                    selection.addCell(gx - map.x * cols, rows - 1 - (gyDown - map.y * rows));
                }
            }
        }
        if (selection.isEmpty()) {
            clearSelection();
        }
    }

    protected void refreshMaps(Set<Point> maps) {
        int layer = handler.getActiveLayerIndex();
        for (Point p : maps) {
            MapData mapData = handler.getMapMatrix().getMapAndCreate(p);
            mapData.getGrid().updateMapLayerGL(layer, handler.useRealTimePostProcessing());
            mapData.updateMapThumbnail();
        }
        handler.updateLayerThumbnail(layer);
        handler.repaintThumbnailLayerSelector();
        handler.getMainFrame().updateMapMatrixDisplay();
        repaint();
    }

    protected void refreshMapLayer(Point mapCoords) {
        refreshMaps(java.util.Collections.singleton(mapCoords));
    }

    /* -------------------- Bucket / picker / line / rectangle tools -------------------- */

    protected void bucketFill(MouseEvent e) {
        if (handler.getTileset().size() == 0) {
            return;
        }
        setMapSelected(e);
        Point p = getCoordsInSelectedMap(e);
        if (!isPointInsideGrid(p.x, p.y)
                || handler.getActiveTileLayer()[p.x][p.y] == handler.getTileIndexSelected()) {
            return;
        }
        handler.addMapState(new MapLayerState("Flood Fill Tile", handler));
        Tile tile = handler.getTileSelected();
        boolean[][] restrict = null;
        if (hasSelection() && selection.getMapCoords().equals(handler.getMapSelected())
                && selection.contains(p.x, p.y)) {
            restrict = selection.getMask();
        }
        handler.getGrid().floodFillTileGrid(p.x, p.y, handler.getTileIndexSelected(),
                tile.getWidth(), tile.getHeight(), restrict);
        applyAutoCollision(handler.getMapSelected());
        refreshMapLayer(handler.getMapSelected());
    }

    protected void pickTile(MouseEvent e) {
        setMapSelected(e);
        setTileIndexFromGrid(e);
        handler.getMainFrame().updateTileSelectedID();
        handler.getMainFrame().repaintTileSelector();
        handler.getMainFrame().updateTileSelectorScrollBar();
        handler.getMainFrame().repaintTileDisplay();
        //Jump back to the draw tool, like Paint.net's color picker
        setEditMode(EditMode.MODE_EDIT);
        handler.getMainFrame().getJtbModeEdit().setSelected(true);
        repaint();
    }

    protected void startShape(MouseEvent e, boolean inverted) {
        setMapSelected(e);
        shapeMap = getMapCoords(e);
        shapeStart = getCoordsInMap(e, shapeMap);
        shapeEnd = new Point(shapeStart);
        smartShapeInverted = inverted;
    }

    protected void updateShape(MouseEvent e) {
        if (shapeMap != null) {
            shapeEnd = getCoordsInMap(e, shapeMap);
        }
    }

    protected void commitLine() {
        java.util.List<Point> cells = canUseSmartTools()
                ? MapSelection.orthogonalLine(shapeStart, shapeEnd)
                : MapSelection.bresenham(shapeStart, shapeEnd);
        commitShapeCells("Draw Line", cells);
    }

    protected void commitRectShape() {
        commitShapeCells("Draw Rectangle", canUseSmartTools()
                ? getRectFillCells(shapeStart, shapeEnd)
                : getRectOutlineCells(shapeStart, shapeEnd));
    }

    protected boolean canStartSmartStroke() {
        return canUseSmartTools()
                && handler.getSmartGridSelected().containsTile(handler.getTileIndexSelected());
    }

    protected void startSmartStroke(MouseEvent e, boolean inverted) {
        setMapSelected(e);
        smartStrokeMap = new Point(handler.getMapSelected());
        smartStrokeLayer = handler.getActiveLayerIndex();
        smartStrokeStartTile = handler.getTileIndexSelected();
        smartStrokeInverted = inverted;
        smartStrokeCells = new ArrayList<>();
        smartStrokeLast = getCoordsInMap(e, smartStrokeMap);
        smartStrokeCells.add(new Point(smartStrokeLast));
        int[][] source = handler.getMapMatrix().getMapAndCreate(smartStrokeMap)
                .getGrid().tileLayers[smartStrokeLayer];
        smartStrokeBaseLayer = cloneIntGrid(source);
        renderSmartStroke();
    }

    protected void extendSmartStroke(MouseEvent e) {
        if (smartStrokeMap == null || smartStrokeCells == null
                || !smartStrokeMap.equals(getMapCoords(e))) {
            return;
        }
        Point next = getCoordsInMap(e, smartStrokeMap);
        if (next.equals(smartStrokeLast)) {
            return;
        }
        java.util.List<Point> segment = MapSelection.bresenham(smartStrokeLast, next);
        for (int i = 1; i < segment.size(); i++) {
            Point cell = segment.get(i);
            if (smartStrokeCells.isEmpty()
                    || !smartStrokeCells.get(smartStrokeCells.size() - 1).equals(cell)) {
                smartStrokeCells.add(new Point(cell));
            }
        }
        smartStrokeLast = next;
        renderSmartStroke();
    }

    protected void finishSmartStroke() {
        if (smartStrokeMap != null) {
            applyAutoCollision(smartStrokeMap);
            refreshMapLayer(smartStrokeMap);
        }
        resetSmartStroke();
    }

    private void renderSmartStroke() {
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(smartStrokeMap).getGrid();
        int[][] target = grid.tileLayers[smartStrokeLayer];
        for (int x = 0; x < target.length; x++) {
            System.arraycopy(smartStrokeBaseLayer[x], 0, target[x], 0, target[x].length);
        }
        int[][] resolved = handler.getSmartGridSelected().resolvePath(
                smartStrokeCells, smartStrokeStartTile, cols, rows, smartStrokeInverted);
        for (Point cell : smartStrokeCells) {
            int tileIndex = resolved[cell.x][cell.y];
            if (tileIndex >= 0) {
                target[cell.x][cell.y] = tileIndex;
            }
        }
    }

    private void resetSmartStroke() {
        smartStrokeMap = null;
        smartStrokeLast = null;
        smartStrokeCells = null;
        smartStrokeBaseLayer = null;
        smartStrokeLayer = -1;
        smartStrokeStartTile = -1;
        smartStrokeInverted = false;
    }

    private static int[][] cloneIntGrid(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = java.util.Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }

    protected void commitEllipseShape() {
        commitShapeCells("Draw Circle", canUseSmartTools()
                ? getEllipseFillCells(shapeStart, shapeEnd)
                : getEllipseOutlineCells(shapeStart, shapeEnd));
    }

    private void commitShapeCells(String stateName, java.util.List<Point> cells) {
        if (shapeMap == null || handler.getTileset().size() == 0) {
            resetShape();
            return;
        }
        if (canUseSmartTools()) {
            commitSmartShapeCells("Smart " + stateName, cells);
            return;
        }
        handler.addMapState(new MapLayerState(stateName, handler));
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(shapeMap).getGrid();
        int layer = handler.getActiveLayerIndex();
        Tile tile = handler.getTileSelected();
        for (Point cell : cells) {
            clearAreaUnderTile(grid.tileLayers[layer], cell.x, cell.y, tile.getWidth(), tile.getHeight());
            grid.tileLayers[layer][cell.x][cell.y] = handler.getTileIndexSelected();
        }
        Point map = shapeMap;
        applyAutoCollision(map);
        resetShape();
        refreshMapLayer(map);
    }

    private void commitSmartShapeCells(String stateName, java.util.List<Point> cells) {
        Point map = shapeMap;
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(map).getGrid();
        int layer = handler.getActiveLayerIndex();
        boolean[][] mask = buildSmartShapeMask(cells, grid.tileLayers[layer]);
        if (!hasCells(mask)) {
            resetShape();
            return;
        }

        int[][] resolved = editMode == EditMode.MODE_LINE
                ? handler.getSmartGridSelected().resolvePath(cells,
                        handler.getTileIndexSelected(), cols, rows, smartShapeInverted)
                : handler.getSmartGridSelected().resolveMask(mask, smartShapeInverted);
        handler.addMapState(new MapLayerState(stateName, handler));
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (mask[x][y] && resolved[x][y] >= 0) {
                    grid.tileLayers[layer][x][y] = resolved[x][y];
                }
            }
        }
        applyAutoCollision(map);
        resetShape();
        refreshMapLayer(map);
    }

    /**
     * Combines the newly drawn footprint with existing connected cells from
     * the selected template so joins and end caps update together.
     */
    private boolean[][] buildSmartShapeMask(java.util.List<Point> cells, int[][] tileLayer) {
        boolean[][] candidates = new boolean[cols][rows];
        boolean[][] affected = new boolean[cols][rows];
        Set<Integer> templateTiles = handler.getSmartGridSelected().getTileIndices();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                candidates[x][y] = templateTiles.contains(tileLayer[x][y]);
            }
        }

        java.util.ArrayDeque<Point> queue = new java.util.ArrayDeque<>();
        for (Point cell : cells) {
            if (!isPointInsideGrid(cell.x, cell.y)) {
                continue;
            }
            candidates[cell.x][cell.y] = true;
            if (!affected[cell.x][cell.y]) {
                affected[cell.x][cell.y] = true;
                queue.addLast(new Point(cell));
            }
        }

        int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            Point cell = queue.removeFirst();
            for (int[] delta : deltas) {
                int nx = cell.x + delta[0];
                int ny = cell.y + delta[1];
                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows
                        && candidates[nx][ny] && !affected[nx][ny]) {
                    affected[nx][ny] = true;
                    queue.addLast(new Point(nx, ny));
                }
            }
        }
        return affected;
    }

    private static boolean hasCells(boolean[][] mask) {
        for (boolean[] column : mask) {
            for (boolean value : column) {
                if (value) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Clears both the shape geometry and its left/right Smart orientation. */
    protected void resetShape() {
        shapeMap = null;
        shapeStart = null;
        shapeEnd = null;
        smartShapeInverted = false;
    }

    protected java.util.List<Point> getRectOutlineCells(Point a, Point b) {
        java.util.ArrayList<Point> cells = new java.util.ArrayList<>();
        int minX = Math.min(a.x, b.x), maxX = Math.max(a.x, b.x);
        int minY = Math.min(a.y, b.y), maxY = Math.max(a.y, b.y);
        for (int i = minX; i <= maxX; i++) {
            cells.add(new Point(i, minY));
            if (maxY != minY) {
                cells.add(new Point(i, maxY));
            }
        }
        for (int j = minY + 1; j < maxY; j++) {
            cells.add(new Point(minX, j));
            if (maxX != minX) {
                cells.add(new Point(maxX, j));
            }
        }
        return cells;
    }

    protected java.util.List<Point> getRectFillCells(Point a, Point b) {
        java.util.ArrayList<Point> cells = new java.util.ArrayList<>();
        int minX = Math.min(a.x, b.x), maxX = Math.max(a.x, b.x);
        int minY = Math.min(a.y, b.y), maxY = Math.max(a.y, b.y);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                cells.add(new Point(x, y));
            }
        }
        return cells;
    }

    protected java.util.List<Point> getEllipseFillCells(Point a, Point b) {
        java.util.ArrayList<Point> cells = new java.util.ArrayList<>();
        int minX = Math.min(a.x, b.x), maxX = Math.max(a.x, b.x);
        int minY = Math.min(a.y, b.y), maxY = Math.max(a.y, b.y);
        double cx = (minX + maxX + 1) / 2.0;
        double cy = (minY + maxY + 1) / 2.0;
        double rx = (maxX - minX + 1) / 2.0;
        double ry = (maxY - minY + 1) / 2.0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                double dx = (x + 0.5 - cx) / rx;
                double dy = (y + 0.5 - cy) / ry;
                if (dx * dx + dy * dy <= 1.0) {
                    cells.add(new Point(x, y));
                }
            }
        }
        return cells;
    }

    /**
     * Outline cells of the ellipse inscribed in the drag rectangle: the
     * filled ellipse cells that touch a cell outside the fill.
     */
    protected java.util.List<Point> getEllipseOutlineCells(Point a, Point b) {
        java.util.ArrayList<Point> cells = new java.util.ArrayList<>();
        int minX = Math.min(a.x, b.x), maxX = Math.max(a.x, b.x);
        int minY = Math.min(a.y, b.y), maxY = Math.max(a.y, b.y);
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;

        double cx = (minX + maxX + 1) / 2.0;
        double cy = (minY + maxY + 1) / 2.0;
        double rx = w / 2.0;
        double ry = h / 2.0;

        boolean[][] fill = new boolean[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                double dx = (minX + i + 0.5 - cx) / rx;
                double dy = (minY + j + 0.5 - cy) / ry;
                fill[i][j] = dx * dx + dy * dy <= 1.0;
            }
        }
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (!fill[i][j]) {
                    continue;
                }
                boolean boundary = i == 0 || j == 0 || i == w - 1 || j == h - 1
                        || !fill[i - 1][j] || !fill[i + 1][j]
                        || !fill[i][j - 1] || !fill[i][j + 1];
                if (boundary) {
                    cells.add(new Point(minX + i, minY + j));
                }
            }
        }
        return cells;
    }

    /* -------------------- Overlay drawing -------------------- */

    protected void drawSelectionOverlay(Graphics g) {
        if (!hasSelection()) {
            return;
        }
        Point map = selection.getMapCoords();
        boolean[][] mask = selection.getMask();
        int xOffset = (map.x * cols + borderSize) * tileSize;
        int yOffset = (map.y * rows + borderSize) * tileSize;

        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();

        g2d.setColor(new Color(70, 150, 255, 50));
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (mask[i][j]) {
                    g2d.fillRect(xOffset + i * tileSize,
                            yOffset + (rows - 1 - j) * tileSize, tileSize, tileSize);
                }
            }
        }

        //Border segments where a selected cell has an unselected neighbor
        Stroke solid = new BasicStroke(1);
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{4.0f, 4.0f}, 0.0f);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (!mask[i][j]) {
                    continue;
                }
                int px = xOffset + i * tileSize;
                int py = yOffset + (rows - 1 - j) * tileSize;
                //Grid y up: neighbor j + 1 is above on screen
                boolean top = j == rows - 1 || !mask[i][j + 1];
                boolean bottom = j == 0 || !mask[i][j - 1];
                boolean left = i == 0 || !mask[i - 1][j];
                boolean right = i == cols - 1 || !mask[i + 1][j];
                drawSelectionEdges(g2d, solid, dashed, px, py, top, bottom, left, right);
            }
        }
        g2d.setStroke(oldStroke);
    }

    private void drawSelectionEdges(Graphics2D g2d, Stroke solid, Stroke dashed,
                                    int px, int py, boolean top, boolean bottom, boolean left, boolean right) {
        for (int pass = 0; pass < 2; pass++) {
            g2d.setStroke(pass == 0 ? solid : dashed);
            g2d.setColor(pass == 0 ? Color.black : Color.white);
            if (top) {
                g2d.drawLine(px, py, px + tileSize, py);
            }
            if (bottom) {
                g2d.drawLine(px, py + tileSize, px + tileSize, py + tileSize);
            }
            if (left) {
                g2d.drawLine(px, py, px, py + tileSize);
            }
            if (right) {
                g2d.drawLine(px + tileSize, py, px + tileSize, py + tileSize);
            }
        }
    }

    protected void drawPastePreview(Graphics g) {
        if (!pasting || !handler.hasRegionClipboard() || handler.getTileset().size() == 0) {
            return;
        }
        drawRegionPreview(g, handler.getTileRegionClipboard(), handler.getRegionClipboardMask(),
                Math.floorDiv(xMouse, tileSize), Math.floorDiv(yMouse, tileSize));
    }

    protected void drawFloatingMovePreview(Graphics g) {
        if (!floatingMove || floatTiles == null) {
            return;
        }
        int ax = Math.floorDiv(xMouse, tileSize) - floatGrabOffset.x;
        int ay = Math.floorDiv(yMouse, tileSize) - floatGrabOffset.y;
        drawRegionPreview(g, floatTiles, floatMask, ax, ay);
    }

    /**
     * Draws a semi transparent preview of a region (tiles in grid row order)
     * whose top left cell is at the given pixel space cell coords.
     */
    protected void drawRegionPreview(Graphics g, int[][] tiles, boolean[][] mask, int ax, int ay) {
        int w = tiles.length;
        int h = tiles[0].length;

        Graphics2D g2d = (Graphics2D) g;
        java.awt.Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.6f));
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (mask != null && !mask[i][j]) {
                    continue;
                }
                int index = tiles[i][j];
                if (index >= 0 && index < handler.getTileset().size()) {
                    Tile tile = handler.getTileset().get(index);
                    int x = (ax + i) * tileSize;
                    int y = (ay + (h - 1 - j) - (tile.getHeight() - 1)) * tileSize;
                    g.drawImage(tile.getThumbnail(), x, y, null);
                }
            }
        }
        g2d.setComposite(oldComposite);
        g2d.setColor(Color.orange);
        g2d.drawRect(ax * tileSize, ay * tileSize, w * tileSize, h * tileSize);
    }

    protected void drawShapePreview(Graphics g) {
        if (shapeMap == null || shapeStart == null || shapeEnd == null
                || handler.getTileset().size() == 0) {
            return;
        }
        int xOffset = (shapeMap.x * cols + borderSize) * tileSize;
        int yOffset = (shapeMap.y * rows + borderSize) * tileSize;
        java.util.List<Point> cells = getCurrentShapeCells();
        if (canUseSmartTools()) {
            drawSmartShapePreview(g, cells, xOffset, yOffset);
            return;
        }

        Tile tile = handler.getTileSelected();
        Graphics2D g2d = (Graphics2D) g;
        java.awt.Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.6f));
        for (Point cell : cells) {
            int x = xOffset + cell.x * tileSize;
            int y = yOffset + (rows - 1 - cell.y - (tile.getHeight() - 1)) * tileSize;
            g.drawImage(tile.getThumbnail(), x, y, null);
        }
        g2d.setComposite(oldComposite);
        g2d.setColor(Color.red);
        for (Point cell : cells) {
            g2d.drawRect(xOffset + cell.x * tileSize,
                    yOffset + (rows - 1 - cell.y) * tileSize, tileSize, tileSize);
        }
    }

    private java.util.List<Point> getCurrentShapeCells() {
        switch (editMode) {
            case MODE_SHAPE_RECT:
                return canUseSmartTools()
                        ? getRectFillCells(shapeStart, shapeEnd)
                        : getRectOutlineCells(shapeStart, shapeEnd);
            case MODE_SHAPE_ELLIPSE:
                return canUseSmartTools()
                        ? getEllipseFillCells(shapeStart, shapeEnd)
                        : getEllipseOutlineCells(shapeStart, shapeEnd);
            default:
                return canUseSmartTools()
                        ? MapSelection.orthogonalLine(shapeStart, shapeEnd)
                        : MapSelection.bresenham(shapeStart, shapeEnd);
        }
    }

    private void drawSmartShapePreview(Graphics g, java.util.List<Point> cells,
                                       int xOffset, int yOffset) {
        MapGrid grid = handler.getMapMatrix().getMapAndCreate(shapeMap).getGrid();
        int[][] tileLayer = grid.tileLayers[handler.getActiveLayerIndex()];
        boolean[][] mask = buildSmartShapeMask(cells, tileLayer);
        int[][] resolved = editMode == EditMode.MODE_LINE
                ? handler.getSmartGridSelected().resolvePath(cells,
                        handler.getTileIndexSelected(), cols, rows, smartShapeInverted)
                : handler.getSmartGridSelected().resolveMask(mask, smartShapeInverted);
        Graphics2D g2d = (Graphics2D) g;
        java.awt.Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.6f));
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (!mask[x][y] || resolved[x][y] < 0) {
                    continue;
                }
                int tileIndex = resolved[x][y];
                if (tileIndex >= 0 && tileIndex < handler.getTileset().size()) {
                    Tile tile = handler.getTileset().get(tileIndex);
                    g.drawImage(tile.getThumbnail(), xOffset + x * tileSize,
                            yOffset + (rows - 1 - y - (tile.getHeight() - 1)) * tileSize, null);
                }
            }
        }
        g2d.setComposite(oldComposite);
        g2d.setColor(Color.red);
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (mask[x][y] && resolved[x][y] >= 0) {
                    g2d.drawRect(xOffset + x * tileSize,
                            yOffset + (rows - 1 - y) * tileSize, tileSize, tileSize);
                }
            }
        }
    }

    /* -------------------- Context menu / mode helpers -------------------- */

    protected void showSelectionPopup(MouseEvent e) {
        if (selectionPopupMenu == null) {
            selectionPopupMenu = new javax.swing.JPopupMenu();

            javax.swing.JMenuItem miMove = new javax.swing.JMenuItem("Move Selection");
            miMove.setName("selectionItem");
            miMove.addActionListener(evt -> {
                setEditMode(EditMode.MODE_MOVE_SELECT);
                handler.getMainFrame().getJtbModeMoveSelect().setSelected(true);
            });
            selectionPopupMenu.add(miMove);
            selectionPopupMenu.addSeparator();

            javax.swing.JMenuItem miRotate = new javax.swing.JMenuItem("Rotate 90\u00b0 Clockwise");
            miRotate.setName("selectionItem");
            miRotate.addActionListener(evt -> rotateSelection90());
            selectionPopupMenu.add(miRotate);

            javax.swing.JMenuItem miFlipH = new javax.swing.JMenuItem("Flip Horizontal");
            miFlipH.setName("selectionItem");
            miFlipH.addActionListener(evt -> flipSelectionHorizontal());
            selectionPopupMenu.add(miFlipH);

            javax.swing.JMenuItem miFlipV = new javax.swing.JMenuItem("Flip Vertical");
            miFlipV.setName("selectionItem");
            miFlipV.addActionListener(evt -> flipSelectionVertical());
            selectionPopupMenu.add(miFlipV);
            selectionPopupMenu.addSeparator();

            javax.swing.JMenuItem miCut = new javax.swing.JMenuItem("Cut");
            miCut.setName("selectionItem");
            miCut.addActionListener(evt -> cutSelection());
            selectionPopupMenu.add(miCut);

            javax.swing.JMenuItem miCopy = new javax.swing.JMenuItem("Copy");
            miCopy.setName("selectionItem");
            miCopy.addActionListener(evt -> copySelection());
            selectionPopupMenu.add(miCopy);

            javax.swing.JMenuItem miPaste = new javax.swing.JMenuItem("Paste");
            miPaste.setName("pasteItem");
            miPaste.addActionListener(evt -> startPaste());
            selectionPopupMenu.add(miPaste);

            javax.swing.JMenuItem miDelete = new javax.swing.JMenuItem("Delete");
            miDelete.setName("selectionItem");
            miDelete.addActionListener(evt -> deleteSelection());
            selectionPopupMenu.add(miDelete);

            javax.swing.JMenuItem miFill = new javax.swing.JMenuItem("Fill with Selected Tile");
            miFill.setName("selectionItem");
            miFill.addActionListener(evt -> fillSelection());
            selectionPopupMenu.add(miFill);
            selectionPopupMenu.addSeparator();

            //No name on purpose: Deselect stays enabled whenever the menu is
            //shown, even when the cursor is outside the selection
            javax.swing.JMenuItem miDeselect = new javax.swing.JMenuItem("Deselect");
            miDeselect.addActionListener(evt -> deselect());
            selectionPopupMenu.add(miDeselect);
        }

        boolean selectionActions = isCursorInsideSelection(e);
        for (java.awt.Component c : selectionPopupMenu.getComponents()) {
            if (!(c instanceof javax.swing.JMenuItem)) {
                continue;
            }
            javax.swing.JMenuItem item = (javax.swing.JMenuItem) c;
            if ("pasteItem".equals(item.getName())) {
                item.setEnabled(handler.hasRegionClipboard());
            } else if ("selectionItem".equals(item.getName())) {
                item.setEnabled(selectionActions);
            }
        }
        selectionPopupMenu.show(this, e.getX(), e.getY());
    }

    public void toggleSelectMode() {
        if (editMode == EditMode.MODE_SELECT) {
            setEditMode(EditMode.MODE_EDIT);
            handler.getMainFrame().getJtbModeEdit().setSelected(true);
        } else {
            setEditMode(EditMode.MODE_SELECT);
            handler.getMainFrame().getJtbModeSelect().setSelected(true);
        }
    }


    public void setCameraAtMap(Point mapCoods) {
        cameraX = mapCoods.x * cols;
        cameraY = -mapCoods.y * cols;

        viewMode.setCameraAtMap(this);

        /*
        if (orthoEnabled) {
            setOrthoView();
        } else {
            set3DView();
        }*/
    }

    public void setCameraAtSelectedMap() {
        setCameraAtMap(handler.getMapSelected());
    }

    public void setCameraAtMapIfExists(Point mapCoords) {
        if (handler.mapExists(mapCoords)) {
            setCameraAtMap(mapCoords);
        }
    }

    public void setCameraAtNextMapAndSelect(Point displacement) {
        Point mapSelected = handler.getMapSelected();
        Point nextMap = new Point(mapSelected.x + displacement.x, mapSelected.y + displacement.y);
        if (handler.mapExists(nextMap)) {
            setCameraAtMap(nextMap);
            handler.setMapSelected(nextMap);
        }
    }

    public void moveCamera(MouseEvent e) {
        cameraX -= (((float) ((e.getX() - lastMouseX))) / getWidth()) / (orthoScale / (cols + 2 * borderSize));
        cameraY += (((float) ((e.getY() - lastMouseY))) / getHeight()) / (orthoScale / (rows + 2 * borderSize));
        targetX = cameraX;
        targetY = cameraY;
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    public void setHandler(MapEditorHandler handler) {
        this.handler = handler;
    }

    public void requestUpdate() {
        updateRequested = true;
    }

    protected void invertLayerState(int index) {
        if (!handler.renderLayers[index]) {
            handler.setActiveTileLayer(index);
        }
        handler.renderLayers[index] = !handler.renderLayers[index];
        handler.getMainFrame().repaintThumbnailLayerSelector();
    }

    public void setOrthoView() {
        viewMode = ViewMode.VIEW_ORTHO_MODE;
        handler.getMainFrame().getJtbViewOrtho().setSelected(true);

        //orthoScale = 1.0f;
        cameraRotX = 0.0f;
        cameraRotY = 0.0f;
        cameraRotZ = 0.0f;

        cameraZ = 32.0f;

        handler.getMainFrame().setOrthoToolsEnabled(true);

        handler.getMainFrame().updateMapDisplaySize();
    }

    public void set3DView() {
        viewMode = ViewMode.VIEW_3D_MODE;
        handler.getMainFrame().getJtbView3D().setSelected(true);

        clearSelection();
        pasting = false;

        cameraRotX = defaultCamRotX;
        cameraRotY = defaultCamRotY;
        cameraRotZ = defaultCamRotZ;

        cameraZ = 40.0f;

        handler.getMainFrame().setOrthoToolsEnabled(false);

        if (editMode != EditMode.MODE_EDIT && editMode != EditMode.MODE_MOVE && editMode != EditMode.MODE_ZOOM) {
            handler.getMainFrame().getJtbModeEdit().setSelected(true);
            setEditMode(EditMode.MODE_EDIT);
        }

        handler.getMainFrame().updateMapDisplaySize();
    }

    public void setHeightView() {
        viewMode = ViewMode.VIEW_HEIGHT_MODE;
        handler.getMainFrame().getJtbViewHeight().setSelected(true);

        clearSelection();
        pasting = false;

        //orthoScale = 1.0f;
        cameraRotX = 0.0f;
        cameraRotY = 0.0f;
        cameraRotZ = 0.0f;

        cameraZ = 32.0f;

        handler.getMainFrame().setOrthoToolsEnabled(false);

        if (editMode != EditMode.MODE_EDIT && editMode != EditMode.MODE_MOVE && editMode != EditMode.MODE_ZOOM) {
            handler.getMainFrame().getJtbModeEdit().setSelected(true);
            setEditMode(EditMode.MODE_EDIT);
        }

        handler.getMainFrame().updateMapDisplaySize();
    }

    public void toggleGridView() {
        drawGridEnabled = !drawGridEnabled;
    }

    public void disableGridView() {
        drawGridEnabled = false;
    }

    public void setGridEnabled(boolean enable) {
        this.drawGridEnabled = enable;
    }

    public boolean isGridEnabled() {
        return drawGridEnabled;
    }

    public void toggleSmartGrid() {
        if (editMode == EditMode.MODE_SMART_PAINT) {
            setEditMode(EditMode.MODE_EDIT);
            handler.getMainFrame().getJtbModeEdit().setSelected(true);
        } else {
            setEditMode(EditMode.MODE_SMART_PAINT);
            handler.getMainFrame().getJtbModeSmartPaint().setSelected(true);
        }
    }

    protected void disableSmartGrid() {
        if (editMode == EditMode.MODE_SMART_PAINT) {
            setEditMode(EditMode.MODE_EDIT);
            handler.getMainFrame().getJtbModeEdit().setSelected(true);
        } else {
            setEditMode(EditMode.MODE_SMART_PAINT);
            handler.getMainFrame().getJtbModeSmartPaint().setSelected(true);
        }
    }

    public void toggleClearTile() {
        if (editMode == EditMode.MODE_CLEAR) {
            setEditMode(EditMode.MODE_EDIT);
            handler.getMainFrame().getJtbModeEdit().setSelected(true);
        } else {
            setEditMode(EditMode.MODE_CLEAR);
            handler.getMainFrame().getJtbModeClear().setSelected(true);
        }
    }

    public float getAspectRatio() {
        return (float) getWidth() / getHeight();
    }

    public boolean isSphereInsideFrustum(Vec3f spherePos, float radius, Vec3f[][] frustum){
        for(Vec3f[] plane : frustum){
            float distance = distPointPlaneSigned(spherePos, plane);
            if(distance < -radius){
                return false;
            }else if(distance < radius){
                //return false;
            }
        }
        return true;
    }

    public HashMap<Point, MapData> getMapsInsideFrustum(Vec3f[][] frustum){
        float radius = (float) Math.sqrt((MapGrid.cols * MapGrid.cols) / 2.0f);
        HashMap<Point, MapData> maps = new HashMap<Point, MapData>();
        for (HashMap.Entry<Point, MapData> map : handler.getMapMatrix().getMatrix().entrySet()) {
            Point p = map.getKey();
            Vec3f center = new Vec3f(p.x * MapGrid.cols, -p.y * MapGrid.rows, 0.0f);
            if(isSphereInsideFrustum(center, radius, frustum)){
                maps.put(map.getKey(), map.getValue());
            }
        }
        return maps;
    }

    public HashSet<Point> getGridBorderMapsInsideFrustum(Vec3f[][] frustum){
        float radius = (float) Math.sqrt((MapGrid.cols * MapGrid.cols) / 2.0f);
        HashSet<Point> borderMaps = new HashSet<>();
        for (Point borderMap : handler.getMapMatrix().getBorderMaps()) {
            Vec3f center = new Vec3f(borderMap.x * MapGrid.cols, -borderMap.y * MapGrid.rows, 0.0f);
            if(isSphereInsideFrustum(center, radius, frustum)){
                borderMaps.add(borderMap);
            }
        }
        return borderMaps;
    }

    boolean checkOpenGLError() {
        GL2 gl = (GL2) GLContext.getCurrentGL();
        boolean foundError = false;
        GLU glu = new GLU();
        int glErr = gl.glGetError();
        while (glErr != GL_NO_ERROR) {
            System.err.println("glError: " + glu.gluErrorString(glErr));
            foundError = true;
            glErr = gl.glGetError();
        }
        return foundError;
    }

    public GLContext getGLContext() {
        return GLContext.getCurrent();
    }

    protected void drawScreenshot(GL2 gl) {

        float xScale = (float) getWidth() / width;
        float yScale = (float) getHeight() / height;
        final int mapWidth = (int) (cols * tileSize * xScale);
        final int mapHeight = (int) (rows * tileSize * yScale);
        BufferedImage upscaledImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = upscaledImage.getGraphics();

        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(mapWidth * mapHeight * 4);

        gl.glReadBuffer(GL_BACK);
        gl.glReadPixels((int) (borderSize * tileSize * xScale), (int) (borderSize * tileSize * yScale),
                mapWidth, mapHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        for (int h = 0; h < mapHeight; h++) {
            for (int w = 0; w < mapWidth; w++) {
                graphics.setColor(new Color((buffer.get() & 0xff), (buffer.get() & 0xff),
                        (buffer.get() & 0xff)));
                buffer.get();
                graphics.drawRect(w, mapHeight - 2 - h, 1, 1);
            }
        }

        /*
        BufferedImage upscaledImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = upscaledImage.createGraphics();
        paint(g);
        g.dispose();*/

        screenshot = Utils.resize(upscaledImage, cols * tileSize, rows * tileSize, Image.SCALE_FAST);
    }

    public void drawBackImage(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        Point p = handler.getMapSelected();
        g2d.setComposite(AlphaComposite.SrcOver.derive(backImageAlpha));
        g2d.drawImage(backImage,
                borderSize * tileSize + p.x * cols * tileSize,
                borderSize * tileSize + p.y * rows * tileSize, null);
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }

    public void updateLastMapState() {
        try {
            MapLayerState state = (MapLayerState) handler.getMapStateHandler().getLastState();
            state.updateState();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void changeLayerWithNumKey(KeyEvent e, int layerIndex) {
        if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
            handler.renderLayers[layerIndex] = !handler.renderLayers[layerIndex];
        } else {
            handler.setActiveTileLayer(layerIndex);
        }
        handler.getMainFrame().getThumbnailLayerSelector().repaint();
    }

    private void lighting(GL2 gl) {
        //gl.glEnable(GL2.GL_LIGHTING);
        //gl.glEnable (GL2.GL_COLOR_MATERIAL ) ;
        //gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[]{1.0f, 1.0f, 1.0f, 0.0f}, 0);
        //gl.glLightModelfv(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, new float[]{1.0f, 0.0f, 1.0f, 0.0f}, 0);
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, new float[]{1.0f, 0.0f, 1.0f, 0.0f}, 0);

        //gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[]{0.2f, 0.2f, 0.2f, 0.0f}, 0);
        //gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 55.0f);

        gl.glEnable(GL2.GL_LIGHT0);
        //gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, new float[]{1.0f, 1.0f, 1.0f, 0.0f}, 0);
        //gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{-1.0f, 1.0f, 1.0f, 0.0f}, 0);
        //gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);
        //gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{1.0f, 1.0f, 1.0f, 0.0f}, 0);

        gl.glEnable(GL2.GL_LIGHT1);
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, new float[]{0.8f, 0.8f, 0.8f, 0.0f}, 0);
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, new float[]{1.0f, -1.0f, -1.0f, 1.0f}, 0);

    }

    public BufferedImage captureOrthoMapPreview(Point mapCoords) {
        GLContext context = getContext();
        if (context == null) {
            return null;
        }

        boolean contextCurrent = false;
        try {
            contextCurrent = context.makeCurrent() != GLContext.CONTEXT_NOT_CURRENT;
            if (!contextCurrent) {
                return null;
            }
            return captureOrthoMapPreview(getGL().getGL2(), mapCoords);
        } finally {
            if (contextCurrent) {
                context.release();
            }
        }
    }

    private BufferedImage captureOrthoMapPreview(GL2 gl, Point mapCoords) {
        ViewMode previousViewMode = viewMode;
        BufferedImage previousScreenshot = screenshot;
        boolean previousScreenshotRequested = screenshotRequested;
        float previousCameraX = cameraX;
        float previousCameraY = cameraY;
        float previousCameraZ = cameraZ;
        float previousTargetX = targetX;
        float previousTargetY = targetY;
        float previousTargetZ = targetZ;
        float previousCameraRotX = cameraRotX;
        float previousCameraRotY = cameraRotY;
        float previousCameraRotZ = cameraRotZ;
        float previousOrthoScale = orthoScale;
        boolean previousDrawGridEnabled = drawGridEnabled;
        boolean previousDrawWireframeEnabled = drawWireframeEnabled;
        boolean previousDrawGridBorderMaps = drawGridBorderMaps;

        int[] previousViewport = new int[4];
        int[] previousFramebuffer = new int[1];
        int[] previousRenderbuffer = new int[1];
        int[] previousReadBuffer = new int[1];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, previousViewport, 0);
        gl.glGetIntegerv(GL2.GL_FRAMEBUFFER_BINDING, previousFramebuffer, 0);
        gl.glGetIntegerv(GL2.GL_RENDERBUFFER_BINDING, previousRenderbuffer, 0);
        gl.glGetIntegerv(GL2.GL_READ_BUFFER, previousReadBuffer, 0);

        int[] framebuffer = new int[1];
        int[] colorBuffer = new int[1];
        int[] depthBuffer = new int[1];
        int frameWidth = width;
        int frameHeight = height;
        int mapWidth = cols * tileSize;
        int mapHeight = rows * tileSize;

        try {
            gl.glGenFramebuffers(1, framebuffer, 0);
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, framebuffer[0]);

            gl.glGenRenderbuffers(1, colorBuffer, 0);
            gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, colorBuffer[0]);
            gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, frameWidth, frameHeight);
            gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
                    GL2.GL_RENDERBUFFER, colorBuffer[0]);

            gl.glGenRenderbuffers(1, depthBuffer, 0);
            gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depthBuffer[0]);
            gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT24, frameWidth, frameHeight);
            gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
                    GL2.GL_RENDERBUFFER, depthBuffer[0]);

            if (gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE) {
                return null;
            }

            gl.glViewport(0, 0, frameWidth, frameHeight);
            gl.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0);

            viewMode = ViewMode.VIEW_ORTHO_MODE;
            cameraX = mapCoords.x * cols;
            cameraY = -mapCoords.y * cols;
            cameraZ = 32.0f;
            targetX = cameraX;
            targetY = cameraY;
            targetZ = cameraZ;
            cameraRotX = 0.0f;
            cameraRotY = 0.0f;
            cameraRotZ = 0.0f;
            orthoScale = 1.0f;
            drawGridEnabled = false;
            drawWireframeEnabled = false;
            drawGridBorderMaps = false;
            screenshotRequested = false;

            renderMapScene(gl);
            return readFramebufferImage(gl, borderSize * tileSize, borderSize * tileSize, mapWidth, mapHeight);
        } finally {
            viewMode = previousViewMode;
            cameraX = previousCameraX;
            cameraY = previousCameraY;
            cameraZ = previousCameraZ;
            targetX = previousTargetX;
            targetY = previousTargetY;
            targetZ = previousTargetZ;
            cameraRotX = previousCameraRotX;
            cameraRotY = previousCameraRotY;
            cameraRotZ = previousCameraRotZ;
            orthoScale = previousOrthoScale;
            drawGridEnabled = previousDrawGridEnabled;
            drawWireframeEnabled = previousDrawWireframeEnabled;
            drawGridBorderMaps = previousDrawGridBorderMaps;
            screenshot = previousScreenshot;
            screenshotRequested = previousScreenshotRequested;

            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, previousFramebuffer[0]);
            gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, previousRenderbuffer[0]);
            gl.glReadBuffer(previousReadBuffer[0]);
            gl.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);

            if (depthBuffer[0] != 0) {
                gl.glDeleteRenderbuffers(1, depthBuffer, 0);
            }
            if (colorBuffer[0] != 0) {
                gl.glDeleteRenderbuffers(1, colorBuffer, 0);
            }
            if (framebuffer[0] != 0) {
                gl.glDeleteFramebuffers(1, framebuffer, 0);
            }
        }
    }

    private BufferedImage readFramebufferImage(GL2 gl, int x, int y, int imageWidth, int imageHeight) {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(imageWidth * imageHeight * 4);
        gl.glReadPixels(x, y, imageWidth, imageHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        for (int row = 0; row < imageHeight; row++) {
            for (int col = 0; col < imageWidth; col++) {
                int red = buffer.get() & 0xff;
                int green = buffer.get() & 0xff;
                int blue = buffer.get() & 0xff;
                buffer.get();
                image.setRGB(col, imageHeight - 1 - row, (red << 16) | (green << 8) | blue);
            }
        }

        return image;
    }

    public void requestScreenshot() {
        screenshotRequested = true;
    }

    public BufferedImage getScreenshot() {
        return screenshot;
    }

    public void setBackImage(BufferedImage backImage) {
        this.backImage = backImage;
    }

    public void toggleBackImageView() {
        backImageEnabled = !backImageEnabled;
    }

    public void setBackImageEnabled(boolean enabled) {
        this.backImageEnabled = enabled;
    }

    public void setHeightMapAlpha(float alpha) {
        this.heightMapOpacity = alpha;
    }

    public void setBackImageAlpha(float alpha) {
        this.backImageAlpha = alpha;
    }

    public void setSmartToolsEnabled(boolean enabled) {
        smartToolsEnabled = enabled;
        repaint();
    }

    public boolean isSmartToolsEnabled() {
        return smartToolsEnabled;
    }

    public void setAutoCollisionEnabled(boolean enabled) {
        autoCollisionEnabled = enabled;
    }

    public boolean isAutoCollisionEnabled() {
        return autoCollisionEnabled;
    }

    private void applyAutoCollision(Point mapCoords) {
        if (!autoCollisionEnabled) {
            return;
        }
        MapData mapData = handler.getMapMatrix().getMapAndCreate(mapCoords);
        CollisionDefaultsApplier.apply(handler.getTileset(), mapData.getGrid(), mapData.getCollisions());
    }

    private void applyAutoCollision(Set<Point> mapCoords) {
        if (!autoCollisionEnabled) {
            return;
        }
        for (Point point : mapCoords) {
            applyAutoCollision(point);
        }
    }

    protected boolean canUseSmartTools() {
        return smartToolsEnabled && handler != null
                && !handler.getSmartGridArray().isEmpty()
                && !handler.getSmartGridSelected().getTileIndices().isEmpty();
    }

    public void setEditMode(EditMode mode) {
        if (editMode != mode) {
            if (floatingMove) {
                cancelFloatingMove();
            }
            resetShape();
            resetSmartStroke();
        }
        editMode = mode;
        if (mode != EditMode.MODE_SELECT) {
            pasting = false;
        }
        setCursor(editMode.cursor);
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setDrawAreasEnabled(boolean drawAreas) {
        this.drawAreasEnabled = drawAreas;
    }

    public void setDrawWireframeEnabled(boolean drawWireframeEnabled) {
        this.drawWireframeEnabled = drawWireframeEnabled;
    }

    public void setDrawGridBorderMaps(boolean drawGridBorderMaps) {
        this.drawGridBorderMaps = drawGridBorderMaps;
    }

    protected static interface DrawGeometryGLFunction {

        public void draw(GL2 gl, GeometryGL geometryGL, ArrayList<Texture> textures);
    }

}
