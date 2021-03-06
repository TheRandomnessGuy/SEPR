package com.rear_admirals.york_pirates.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.rear_admirals.york_pirates.*;
import com.rear_admirals.york_pirates.base.LabelTimer;
import com.rear_admirals.york_pirates.screen.combat.CombatScreen;
import com.rear_admirals.york_pirates.base.BaseActor;
import com.rear_admirals.york_pirates.base.BaseScreen;
import com.rear_admirals.york_pirates.screen.combat.attacks.Attack;
import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.*;

import static com.rear_admirals.york_pirates.College.*;
import static com.rear_admirals.york_pirates.PirateGame.*;
import static com.rear_admirals.york_pirates.ShipType.*;

public class SailingScreen extends BaseScreen {

    private Ship playerShip;

    //Map Variables
    private ArrayList<BaseActor> obstacleList;
    private ArrayList<BaseActor> removeList;
    private ArrayList<BaseActor> regionList;


    private final int mapPixelWidth;
    private final int mapPixelHeight;


    private OrthogonalTiledMapRenderer tiledMapRenderer;
    private OrthographicCamera tiledCamera;
    private int[] backgroundLayers = {0,1,2,3};
    private int[] foregroundLayers = {4};

    private Label sailsHealthValueLabel;
    private Label hullHealthValueLabel;
    private Label goldValueLabel;
    private Label pointsValueLabel;

    private Label mapMessage;
    private Label hintMessage;

    private Float timer;

    // New for Assessment 4
    // Variables that are updated while the program is running, mainly for debug reasons but also to give player information about how the game is running should they wish.
    private Table infoTable;
    private Label coordinateLabel;
    private Label fpsLabel;

    // New for Assessment 4
    // Lists to store objects related to monster collisions
    private ArrayList<SeaMonster> monsterArrayList;
    private ArrayList<LabelTimer> damageLabels;

    private Skin skin;
    private Player player;
    private LabelTimer deathLabel;

    // New for Assessment 4
    // Boolean to pause the game updates, used for when the ship needs to respawn.
    private Boolean paused = false;

    public SailingScreen(final PirateGame main, boolean isFirstSailingInstance){
        super(main);

        this.player = main.getPlayer();
        this.skin = main.getSkin();

        playerShip = main.getPlayer().getPlayerShip();
        Gdx.app.debug("Sailing","Player ship's name is "+playerShip.getName());

        mainStage.addActor(playerShip);
        Gdx.app.debug("Sailing","playerShip added to mainStage");

        Table uiTable = new Table();

        // New for Assessment 4
        // Creating and positioning the label in the screen, so whenever needed can be set to visible.
        deathLabel = new LabelTimer("YOU DIED! Respawning in 3 seconds.", skin, 3, Color.RED);
        deathLabel.setSize(600, 300);
        uiStage.addActor(deathLabel);
        deathLabel.setFillParent(true);
        deathLabel.setAlignment(Align.center);
        deathLabel.setVisible(false);


        // New for Assessment 4
        // Creating the lists for random
        monsterArrayList = new ArrayList<>();
        damageLabels = new ArrayList<>();

        Label sailsHealthTextLabel = new Label("Sails Health: ", main.getSkin(), "default_black");
        sailsHealthValueLabel = new Label(Integer.toString(main.getPlayer().getPlayerShip().getSailsHealth()), main.getSkin(), "default_black");
        sailsHealthValueLabel.setAlignment(Align.left);

        Label hullHealthTextLabel = new Label("Hull Health: ", main.getSkin(), "default_black");
        hullHealthValueLabel = new Label(Integer.toString(main.getPlayer().getPlayerShip().getHullHealth()), main.getSkin(),"default_black");
        hullHealthValueLabel.setAlignment(Align.left);

        Label pointsTextLabel = new Label("Points: ", main.getSkin(), "default_black");
        pointsValueLabel = new Label(Integer.toString(main.getPlayer().getPoints()), main.getSkin(), "default_black");
        pointsValueLabel.setAlignment(Align.left);

        Label goldTextLabel = new Label("Gold:", main.getSkin(), "default_black");
        goldValueLabel = new Label(Integer.toString(main.getPlayer().getGold()), main.getSkin(), "default_black");
        goldValueLabel.setAlignment(Align.left);


        uiTable.add(sailsHealthTextLabel).fill();
        uiTable.add(sailsHealthValueLabel).fill();
        uiTable.row();
        uiTable.add(hullHealthTextLabel).fill();
        uiTable.add(hullHealthValueLabel).fill();
        uiTable.row();
        uiTable.add(goldTextLabel).fill();
        uiTable.add(goldValueLabel).fill();
        uiTable.row();
        uiTable.add(pointsTextLabel).fill();
        uiTable.add(pointsValueLabel).width(pointsTextLabel.getWidth());
        uiTable.align(Align.topRight);
        uiTable.setFillParent(true);

        uiStage.addActor(uiTable);

        coordinateLabel = new Label("", skin, "default_black");
        fpsLabel = new Label("FPS: " + Gdx.graphics.getFramesPerSecond(), skin, "default_black");

        infoTable = new Table();
        infoTable.add(coordinateLabel);
        infoTable.row();
        infoTable.add(fpsLabel).left();
        uiStage.addActor(infoTable);

        infoTable.setVisible(false);
        infoTable.setFillParent(true);
        infoTable.align(Align.topLeft);


        mapMessage = new Label("", main.getSkin(), "default_black");
        hintMessage = new Label("", main.getSkin(),"default_black");

        Table messageTable = new Table();
        messageTable.add(mapMessage);
        messageTable.row();
        messageTable.add(hintMessage);

        messageTable.setFillParent(true);
        messageTable.top();

        uiStage.addActor(messageTable);

        obstacleList = new ArrayList<BaseActor>();
        removeList = new ArrayList<BaseActor>();
        regionList = new ArrayList<BaseActor>();

        // set up Tiled Map and associated properties/attributes (width/height)
        //calculate game world dimensions
        TiledMap tiledMap = new TmxMapLoader().load("new_game_map.tmx");
        MapProperties mapProperties = tiledMap.getProperties();
        int mapTileWidth = mapProperties.get("width", Integer.class);
        int mapTileHeight = mapProperties.get("height", Integer.class);
        int tilePixelSize = mapProperties.get("tilewidth", Integer.class);
        mapPixelWidth = tilePixelSize * mapTileWidth;
        mapPixelHeight = tilePixelSize * mapTileHeight;


        // Setup renderer
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        // Setup camera
        tiledCamera = new OrthographicCamera();
        tiledCamera.setToOrtho(false, viewwidth, viewheight);
        tiledCamera.update();

        MapObjects objects = tiledMap.getLayers().get("ObjectData").getObjects();
        for (MapObject object : objects) {
            String name = object.getName();

            // all object data assumed to be stored as rectangles
            RectangleMapObject rectangleObject = (RectangleMapObject)object;
            Rectangle r = rectangleObject.getRectangle();

            if (name.equals("player") && isFirstSailingInstance){
                playerShip.setPosition(r.x, r.y);
            }
            else if (name.equals("player") && !isFirstSailingInstance) {
                playerShip.setPosition(pirateGame.getSailingShipX(), pirateGame.getSailingShipY());
                playerShip.setRotation(pirateGame.getSailingShipRotation());
            }
            else{
                System.err.println("Unknown tilemap object: " + name);
            }
        }

        objects = tiledMap.getLayers().get("PhysicsData").getObjects();
        for (MapObject object : objects) {
            if (object instanceof RectangleMapObject) {
                RectangleMapObject rectangleObject = (RectangleMapObject) object;
                Rectangle r = rectangleObject.getRectangle();

                BaseActor solid = new BaseActor();
                solid.setPosition(r.x, r.y);
                solid.setSize(r.width, r.height);
                solid.setName(object.getName());
                solid.setRectangleBoundary();
                String objectName = object.getName();

                if (objectName.equals("derwent")) solid.setCollege(Derwent);
                else if (objectName.equals("james")) solid.setCollege(James);
                else if (objectName.equals("vanbrugh")) solid.setCollege(Vanbrugh);
                else if (objectName.equals("alcuin")) solid.setCollege(Alcuin);
                else if (objectName.equals("wentworth")) solid.setCollege(Wentworth);
                else if (objectName.equals("chemistry"))solid.setDepartment(Chemistry);
                else if (objectName.equals("physics")) solid.setDepartment(Physics);
                else if (objectName.equals("economics")) solid.setDepartment(Economics);
                else{
                    Gdx.app.debug("Sailing","Not college/department: " + solid.getName());
                }
                obstacleList.add(solid);
            } else {
                Gdx.app.error("Sailing","Unknown PhysicsData object.");
            }
        }

        objects = tiledMap.getLayers().get("RegionData").getObjects();
        for (MapObject object : objects) {
            if (object instanceof RectangleMapObject) {
                RectangleMapObject rectangleObject = (RectangleMapObject) object;
                Rectangle r = rectangleObject.getRectangle();

                BaseActor region = new BaseActor();
                region.setPosition(r.x, r.y);
                region.setSize(r.width, r.height);
                region.setRectangleBoundary();
                region.setName(object.getName());

                if (object.getName().equals("derwentregion")) region.setCollege(Derwent);
                else if (object.getName().equals("jamesregion")) region.setCollege(James);
                else if (object.getName().equals("vanbrughregion")) region.setCollege(Vanbrugh);
                else if (object.getName().equals("alcuinregion")) region.setCollege(Alcuin);
                else if (object.getName().equals("wentworthregion")) region.setCollege(Wentworth);
                regionList.add(region);
            } else {
                System.err.println("Unknown RegionData object.");
            }
        }

        timer = 0f;

        InputMultiplexer im = new InputMultiplexer(uiStage, mainStage);
        Gdx.input.setInputProcessor(im);
    }

    @Override
    public void update(float delta) {
        removeList.clear();
        goldValueLabel.setText(Integer.toString(pirateGame.getPlayer().getGold()));
        this.playerShip.playerMove(delta);

        pirateGame.setSailingShipX(playerShip.getX());
        pirateGame.setSailingShipY(playerShip.getY());
        pirateGame.setSailingShipRotation(playerShip.getRotation());

        Boolean x = false;
        for (BaseActor region : regionList) {
            String name = region.getName();
            if (playerShip.overlaps(region, false)) {
                x = true;
                mapMessage.setText(capitalizeFirstLetter(name.substring(0, name.length() - 6)) + " Territory");
                College college = region.getCollege();
                if (playerShip.getCollege().getAlly().contains(college)) {
                    mapMessage.setText(capitalizeFirstLetter(name.substring(0, name.length() - 6)) + " Territory (peaceful)");
                }
                int enemyChance = ThreadLocalRandom.current().nextInt(0, 10001);
                if (enemyChance <= 15) {
                    pirateGame.setSailingShipX(playerShip.getX());
                    pirateGame.setSailingShipY(playerShip.getY());
                    pirateGame.setSailingShipRotation(playerShip.getRotation());
                    Gdx.app.log("Sailing","Enemy encountered in " + name);
                    if (!playerShip.getCollege().getAlly().contains(college)) {
                        Gdx.app.debug("Sailing",name);
                        pirateGame.setScreen(new CombatScreen(pirateGame, new Ship(Brig, college)));
                    }
                }
            }
        }

        if (!x) {
            mapMessage.setText("Neutral Territory");
        }

        // New for Assessment 4
        // Random number generator for the spawning of monsters
        int monsterChance = ThreadLocalRandom.current().nextInt(0, 10001);
        if (monsterChance < 20){
            Boolean monsterAllowedPosition = false;
            SeaMonster monster = new SeaMonster(0, 0);
            while (!monsterAllowedPosition){
                // Once a monster has been generated, keep randomly generating a position on the map until a valid position for spawning is found
                Integer monsterPosX = ThreadLocalRandom.current().nextInt(0, mapPixelWidth);
                Integer monsterPosY = ThreadLocalRandom.current().nextInt(0, mapPixelHeight);
                monster.setPosition(monsterPosX, monsterPosY);
                Boolean safePos = true;

                for (BaseActor obstacle : obstacleList) {
                    if (monster.overlaps(obstacle, false)){
                        safePos = false;
                    }
                }
                monsterAllowedPosition = safePos;
            }
            monsterArrayList.add(monster);
            mainStage.addActor(monster);
        }

        Boolean resetHintMessage = false;
        for (BaseActor obstacle : obstacleList) {
            String name = obstacle.getName();
            if (playerShip.overlaps(obstacle, true)) {
                // If true, then ship is colliding with a solid object
                resetHintMessage = true;
                if (!(obstacle.getDepartment() == null)) {
                    mapMessage.setText(capitalizeFirstLetter(name) + " Island");
                    hintMessage.setText("Press F to interact");
                    if (Gdx.input.isKeyPressed(Input.Keys.F)) pirateGame.setScreen(new DepartmentScreen(pirateGame, obstacle.getDepartment()));
                }
                // Obstacle must be a college if college not null
                else if (!(obstacle.getCollege() == null)) {
                    mapMessage.setText(capitalizeFirstLetter(name) + " Island");
                    hintMessage.setText("Press F to interact");
                    College college = obstacle.getCollege();
                    if (Gdx.input.isKeyPressed(Input.Keys.F)) {
                        Gdx.app.debug("Sailing","Interacted with a college");
                        if (!playerShip.getCollege().getAlly().contains(college) && !obstacle.getCollege().isBossDead()) {
                            Gdx.app.debug("Sailing","College is hostile.");
                            pirateGame.setScreen(new CombatScreen(pirateGame, new Ship(1.25f, 8, 1.25f, Brig, college, college.getName() + " Boss", true)));
                        } else {
                            Gdx.app.debug("Sailing","College is friendly.");
                            pirateGame.setScreen(new CollegeScreen(pirateGame, college));
                        }
                    }
                } else {
//                    Gdx.app.debug("Sailing","Pure obstacle encountered");
                }
            }
        }

        if (!resetHintMessage) hintMessage.setText("");

        for (BaseActor object : removeList) {
            object.remove();
        }

        // camera adjustment
        Camera mainCamera = mainStage.getCamera();

        // center camera on player
        mainCamera.position.x = playerShip.getX() + playerShip.getOriginX();
        mainCamera.position.y = playerShip.getY() + playerShip.getOriginY();

        // bound camera to layout
        mainCamera.position.x = MathUtils.clamp(mainCamera.position.x, viewwidth / 2, mapPixelWidth - viewwidth / 2);
        mainCamera.position.y = MathUtils.clamp(mainCamera.position.y, viewheight / 2, mapPixelHeight - viewheight / 2);
        mainCamera.update();

        // adjust tilemap camera to stay in sync with main camera
        tiledCamera.position.x = mainCamera.position.x;
        tiledCamera.position.y = mainCamera.position.y;
        tiledCamera.update();
        tiledMapRenderer.setView(tiledCamera);

        timer += delta;

        // Every second everything in this selection statement is completed.
        if (timer > 1) {
            // Update 'debug' table
            coordinateLabel.setText("X: " + ((int) playerShip.getX()) + ", Y: " + ((int)playerShip.getY()));
            fpsLabel.setText("FPS: " + Gdx.graphics.getFramesPerSecond());

            // This Iterator 'iter' is used to control the length of time a SeaMonster is alive for
            Iterator<SeaMonster> iter = monsterArrayList.iterator();
            while (iter.hasNext()) {
                SeaMonster item = iter.next();
                item.setTime(item.getTime() - 1);
                if (item.getTime() <= 0){
                    item.remove();
                    iter.remove();
                }
            }

            // This Iterator 'labelIterator' is used to control the length of time a damage label is shown for
            Iterator<LabelTimer> labelIterator = damageLabels.iterator();
            while (labelIterator.hasNext()){
                LabelTimer label = labelIterator.next();
                label.setTimer(label.getTimer()-1);
                if (label.getTimer() < 0) {
                    label.remove();
                    labelIterator.remove();
                }
            }

            // Only give the player points when not sailing in neutral territory.
            if (x){
                pirateGame.getPlayer().addPoints(1);
            }
            timer -= 1;

        }

        // This iterator loops through every active SeaMonster to check for collisions with the player and walls, and to enable AI movement
        Iterator<SeaMonster> monsterIterator = monsterArrayList.iterator();

        while (monsterIterator.hasNext()) {
            SeaMonster monster = monsterIterator.next();
            for (BaseActor obstacle : obstacleList) {
                if (monster.isCollide()){
                    monster.overlaps(obstacle, true);
                }
            }
            if (monster.overlaps(playerShip, true)){
                playerShip.setHullHealth(playerShip.getHullHealth() - 10);
                monster.remove();
                monsterIterator.remove();

                LabelTimer damageLabel = new LabelTimer("-10hp", skin, 1, Color.RED);
                mainStage.addActor(damageLabel);
                damageLabel.setPosition(playerShip.getX(),playerShip.getY(), Align.top);
                damageLabels.add(damageLabel);
            }

            // Monster movement
            monster.monsterMovement(this.playerShip);
        }

        // Update labels
        pointsValueLabel.setText(Integer.toString(pirateGame.getPlayer().getPoints()));
        sailsHealthValueLabel.setText(Integer.toString(pirateGame.getPlayer().getPlayerShip().getSailsHealth()));
        hullHealthValueLabel.setText(Integer.toString(pirateGame.getPlayer().getPlayerShip().getHullHealth()));
    }

    @Override
    public void render(float delta) {
        uiStage.act(delta);

        mainStage.act(delta);

        if (!paused) update(delta);

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        tiledMapRenderer.render(backgroundLayers);
        mainStage.draw();

        tiledMapRenderer.render(foregroundLayers);

        uiStage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.P)){
            Gdx.app.log("game save","Game saved");
            saveFile(pirateGame.getSave_file());
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)){
            infoTable.setVisible(!infoTable.isVisible());
        }
        if (!playerShip.isAnchor()){
            playerShip.addAccelerationAS(playerShip.getRotation(), 10000);
        } else{
            playerShip.setAccelerationXY(0,0);
            playerShip.setDeceleration(250);
        }
        // New for Assessment 4
        // Control the behaviour when the ship is sunk while Sailing
        if (playerShip.getHullHealth() <= 0){
            paused = true;
            deathLabel.setVisible(true);
            player.getPlayerShip().setSpeed(0);
            player.getPlayerShip().setAccelerationXY(0,0);
            player.getPlayerShip().setAnchor(true);
            playerShip.setVisible(false);

            deathLabel.setTimer(deathLabel.getTimer() - delta);
            deathLabel.setText("YOU DIED! Respawning in " + Math.ceil(deathLabel.getTimer()) + "seconds.");

            if (deathLabel.getTimer() <= 0){
                player.addGold(-player.getGold()/2);
                player.setPoints(0);
                playerShip.setSailsHealth(Math.max(playerShip.getSailsHealth(), playerShip.getHealthMax() / 4));
                playerShip.setHullHealth(Math.max(playerShip.getHullHealth(), playerShip.getHealthMax() / 4));
                playerShip.setPosition(4250,1900);
                playerShip.setVisible(true);
                deathLabel.setVisible(false);
                deathLabel.setTimer(3);
                paused = false;
            }

        }
    }

    @Override
    public void dispose () {
        mainStage.dispose();
        uiStage.dispose();
        playerShip.getSailingTexture().dispose();
    }

    //This function capitalizes the first letter of a string taken as a parameter
    private String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    // New for Assessment 4
    // This method stores all data in an external file
    private void saveFile(Preferences file){

        //
        byte[] ownedAttacks = SerializationUtils.serialize(new ArrayList<Attack>(pirateGame.getPlayer().getOwnedAttacks()));
        String encodedOwnedAttacks = Base64.getEncoder().encodeToString(ownedAttacks);

        byte[] equippedAttacks = SerializationUtils.serialize(new ArrayList<Attack>(pirateGame.getPlayer().getEquippedAttacks()));
        String encodedEquippedAttacks = Base64.getEncoder().encodeToString(equippedAttacks);

        byte[] derwentData = SerializationUtils.serialize(Derwent);
        String encodedDerwent = Base64.getEncoder().encodeToString(derwentData);

        byte[] vanbrughData = SerializationUtils.serialize(Vanbrugh);
        String encodedVanbrugh = Base64.getEncoder().encodeToString(vanbrughData);

        byte[] jamesData = SerializationUtils.serialize(James);
        String encodedJames = Base64.getEncoder().encodeToString(jamesData);

        byte[] alcuinData = SerializationUtils.serialize(Alcuin);
        String encodedAlcuin = Base64.getEncoder().encodeToString(alcuinData);

        byte[] wentworthData = SerializationUtils.serialize(Wentworth);
        String encodedWentworth = Base64.getEncoder().encodeToString(wentworthData);



        file.putString("owned attacks", encodedOwnedAttacks);
        file.putString("equipped attacks", encodedEquippedAttacks);

        file.putString("derwent", encodedDerwent);
        file.putString("vanbrugh", encodedVanbrugh);
        file.putString("james", encodedJames);
        file.putString("alcuin", encodedAlcuin);
        file.putString("wentworth", encodedWentworth);


        // Player Data
        file.putInteger("gold", pirateGame.getPlayer().getGold());
        file.putInteger("points", pirateGame.getPlayer().getPoints());


        //Ship Data: float atkMultiplier, int defence, int accMultiplier, ShipType type, College college, String name, boolean isBoss
        file.putFloat("atkMultiplier", playerShip.getAtkMultiplier());
        file.putInteger("defence", playerShip.getDefence());
        file.putFloat("accMultiplier", playerShip.getAccMultiplier());
        file.putString("name", playerShip.getName());
        file.putInteger("sail health", playerShip.getSailsHealth());
        file.putInteger("hull health", playerShip.getHullHealth());


        // Ship Position Data
        file.putFloat("shipX", pirateGame.getSailingShipX());
        file.putFloat("shipY", pirateGame.getSailingShipY());
        file.putFloat("shipRotation", pirateGame.getSailingShipRotation());

        file.flush();
    }

}
