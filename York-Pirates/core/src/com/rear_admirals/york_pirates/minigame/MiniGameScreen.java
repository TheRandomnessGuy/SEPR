package com.rear_admirals.york_pirates.minigame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.rear_admirals.york_pirates.PirateGame;
import com.rear_admirals.york_pirates.base.BaseActor;
import com.rear_admirals.york_pirates.base.BaseScreen;
import com.rear_admirals.york_pirates.screen.SailingScreen;

import java.util.ArrayList;

import static com.rear_admirals.york_pirates.College.Derwent;

public class MiniGameScreen extends BaseScreen {
    //Map Variables.
    private int tileSize = 64;
    private int tileCountWidth = 30;
    private int tileCountHeight = 30;
    private final int mapWidth = tileSize * tileCountWidth;
    private final int mapHeight = tileSize * tileCountHeight;
    private MiniGamePlayer player;
    private ArrayList<MiniGameEnemy> enemies;
    private ArrayList<BaseActor> wallList;
    private BaseActor exit;

    //In-Class map and game status storage.
    private boolean[][] isWall = new boolean[30][30];
    private boolean[][] isExit = new boolean[30][30];

    //Size calculations.
    private float elementSize;
    private float screenWidth;
    private float screenHeight;

    private OrthogonalTiledMapRenderer tiledMapRenderer;
    private OrthographicCamera tiledCamera;
    private SpriteBatch batch;

    public MiniGameScreen(final PirateGame main){
        super(main);

        //Initialize.
        player = new MiniGamePlayer();
        mainStage.addActor(player);

        exit = new BaseActor();
        exit.setTexture(new Texture(Gdx.files.internal("coin.png")));
        exit.setRectangleBoundary();
        mainStage.addActor(exit);

        Gdx.app.debug("Minigame","Minigame is loading.");
        enemies = new ArrayList<MiniGameEnemy>();
        wallList = new ArrayList<BaseActor>();
        batch = new SpriteBatch();

        //Setup renderer and camera.
        TiledMap tiledMap = new TmxMapLoader().load("miniGame_try.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        tiledCamera = new OrthographicCamera(viewwidth,viewheight);
        tiledCamera.setToOrtho(false, viewwidth, viewheight);
        tiledCamera.update();

        getMapObject(tiledMap);

        //Get Screen size.
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();
        elementSize = 20*(screenWidth/640);
    }
    //Get the location of walls, player and enemies in the map.
    private void getMapObject(TiledMap tiledMap){
        MapObjects objects = tiledMap.getLayers().get("ObjectData").getObjects();
        for (MapObject object : objects) {
            String name = object.getName();

            // all object data assumed to be stored as rectangles
            RectangleMapObject rectangleObject = (RectangleMapObject) object;
            Rectangle r = rectangleObject.getRectangle();

            if (name.equals("player")){
                player.setPosition(r.x, r.y);
            }
            if(name.equals("enemy")){
                enemies.add(new MiniGameEnemy(r.x,r.y));
            }
            if(name.equals("exit")){
                isExit[(int)(r.x/64)][(int)(r.y/64)] = true;
                exit.setPosition(r.x+14, r.y+10);
            }
        }

        TiledMapTileLayer wallLayer = (TiledMapTileLayer)tiledMap.getLayers().get("wall");

        for(int i = 0; i<tileCountWidth; i++){
            for(int j = 0; j< tileCountHeight;j++){
                if(wallLayer.getCell(i,j)!=null){
                    isWall[i][j] = true;
                }
            }
        }

        // New wall for player movement
        objects = tiledMap.getLayers().get("PhysicsData").getObjects();
        for (MapObject object : objects) {
            RectangleMapObject rectangleObject = (RectangleMapObject) object;
            Rectangle r = rectangleObject.getRectangle();

            BaseActor wall = new BaseActor();
            wall.setPosition(r.x, r.y);
            wall.setSize(r.width, r.height);
            wall.setRectangleBoundary();
            wallList.add(wall);
        }
    }

    //Draw enemies and player.
    private void drawEnemies(){

        for(MiniGameEnemy enemy : enemies){
            batch.draw(enemy.getEnemyTexture(),enemy.getX()/(1920/screenWidth),enemy.getY()/(1080/screenHeight),elementSize,elementSize);
        }
    }
    public void drawPlayer(){
        batch.draw(player.getPlayerTexture(),player.getX()/(1920/screenWidth),player.getY()/(1080/screenHeight),elementSize,elementSize);
    }

    @Override
    public void render(float delta) {
        mainStage.act();
        update(delta);
        //Show all the element on the screen
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        tiledMapRenderer.render();
        mainStage.draw();
        batch.begin();
        drawEnemies();
//        drawPlayer();
        batch.end();
    }

    @Override
    public void update(float delta){
        //Set escape to SellingScreen
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            Gdx.app.debug("Minigame","Escape key was pressed. Exiting to sailing mode.");
            pirateGame.setScreen(new SailingScreen(pirateGame, false));
        }
        //Check if Player finish the game or dead.
        player.resetMovable();
        boolean finish = player.movable(player, isWall, isExit);
        boolean isDead = player.isDead(enemies, player);
//        Show the finish screen.
        if(finish){
            Gdx.app.debug("Minigame","Minigame finished. Transitioning to end screen.");
            pirateGame.setScreen(new MiniGameFinishScreen(pirateGame,false));
        }
        if(isDead){
            Gdx.app.debug("Minigame","Player has died. Transitioning to end screen.");
            pirateGame.setScreen(new MiniGameFinishScreen(pirateGame,true));
        }

        //Player movement.
        player.playerMove(delta);

        for (BaseActor wall : wallList) {
           player.overlaps(wall,true);
        }

        if (player.overlaps(exit, false)) pirateGame.setScreen(new MiniGameFinishScreen(pirateGame,false));

        //Enemies movement.
        for(MiniGameEnemy enemy : enemies){
            enemy.enemyMovement(delta,isWall);
        }
        //Camera update.
        cameraMove();

    }
    //Camera update function.
    private void cameraMove(){
        Camera mainCamera = mainStage.getCamera();
        // bound camera to layout
        mainCamera.position.x = MathUtils.clamp(mainCamera.position.x, viewwidth / 2, mapWidth - viewwidth / 2);
        mainCamera.position.y = MathUtils.clamp(mainCamera.position.y, viewheight / 2, mapHeight - viewheight / 2);
        mainCamera.update();
        // adjust tilemap camera to stay in sync with main camera
        tiledCamera.position.x = mainCamera.position.x;
        tiledCamera.position.y = mainCamera.position.y;
        tiledCamera.update();
        tiledMapRenderer.setView(tiledCamera);
    }

    @Override
    public void dispose () {
        player.getPlayerTexture().dispose();
        batch.dispose();
    }

}
