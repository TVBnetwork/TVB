package engineTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import entities.Camera;
import entities.Entity;
import entities.Light;
import entities.Player;
import guis.GuiRenderer;
import guis.GuiTexture;
import models.TexturedModel;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import renderEngine.OBJLoader;
import terrains.Terrain;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import water.WaterFrameBuffers;
import water.WaterRenderer;
import water.WaterShader;
import water.WaterTile;

public class MainGameLoop {

    public static void main(String[] args) {

        DisplayManager.createDisplay();
        Loader loader = new Loader();

        // *********TERRAIN TEXTURE STUFF***********
        TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy2"));
        TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("mud"));
        TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("mud"));
        TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("mud"));

        TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
        TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
        Terrain terrain = new Terrain(0,-1,loader, texturePack, blendMap, "heightMap");

        List <Terrain> terrains = new ArrayList<Terrain>();
        terrains.add(terrain);

        // *****************************************

        TexturedModel tree = new TexturedModel(OBJLoader.loadObjModel("pine", loader), new ModelTexture(loader.loadTexture("pine")));

        ModelTexture fernTexture = new ModelTexture(loader.loadTexture("fern"));
        fernTexture.setNumberOfRows(2);
        TexturedModel fern = new TexturedModel(OBJLoader.loadObjModel("fern", loader), fernTexture);


        tree.getTexture().setReflectivity(0);
        fern.getTexture().setHasTransparency(true);
        fern.getTexture().setUseFakeLighting(false);
        fern.getTexture().setReflectivity(0);

        List <Light> lights = new ArrayList<Light>();

        List<Entity> entities = new ArrayList<Entity>();

        float x=0, y=0, z=0;

        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            if (i % 7 == 0) {
                x = random.nextFloat() * 800;
                z = random.nextFloat() * -800;
                y = terrain.getHeightOfTerrain(x, z);

                entities.add(new Entity(fern, random.nextInt(4), new Vector3f(x, y, z), 0, random.nextFloat() * 360, 0, 4.5f));
            }

            if (i % 3 == 0) {
                x = random.nextFloat() * 800;
                z = random.nextFloat() * -800;
                y = terrain.getHeightOfTerrain(x, z);

                entities.add(new Entity(tree, new Vector3f(x, y, z), 0, 0, 0, random.nextFloat() + 5));
            }
        }
        TexturedModel rock = new TexturedModel(OBJLoader.loadObjModel("rocks", loader), new ModelTexture(loader.loadTexture("rocks")));
        entities.add(new Entity(rock, 0, new Vector3f(400, 5,-400), 0, 0, 0, 400f));
        rock.getTexture().setReflectivity(0);
        rock.getTexture().setHasTransparency(true);
        rock.getTexture().setUseFakeLighting(false);

        GuiRenderer guiRenderer = new GuiRenderer(loader);

        List<GuiTexture> mainGUI = new ArrayList<GuiTexture>();
        List<GuiTexture> guiMap = new ArrayList<GuiTexture>();

        GuiTexture gui1 = new GuiTexture(loader.loadTexture("socuwan"), new Vector2f(0.5f, 0.5f), new Vector2f(0.25f, 0.25f));
        GuiTexture gui2 = new GuiTexture(loader.loadTexture("thinmatrix"), new Vector2f(0.30f, 0.58f), new Vector2f(0.4f, 0.4f));
        GuiTexture gui3 = new GuiTexture(loader.loadTexture("health"), new Vector2f(-0.74f, 0.925f), new Vector2f(0.25f, 0.25f));
        mainGUI.add(gui1);
        mainGUI.add(gui2);
        mainGUI.add(gui3);

        Light sun = new Light(new Vector3f(1000,1000,1000), new Vector3f(1.0f,1.0f,1.0f));
        lights.add(sun); // main sun light
        Light sunBottom = new Light(new Vector3f(400,-2000,-400), new Vector3f(1.0f,1.0f,1.0f));
        lights.add(sunBottom); // main sun light

        TexturedModel avatar = new TexturedModel(OBJLoader.loadObjModel("player",  loader), new ModelTexture(loader.loadTexture("playerTexture")));

        Player player = new Player(avatar, new Vector3f(400,0,-400), 0,180,0,1);
        Camera camera = new Camera(player);

        MasterRenderer renderer = new MasterRenderer(loader,0);

        WaterShader waterShader = new WaterShader();
        WaterRenderer waterRenderer = new WaterRenderer(loader, waterShader, renderer.getProjectionMatrix());
        List<WaterTile> waters = new ArrayList<WaterTile>();
        WaterTile water = new WaterTile(400, -400, 0);
        waters.add(water);


        WaterFrameBuffers fbos = new WaterFrameBuffers();
        GuiTexture refraction = new GuiTexture(fbos.getRefractionDepthTexture(), new Vector2f(0.5f, 0.5f), new Vector2f(0.25f, 0.25f));
        GuiTexture reflection = new GuiTexture(fbos.getReflectionTexture(), new Vector2f(-0.5f, 0.5f), new Vector2f(0.25f, 0.25f));
        guiMap.add(refraction);
        guiMap.add(reflection);

        while(!Display.isCloseRequested()) {
            camera.move();
            player.move(terrain);

            GL11.glEnable(GL30.GL_CLIP_DISTANCE0);

            fbos.bindReflectionFrameBuffer();
            renderer.renderScene(entities, terrains, lights, camera, player, new Vector4f(0, 1, 0, -water.getHeight()));

            fbos.bindRefractionFrameBuffer();
            renderer.renderScene(entities, terrains, lights, camera, player, new Vector4f(0, -1, 0, water.getHeight()));

            GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
            fbos.unbindCurrentFrameBuffer();

            renderer.renderScene(entities, terrains, lights, camera, player, new Vector4f(0, 1, 0, 100000)); //<-- hack to make sure nothing ever gets clipped
            waterRenderer.render(waters, camera);
            //guiRenderer.render(mainGUI);
            guiRenderer.render(guiMap);
            DisplayManager.updateDisplay();
        }

        fbos.cleanUp();
        guiRenderer.cleanUp();
        renderer.cleanUp();
        loader.cleanUp();

        DisplayManager.closeDisplay();
    }

}
