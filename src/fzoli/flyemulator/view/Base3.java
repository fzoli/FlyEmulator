package fzoli.flyemulator.view;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.water.ProjectedGrid;
import com.ardor3d.extension.effect.water.WaterHeightGenerator;
import com.ardor3d.extension.effect.water.WaterNode;
import com.ardor3d.extension.model.obj.ObjGeometryStore;
import com.ardor3d.extension.model.obj.ObjImporter;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainDataProvider;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvas;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.logical.AnyKeyCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.StereoCamera;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.jogl.JoglTextureRendererProvider;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.extension.Skybox;
import com.ardor3d.scenegraph.visitor.UpdateModelBoundVisitor;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardor3d.util.stat.StatCollector;
import fzoli.flyemulator.mp3.MP3;
import java.util.ArrayList;
 
public class Base3 implements Runnable, Updater, Scene {
    
        // { égbolt és víz
        private final double farPlane = 10000.0;
        private Skybox skybox;
        private WaterNode waterNode;
        // }
    
        // { hegység
        private Terrain terrain;
        private boolean terrainDebug = false;
        // }
        
        // { anaglif kamera objektumai, változói:
        private StereoCamera _camera;
	private ColorMaskState noRed, redOnly;
        private boolean _stereo;
	private static final boolean _sideBySide = false;
	private static final boolean _useAnaglyph = true;
        private MP3 bombMp3 = new MP3("bomb_down.mp3");
        private MP3 splashMp3 = new MP3("splash.mp3");
        private MP3 boomMp3 = new MP3("boom.mp3");
        // }
        
        //kigyulladás effekthez
        private ParticleSystem particles;
        private boolean destroyed = false;
        
        // { repülő és bomba + mozgatáshoz/forgatáshoz változók
        private Node fighter;
        private ArrayList<Node> bombs = new ArrayList<Node>();
        private boolean dropped = false;
        private double t2=-1*Math.PI/4,t3=0,t4=-1*Math.PI/2; //2PI = 360; PI = 180; PI/2 = 90 fok
        private double ttx=0,tty=50,ttz=0;
        // }
        
        private final String OBJ_PATH = "fzoli/flyemulator/obj";
        
	private final Timer timer = new Timer();
	private final FrameHandler frameHandler = new FrameHandler(timer);
 
	private final JoglCanvas canvas;
	private PhysicalLayer physicalLayer;
	
	private Vector3 worldUp = new Vector3(0, 1, 0);
 
	private final Node root = new Node();
 
	private final LogicalLayer logicalLayer = new LogicalLayer();
	private boolean exit;
 
	public Base3(DisplaySettings settings, boolean stereo) {
		JoglCanvasRenderer canvasRenderer = new JoglCanvasRenderer(this);
		canvas = new JoglCanvas(canvasRenderer, settings);
		TextureRendererFactory.INSTANCE.setProvider(new JoglTextureRendererProvider());
                physicalLayer = new PhysicalLayer(new AwtKeyboardWrapper(canvas), new AwtMouseWrapper(canvas, new AwtMouseManager(canvas)));
		logicalLayer.registerInput(canvas, physicalLayer);
		frameHandler.addUpdater(this);
		frameHandler.addCanvas(canvas);
		canvas.setTitle("Fly emulator - Farkas Zoltán - DZ54IQ");
                // { színek piros-cián anaglif szemüveghez állítása, ha kérik az anaglif megjelenítést
                if (_stereo = stereo) setAnaglyphColors(); //if-en belüli értékadás, nem öszehasonlítás!
                // }
	}

        private void setAnaglyphColors() {
            redOnly = new ColorMaskState();
	    redOnly.setAll(false);
            redOnly.setRed(true);
	    noRed = new ColorMaskState();
	    noRed.setAll(true);
	    noRed.setRed(false);
        }
        
	public void run() {
            try {
                frameHandler.init();
                while (!exit) {
                    frameHandler.updateFrame();
                    Thread.yield();
                }
                canvas.getCanvasRenderer().makeCurrentContext();
                ContextGarbageCollector.doFinalCleanup(canvas.getCanvasRenderer().getRenderer());
                canvas.close();
            }
            catch (final Throwable t) {
                System.err.println("Throwable caught in MainThread - exiting");
		t.printStackTrace(System.err);
            }
	}
        
        private Camera getCamera() {
            return canvas.getCanvasRenderer().getCamera();
        }
        
        private void setStereoCamera() {
            if (_stereo) {
                _camera = new StereoCamera(getCamera());
	        canvas.getCanvasRenderer().setCamera(_camera);
	        _camera.setFocalDistance(2.65); //1.0 esetén minden képernyőn befelé 3D, 2.65 esetén képernyőn kinyúlhatnak tárgyak
	        _camera.setEyeSeparation(_camera.getFocalDistance() / 65.0); //eredetileg 30; 65 helyett
	        _camera.setAperture(45.0 * MathUtils.DEG_TO_RAD);
	        _camera.setSideBySideMode(_sideBySide);
	        _camera.setupLeftRightCameras();
            }
        }
        
        private void loadObjects() {
            // repülő és bomba betöltése és megjelenítése
            fighter = importObj("fighter", "fighter.obj");
            Node bomb = importObj("fighter", "bomb.obj");
            bombs.add(bomb);
            //Node bomb2 = bomb.makeCopy(false);
            //root.attachChild(bomb2);
            fighter.setTranslation(ttx, tty, ttz);
            fighter.setRotation(new Quaternion().fromEulerAngles(t2, t3, t4));
            bomb.setRotation(fighter.getRotation());
        }
        
        private void dieFighterIfNeed() {
            if (!destroyed) {
                if (fighter.getTranslation().getY() <= waterNode.getWaterHeight()) {
                    destroyed = true;
                    splashMp3.play();
                    fighter.setRotation(new Quaternion().fromEulerAngles(Math.PI/4, 0, Math.PI/2));
                }
                double height = terrain.getHeightAt(fighter.getTranslation().getX(), fighter.getTranslation().getZ());
                if (fighter.getTranslation().getY() <= height) {
                    initFighterPartSystem();
                    destroyed = true;
                    boomMp3.play();
                }
            }
        }
        
        private void initFighterPartSystem() {
            initParticleSystem(fighter.getTranslation());
        }
        
        private void initParticleSystem(ReadOnlyVector3 point) { //kigyulladás effekt létrehozása
            particles = ParticleFactory.buildParticles("particles", 300);
	    particles.setEmissionDirection(new Vector3(0, 1, 0));
            particles.setInitialVelocity(.006);
	    particles.setStartSize(1.5);
	    particles.setEndSize(.5);
	    particles.setMinimumLifeTime(100);
	    particles.setMaximumLifeTime(1500);
	    particles.setStartColor(new ColorRGBA(1, 0, 0, 1));
	    particles.setEndColor(new ColorRGBA(0, 1, 0, 0));
	    particles.setMaximumAngle(360 * MathUtils.DEG_TO_RAD);
	    particles.getParticleController().setControlFlow(false);
	    particles.setParticlesInWorldCoords(true);
	
	    final BlendState blend = new BlendState();
	    blend.setBlendEnabled(true);
	    blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
	    blend.setDestinationFunction(BlendState.DestinationFunction.One);
	    particles.setRenderState(blend);
	
	    final TextureState ts = new TextureState();
	    ts.setTexture(TextureManager.load("fzoli/flyemulator/img/flaresmall.jpg", Texture.MinificationFilter.Trilinear,
	            TextureStoreFormat.GuessCompressedFormat, true));
	    ts.getTexture().setWrap(WrapMode.BorderClamp);
	    ts.setEnabled(true);
	    particles.setRenderState(ts);
	
            final ZBufferState zstate = new ZBufferState();
	    zstate.setWritable(false);
            particles.setRenderState(zstate);
	
	    particles.getParticleGeometry().setModelBound(new BoundingBox());

	    root.attachChild(particles);

            particles.setTranslation(point);

            particles.warmUp(60);
        }
        
        private Skybox createSkyBox() {
            Skybox skybox = new Skybox("skybox", 10, 10, 10);

            final String dir = "fzoli/flyemulator/img/skybox/";
            final Texture north = TextureManager
                    .load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
            final Texture south = TextureManager
                    .load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
            final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
            final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
            final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
            final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);

            skybox.setTexture(Skybox.Face.North, north);
            skybox.setTexture(Skybox.Face.West, west);
            skybox.setTexture(Skybox.Face.South, south);
            skybox.setTexture(Skybox.Face.East, east);
            skybox.setTexture(Skybox.Face.Up, up);
            skybox.setTexture(Skybox.Face.Down, down);
            return skybox;
    }
        
        public void init() {
            setStereoCamera();
            registerInputTriggers();
            
            AWTImageLoader.registerLoader();
            final ZBufferState buf = new ZBufferState();
            buf.setEnabled(true);
            buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
            root.setRenderState(buf);
            
            loadObjects();
            Node reflectedNode = initSkyAndWater();
            initTerrain(reflectedNode);
            
            setCameraLocation(ttx-20, tty+5, ttz+20);
            getCamera().lookAt(new Vector3(ttx, tty, ttz), Vector3.UNIT_Y);
        }
        
        private Node initSkyAndWater() {
            canvas.getCanvasRenderer().getCamera().setFrustumPerspective(
                    45.0,
                    (float) canvas.getCanvasRenderer().getCamera().getWidth()
                  / (float) canvas.getCanvasRenderer().getCamera().getHeight(), 1, farPlane);
            canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);
            
            String imgPath = "fzoli/flyemulator/img/";
            final CullState cullFrontFace = new CullState();
            cullFrontFace.setEnabled(true);
            cullFrontFace.setCullFace(CullState.Face.Back);
            root.setRenderState(cullFrontFace);

            final MaterialState ms = new MaterialState();
            ms.setColorMaterial(ColorMaterial.Diffuse);
            root.setRenderState(ms);

            fogState = setupFog();

            final Node reflectedNode = new Node("reflectNode");
            skybox = createSkyBox();
            reflectedNode.attachChild(skybox);
            root.attachChild(reflectedNode);

            final Camera cam = getCamera();

            waterNode = new WaterNode(cam, 4, true, true);
            waterNode.setClipBias(0.5f);
            waterNode.setWaterMaxAmplitude(5.0f);

            waterNode.setNormalMapTextureString(imgPath+"water/normalmap3.dds");
            waterNode.setDudvMapTextureString(imgPath+"water/dudvmap.png");
            waterNode.setFallbackMapTextureString(imgPath+"water/water2.png");
            waterNode.setFoamMapTextureString(imgPath+"water/oceanfoam.png");

            waterNode.setWaterPlane(new Plane(new Vector3(0.0, 1.0, 0.0), 0.0));

            ProjectedGrid projectedGrid = new ProjectedGrid("ProjectedGrid", cam, 100, 70, 0.01f, new WaterHeightGenerator(), timer);
            projectedGrid.setNrUpdateThreads(Runtime.getRuntime().availableProcessors());

            waterNode.attachChild(projectedGrid);

            waterNode.addReflectedScene(reflectedNode);
            waterNode.setSkybox(skybox);

            root.attachChild(waterNode);
            waterNode.reloadShader();

            root.acceptVisitor(new UpdateModelBoundVisitor(), false);    
            
            return reflectedNode;
        }
        
        private FogState setupFog() {
            final FogState fogState = new FogState();
            fogState.setDensity(1.0f);
            fogState.setEnabled(true);
            fogState.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
            fogState.setEnd((float) farPlane);
            fogState.setStart((float) farPlane / 10.0f);
            fogState.setDensityFunction(FogState.DensityFunction.Linear);
            fogState.setQuality(FogState.Quality.PerVertex);
            root.setRenderState(fogState);
            return fogState;
        }
        
        private void setCameraLocation(double x, double y, double z) {
            getCamera().setLocation(new Vector3(x, y, z));
        }
        
        private void initTerrain(Node reflectedNode) {
            try {
                final double scale = 1.0 / 4000.0;
                Function3D functionTmp = new FbmFunction3D(Functions.simplexNoise(), 9, 0.5, 0.5, 3.14);
                functionTmp = Functions.clamp(functionTmp, -1.2, 1.2);
                final Function3D function = Functions.scaleInput(functionTmp, scale, scale, 1);
                final TerrainDataProvider terrainDataProvider = new ProceduralTerrainDataProvider(function, new Vector3(1,
                        200, 1), -1.2f, 1.2f);
                terrain = new TerrainBuilder(terrainDataProvider, getCamera()).setShowDebugPanels(terrainDebug).build();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            reflectedNode.attachChild(terrain);
        }
        
        private Node importObj(String folder, String file) {
            String fullpath = OBJ_PATH + "/" + folder + "/";
            final ObjImporter importer = new ObjImporter();
	    try {
                importer.setTextureLocator(new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(Base3.class, fullpath)));
	    }
            catch (final Exception ex) {
	        ex.printStackTrace();
	    }
            ObjGeometryStore storage = importer.load(fullpath + file);
            Node node = storage.getScenegraph();
	    root.attachChild(node);
            return node;
        }
        
        private void setBombLocation(Node bomb) {
            ReadOnlyVector3 bt = bomb.getTranslation();
            boolean underwater = bt.getY() < waterNode.getWaterHeight();
            boolean underterrain = bt.getY() < terrain.getHeightAt(bt.getX(), bt.getZ());
            if (underwater) {
            }
            if (underterrain) {
                initParticleSystem(bt);
            }
            if (underterrain || underwater) {
                return;
            }
            bomb.setTranslation(bt.getX(), bt.getY() - 1, bt.getZ());
        }
        
        private double heightCounter = 0;
        
        private void updateLocations() {
        
            if (!destroyed) {
                ttx+=1;
                ttz-=1;
                
                if (t4 > -1 * Math.PI/2 + 0.5) t4 = -1 * Math.PI/2 + 0.5;
                if (t4 < -1 * Math.PI/2 - 0.5) t4 = -1 * Math.PI/2 - 0.5;
                
                double tmp = -1*Math.PI/2 - t4;
                heightCounter -= tmp;
                double ty = 2 * heightCounter;
                
                setCameraLocation(ttx-20, ty+tty+5, ttz+20);
                fighter.setTranslation(ttx, ty+tty, ttz);
                for (int i = 0; i < bombs.size(); i++) {
                    Node bomb = bombs.get(i);
                    if (i < bombs.size() -1) { //nem az utolsó
                        setBombLocation(bomb);
                    }
                    else { //az utolsó
                        if (!dropped) bomb.setTranslation(ttx, ty+tty, ttz);
                        else {
                            setBombLocation(bomb);
                        }
                    }
                    
                }
                
            }
        }
        
        public void update(ReadOnlyTimer timer) {
            if (canvas.isClosing()) {
		exit = true;
            }
 
            if (Constants.stats) {
                StatCollector.update();
            }
 
            logicalLayer.checkTriggers(timer.getTimePerFrame());
            GameTaskQueueManager.getManager(canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.UPDATE).execute();
            root.updateGeometricState(timer.getTimePerFrame(), true);

            updateSkyAndWater();
            updatePositions();
            updateLights();
            
            updateLocations();
            
            dieFighterIfNeed();
        }
        
        private FogState fogState;
        private boolean groundCamera, aboveWater = false;
        
        private void updatePositions() {
            final Camera camera = getCamera();

            final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ());
            if (groundCamera || camera.getLocation().getY() < height) {
                camera.setLocation(new Vector3(camera.getLocation().getX(), height, camera.getLocation().getZ()));
            }
        }
        
        private void updateLights() {
            final Camera camera = getCamera();

            if (aboveWater && camera.getLocation().getY() < waterNode.getWaterHeight()) {
                fogState.setStart(-1000f);
                fogState.setEnd((float)farPlane / 10f);
                fogState.setColor(new ColorRGBA(0.0f, 0.0f, 0.1f, 1.0f));
                aboveWater = false;
            } else if (!aboveWater && camera.getLocation().getY() >= waterNode.getWaterHeight()) {
                fogState.setStart((float)farPlane / 2.0f);
                fogState.setEnd((float)farPlane);
                fogState.setColor(new ColorRGBA(0.96f, 0.97f, 1.0f, 1.0f));
                aboveWater = true;
            }
        }
        
        private void updateSkyAndWater() {                   
            final Camera cam = canvas.getCanvasRenderer().getCamera();

            skybox.setTranslation(cam.getLocation());
            skybox.updateGeometricState(0.0f, true);

            waterNode.update(timer.getTimePerFrame());
        }
        
	public PickResults doPick(Ray3 pickRay) {
		return null; //minek ez nekem?:D
	}
 
	public boolean renderUnto(Renderer renderer) {
	     if (_stereo) {   
	        _camera.updateLeftRightCameraFrames();

	        // BAL SZEM
	        {
                    if (!_sideBySide && !_useAnaglyph) {
                        renderer.setDrawBuffer(DrawBufferTarget.BackLeft);
                        renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
                    } else if (_useAnaglyph) {
                        renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
                        ContextManager.getCurrentContext().enforceState(redOnly);
                    }
                    _camera.switchToLeftCamera(renderer);	
                    renderer.draw(root);
                    renderer.renderBuckets();
	        }
	
	        // JOBB SZEM
	        {
                    if (!_sideBySide && !_useAnaglyph) {
                        renderer.setDrawBuffer(DrawBufferTarget.BackRight);
                        renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
                    } else if (_useAnaglyph) {
                        renderer.clearBuffers(Renderer.BUFFER_DEPTH);
                        ContextManager.getCurrentContext().enforceState(noRed);
                    }
                    _camera.switchToRightCamera(renderer);
                    renderer.draw(root);
                    renderer.renderBuckets();
	        }
	
	        if (_useAnaglyph) {
	            ContextManager.getCurrentContext().clearEnforcedState(StateType.ColorMask);
	        }
                return true;
             }
             else {
                 GameTaskQueueManager.getManager(canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.RENDER).execute(renderer);
		 ContextGarbageCollector.doRuntimeCleanup(renderer);
		 if (!canvas.isClosing()) {
		     renderer.draw(root);
                     return true;
		 } else {
                     return false;
		 }
             }
	}
         
	protected void registerInputTriggers() {
		//FirstPersonControl.setupTriggers(logicalLayer, worldUp, true); //TODO: saját eseménykezelő kellene de NINCS RÁ IDŐ
                
                // { MOZGÁS/FORGÁS TESZT:
		logicalLayer.registerTrigger(new InputTrigger(new AnyKeyCondition(), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {

                                char c = inputState.getCurrent().getKeyboardState().getKeyEvent().getKeyChar();
                                KeyboardState ks = inputState.getCurrent().getKeyboardState();

                                if (ks.isDown(Key.ESCAPE)) System.exit(0);
                                
                                double d = 0.04;
                                if (ks.isDown(Key.UP)) t4 -= d;
                                if (ks.isDown(Key.DOWN)) t4 += d;
                                if (!destroyed && ks.isDown(Key.SPACE)) {
                                    if (!dropped) {
                                        dropped = true;
                                        Node b = bombs.get(bombs.size() -1);
                                        Node b2 = b.makeCopy(false);
                                        bombs.add(b2);
                                        bombMp3.play();
                                    }
                                    else {
                                        dropped = false;
                                        root.attachChild(bombs.get(bombs.size()-1));
                                    }
                                }

                                switch (c) {
                                    case 'q':
                                        destroyed = false;
                                        heightCounter = 0;
                                        ttx=ttz=0;
                                        tty=50;

                                        t2=-1*Math.PI/4;
                                        t3=0;
                                        t4=-1*Math.PI/2;

                                        setCameraLocation(ttx-20, tty+5, ttz+20);
                                        getCamera().lookAt(new Vector3(ttx, tty, ttz), Vector3.UNIT_Y);

                                        bombs.get(bombs.size() - 1).setTranslation(ttx, tty, ttz);
                                        break;
                                    case 'á':
                                        initFighterPartSystem();
                               }
                               if (!destroyed) {
                                    Quaternion q = new Quaternion().fromEulerAngles(t2, t3, t4);
                                    fighter.setRotation(q);
                                    if (!dropped) bombs.get(bombs.size() - 1).setRotation(q);
                                }
			}
		}));
                // }
	}

}