package com.rear_admirals.york_pirates.screen.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.rear_admirals.york_pirates.screen.SailingScreen;
import com.rear_admirals.york_pirates.screen.WinScreen;
import com.rear_admirals.york_pirates.screen.combat.attacks.*;
import com.rear_admirals.york_pirates.PirateGame;
import com.rear_admirals.york_pirates.Player;
import com.rear_admirals.york_pirates.base.BaseScreen;
import com.rear_admirals.york_pirates.Ship;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.*;

public class CombatScreen extends BaseScreen {

    // Labels changed throughout the scene
    private Label descriptionLabel;
    private Label playerSailsHPLabel;
    private Label playerHullHPLabel;
    private Label enemySailsHPLabel;
    private Label enemyHullHPLabel;

    // Health bars of both ships
    private ProgressBar playerSailsHP;
    private ProgressBar playerHullHP;
    private ProgressBar enemySailsHP;
    private ProgressBar enemyHullHP;

    // Image textures and images for the various stages
    private Texture bg_texture;
    private Image background_wood;

    private Player player;
    private Ship enemy;

    // Control the layout of the stage
    private Table completeAttackTable;

    // Written text box
    private TextButton textBox;

    // Variables used in handling combat
    private Stack<Attack> combatStack;
    private static List<Attack> enemyAttacks;
    private Attack currentAttack;
    private BattleEvent queuedCombatEvent;

    // Variables used in text animation
    private float delayTime = 0;
    private boolean textAnimation = false;
    private int animationIndex = 0;
    private String displayText = "";

    public CombatScreen(final PirateGame pirateGame, Ship enemy){
        // Calls superclass BaseScreen
        super(pirateGame);

        // This constructor also replaces the create function that a stage would typically have.
        this.pirateGame = pirateGame;
        this.player = pirateGame.getPlayer();
        this.enemy = enemy;

        // Load the skin for this screen
        pirateGame.setSkin(new Skin(Gdx.files.internal("flat-earth-ui.json")));

        combatStack = new Stack();

        // Sets size constants for the scene depending on viewport, also sets button padding constants for use in tables
        // screen layout variables
        float button_pad_bottom = viewheight / 24f;
        float button_pad_right = viewwidth / 32f;

        // Instantiate the image textures for use within the scene as backgrounds.
        bg_texture = new Texture("water_texture_sky.png");
        Image background = new Image(bg_texture);
        background.setSize(viewwidth, viewheight);

        Texture wood_texture = new Texture("wood_vertical_board_texture.png");
        background_wood = new Image(wood_texture);
        background_wood.setSize(viewwidth, viewheight);

        // Create a Container which takes up the whole screen (used for layout purposes)
        Container<Table> tableContainer = new Container<Table>();
        tableContainer.setFillParent(true);
        tableContainer.setPosition(0,0);
        tableContainer.align(Align.bottom);

        // Instantiate some different tables used throughout scene
        Table rootTable = new Table();
        Table descriptionTable = new Table();
        Table attackTable = new Table();

        // Instantiate both the ships for the battle
        CombatShip myShip = new CombatShip("ship1.png", viewwidth/3.5f);
        CombatShip enemyShip = new CombatShip("ship2.png",viewwidth/3.5f);

        Label shipName = new Label(player.getPlayerShip().getName(),pirateGame.getSkin(), "default_black");
        playerSailsHP = new ProgressBar(0, player.getPlayerShip().getHealthMax(), 0.1f, false, pirateGame.getSkin());
        playerSailsHPLabel = new Label("Sails: " + player.getPlayerShip().getSailsHealth() + "/" + player.getPlayerShip().getHealthMax(), pirateGame.getSkin());
        playerHullHP = new ProgressBar(0, player.getPlayerShip().getHealthMax(), 0.1f, false, pirateGame.getSkin());
        playerHullHPLabel = new Label("Hull: " + player.getPlayerShip().getHullHealth() + "/" + player.getPlayerShip().getHealthMax(), pirateGame.getSkin());

        playerSailsHP.getStyle().background.setMinHeight(playerSailsHP.getPrefHeight() * 2);
        playerSailsHP.getStyle().background.setMinHeight(playerSailsHP.getPrefHeight());
        playerHullHP.getStyle().background.setMinHeight(playerHullHP.getPrefHeight() * 2);
        playerHullHP.getStyle().knobBefore.setMinHeight(playerHullHP.getPrefHeight());

        Label enemyName = new Label(enemy.getName(), pirateGame.getSkin(),"default_black");
        Gdx.app.debug("Combat","\n" + enemy.getHealthMax()+"\n");
        enemySailsHP = new ProgressBar(0, enemy.getHealthMax(), 0.1f, false, pirateGame.getSkin());
        enemySailsHPLabel = new Label("Sails: " + enemy.getHullHealth() + "/" + enemy.getHealthMax(), pirateGame.getSkin());
        enemyHullHP = new ProgressBar(0, enemy.getHealthMax(), 0.1f, false, pirateGame.getSkin());
        enemyHullHPLabel = new Label("Hull: " + enemy.getHullHealth() + "/" + enemy.getHealthMax(), pirateGame.getSkin());

        playerSailsHP.setValue(player.getPlayerShip().getSailsHealth());
        playerHullHP.setValue(player.getPlayerShip().getHullHealth());
        enemySailsHP.setValue(enemy.getHealthMax());
        enemyHullHP.setValue(enemy.getHealthMax());

        Table playerSailsHPTable = new Table();
        Table playerHullHPTable = new Table();
        Table enemySailsHPTable = new Table();
        Table enemyHullHPTable = new Table();

        playerSailsHPTable.add(playerSailsHPLabel).width(viewwidth/8f).left();
        playerSailsHPTable.add(playerSailsHP).width(viewwidth/5);
        playerSailsHPTable.row().padTop(viewheight/48f);
        playerSailsHPTable.add(playerHullHPLabel).width(viewwidth/8f).left();
        playerSailsHPTable.add(playerHullHP).width(viewwidth/5);

        enemySailsHPTable.add(enemySailsHPLabel).width(viewwidth/8f).left();
        enemySailsHPTable.add(enemySailsHP).width(viewwidth/5);
        enemySailsHPTable.row().padTop(viewheight/48f);
        enemySailsHPTable.add(enemyHullHPLabel).width(viewwidth/8f).left();
        enemySailsHPTable.add(enemyHullHP).width(viewwidth/5);

        Label screenTitle = new Label("Combat Mode", pirateGame.getSkin(),"title_black");
        screenTitle.setAlignment(Align.center);

        if (enemy.getIsBoss()){
            textBox = new TextButton("You encountered the "+ enemy.getCollege().getName()+" boss!", pirateGame.getSkin());
        }
        else{
            textBox = new TextButton("You encountered a "+enemy.getCollege().getName()+" "+enemy.getType()+"!", pirateGame.getSkin());
        }

        textBox.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (textAnimation) {
                    textAnimation = false;
                    textBox.setText(displayText);
                } else {
                    Gdx.app.debug("Combat","Button clicked, running combat handler with event " + queuedCombatEvent.toString());
                    textBox.setText("");
                    updateHP();
                    combatHandler(queuedCombatEvent);
                }
            }
        });

        // Control combat
        this.queuedCombatEvent = BattleEvent.NONE;
        currentAttack = null;


        // Instantiation of the combat buttons. Attack and Flee are default attacks, the rest can be modified within player class.
        while (player.equippedAttacks.size() < 3){
            player.equippedAttacks.add(Attack.attackNone);
        }
        final AttackButton button1 = new AttackButton(Attack.attackMain, pirateGame.getSkin());
        buttonListener(button1);

        final AttackButton button2 = new AttackButton(player.equippedAttacks.get(0), pirateGame.getSkin());
        buttonListener(button2);

        final AttackButton button3 = new AttackButton(player.equippedAttacks.get(1), pirateGame.getSkin());
        buttonListener(button3);

        final AttackButton button4 = new AttackButton(player.equippedAttacks.get(2), pirateGame.getSkin());
        buttonListener(button4);

        final AttackButton fleeButton = new AttackButton(Flee.attackFlee, pirateGame.getSkin(), "red");
        buttonListener(fleeButton);

        descriptionLabel = new Label("What would you like to do?", pirateGame.getSkin());
        descriptionLabel.setWrap(true);
        descriptionLabel.setAlignment(Align.center);

        descriptionTable.center();
        descriptionTable.add(descriptionLabel).uniform().pad(0, button_pad_right,0, button_pad_right).size(viewwidth/2 - button_pad_right *2, viewheight/12).top();
        descriptionTable.row();
        descriptionTable.add(fleeButton).uniform();

        attackTable.row();
        attackTable.add(button1).uniform().width(viewwidth/5).padRight(button_pad_right);
        attackTable.add(button2).uniform().width(viewwidth/5);
        attackTable.row().padTop(button_pad_bottom);
        attackTable.add(button3).uniform().width(viewwidth/5).padRight(button_pad_right);
        attackTable.add(button4).uniform().width(viewwidth / 5);

        rootTable.row().width(viewwidth*0.9f);
        rootTable.add(screenTitle).colspan(2);
        rootTable.row();
        rootTable.add(shipName);
        rootTable.add(enemyName);
        rootTable.row().fillX();
        rootTable.add(myShip);
        rootTable.add(enemyShip);
        rootTable.row();
        rootTable.add(playerSailsHPTable);
        rootTable.add(enemySailsHPTable);
        rootTable.row();
        rootTable.add(playerHullHPTable);
        rootTable.add(enemyHullHPTable);
        rootTable.row();
        rootTable.add(textBox).colspan(2).fillX().height(viewheight/9f).pad(viewheight/12,0,viewheight/12,0);
        tableContainer.setActor(rootTable);

        completeAttackTable = new Table();
        completeAttackTable.setFillParent(true);
        completeAttackTable.align(Align.bottom);
        completeAttackTable.row().expandX().padBottom(viewheight/18f);
        completeAttackTable.add(descriptionTable).width(viewwidth/2);
        completeAttackTable.add(attackTable).width(viewwidth/2);

        background_wood.setVisible(false);
        completeAttackTable.setVisible(false);
        mainStage.addActor(background_wood);
        mainStage.addActor(completeAttackTable);

        uiStage.addActor(background);
        uiStage.addActor(tableContainer);

        // Setup Enemy attacks - may need to be modified if you want to draw attacks from enemy's class
        enemyAttacks = new ArrayList<Attack>();
        enemyAttacks.add(Attack.attackMain);
        enemyAttacks.add(GrapeShot.attackGrape);
        enemyAttacks.add(Attack.attackSwivel);


        Gdx.input.setInputProcessor(uiStage);

        Gdx.app.debug("Combat",viewwidth + "," + viewheight + " AND " + Gdx.graphics.getWidth() + "," + Gdx.graphics.getHeight());
    }

    @Override
    public void update(float delta){ }

	@Override
	public void render (float delta) {
	    Gdx.gl.glClearColor(0,0,0,1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiStage.draw();
        uiStage.act();
        mainStage.draw();
        mainStage.act();
        labelAnimationUpdate(delta);
    }
	
	@Override
	public void dispose () {
        uiStage.dispose();
        mainStage.dispose();
        bg_texture.dispose();
	}


    private void toggleAttackStage(){
        // This method toggles the visibility of the players attack moves and changes input processor to relevant stage
        if (background_wood.isVisible()) {
            background_wood.setVisible(false);
            completeAttackTable.setVisible(false);
            Gdx.input.setInputProcessor(uiStage);
        } else {
            background_wood.setVisible(true);
            completeAttackTable.setVisible(true);
            Gdx.input.setInputProcessor(mainStage);
        }
    }

    // combat Handler
    // This function handles the ship combat using BattleEvent enum type
    private void combatHandler(BattleEvent status){
        //Debugging
        Gdx.app.debug("Combat","Running combatHandler with status: " + status.toString());

        if (!combatStack.empty()){
            currentAttack = combatStack.pop();
        }

        switch(status) {
            case NONE:
                toggleAttackStage();
                break;
            case PLAYER_MOVE:
                toggleAttackStage();
                textBox.setStyle(pirateGame.getSkin().get("default", TextButton.TextButtonStyle.class));
                Gdx.app.debug("Combat","Running player's move");
                if (currentAttack.isSkipMoveStatus()) {
                    Gdx.app.debug("Combat","Charging attack");
                    currentAttack.setSkipMoveStatus(false);
                    combatStack.push(currentAttack);
                    dialog("Charging attack " + currentAttack.getName(), BattleEvent.ENEMY_MOVE);
                } else if (currentAttack.getName() == "FLEE") {
                    if (currentAttack.doAttack(player.getPlayerShip(), enemy) == 1) {
                        Gdx.app.debug("Combat","Flee successful");
                        dialog("Flee successful!", BattleEvent.PLAYER_FLEES);
                    } else {
                        Gdx.app.debug("Combat","Flee Failed");
                        dialog("Flee failed.", BattleEvent.ENEMY_MOVE);
                    }
                } else {
                    int damage = currentAttack.doAttack(player.getPlayerShip(), enemy); // Calls the attack function on the player and stores damage output
                    // This selection statement returns Special Charge attacks to normal state
                    if (currentAttack.isSkipMove()) {
                        currentAttack.setSkipMoveStatus(true);
                    }

                    if (damage == 0) {
                        Gdx.app.debug("Combat","Player "+currentAttack.getName() + " MISSED, damage dealt: " + damage + ", Player Sails Health: " + player.getPlayerShip().getSailsHealth() + ", Player Hull Health: " + player.getPlayerShip().getHullHealth() + ", Enemy Sails Health: " + enemy.getSailsHealth() + ", Enemy Hull Health: " + enemy.getHullHealth());
                        dialog("Attack Missed", BattleEvent.ENEMY_MOVE);
                    } else {
                        Gdx.app.debug("Combat","Player "+currentAttack.getName() + " SUCCESSFUL, damage dealt: " + damage + ", Player Sails Health: " + player.getPlayerShip().getSailsHealth() + ", Player Hull Health: " + player.getPlayerShip().getHullHealth() + ", Enemy Sails Health: " + enemy.getSailsHealth() + ", Enemy Hull Health: " + enemy.getHullHealth());
                        if (player.getPlayerShip().getHullHealth() <= 0) { // Combat ends when hull is fully damaged and ship sinks
                            Gdx.app.debug("Combat","Player has died");
                            dialog("You dealt " + damage + " with " + currentAttack.getName() + "!", BattleEvent.PLAYER_DIES);
                        } else if (enemy.getHullHealth() <= 0) {
                            Gdx.app.debug("Combat","Enemy has died");
                            dialog("You dealt " + damage + " with " + currentAttack.getName() + "!", BattleEvent.ENEMY_DIES);
                        } else{
                            dialog("You dealt " + damage + " with " + currentAttack.getName() + "!", BattleEvent.ENEMY_MOVE);
                        }
                    }
                }
                break;
            case ENEMY_MOVE:
                Gdx.app.debug("Combat","Running enemy move");
                textBox.setStyle(pirateGame.getSkin().get("red", TextButton.TextButtonStyle.class));
                Attack enemyAttack = enemyAttacks.get(ThreadLocalRandom.current().nextInt(0,3));
                int damage = enemyAttack.doAttack(enemy, player.getPlayerShip());
                String message;
                if (damage == 0){
                    Gdx.app.debug("Combat","Enemy " + enemyAttack.getName() + " ATTACK MISSED");
                    message = "Enemies " + enemyAttack.getName() + " missed.";
                } else {
                    Gdx.app.debug("Combat","ENEMY " + enemyAttack.getName() + " SUCCESSFUL, damage dealt: " + damage + ", Player Sails Health: " + player.getPlayerShip().getSailsHealth() + ", Player Hull Health: " + player.getPlayerShip().getHullHealth() + ", Enemy Sails Health: " + enemy.getSailsHealth() + ", Enemy Hull Health: " + enemy.getHullHealth());
                    message = "Enemy "+enemy.getName()+ " dealt " + damage + " with " + enemyAttack.getName()+ "!";
                }

                if (player.getPlayerShip().getHullHealth() <= 0) {
                    Gdx.app.debug("Combat","Player has died");
                    dialog("Enemies " + enemyAttack.getName() + " hit you for "+ damage, BattleEvent.PLAYER_DIES);
                //} else if (enemy.getHealth() <= 0) {========================================================================
                } else if (enemy.getHullHealth() <= 0) {
                    Gdx.app.debug("Combat","Enemy has died");
                    dialog("Enemies " + enemyAttack.getName() + " hit you for "+ damage, BattleEvent.ENEMY_DIES);
                } else {
                    if (currentAttack.isSkipMove() != currentAttack.isSkipMoveStatus()){
                        Gdx.app.debug("Combat","Loading charged attack");
                        dialog(message, BattleEvent.PLAYER_MOVE);
                    } else {
                        dialog(message, BattleEvent.NONE);
                    }
                }
                break;
            case PLAYER_DIES:
                textBox.setStyle(pirateGame.getSkin().get("red", TextButton.TextButtonStyle.class));
                player.addGold(-player.getGold()/2);
                player.setPoints(0);
                player.getPlayerShip().setSailsHealth(Math.max(player.getPlayerShip().getSailsHealth(), player.getPlayerShip().getHealthMax() / 4));
                player.getPlayerShip().setHullHealth(Math.max(player.getPlayerShip().getHullHealth(), player.getPlayerShip().getHealthMax() / 4));
                dialog("YOU HAVE DIED", BattleEvent.SCENE_RETURN);
                break;
            case ENEMY_DIES:
                textBox.setStyle(pirateGame.getSkin().get("default", TextButton.TextButtonStyle.class));
                String upgradeMessage;
                upgradeMessage = "";

                if (enemy.getIsBoss() == true) {
                    enemy.getCollege().setBossDead(true);
                    player.addPoints(100);
                    this.player.getPlayerShip().getCollege().addAlly(this.enemy.getCollege());

                    // This is new for Assessment 4
                    // Randomly upgrades one of the player's stats.
                    //TODO Make a message appear in-game to let the player know they received an upgrade.
                    int upgrade = ThreadLocalRandom.current().nextInt(0,3);
                    switch(upgrade) {
                        case 0: // Player gets gold instead of an upgrade.
                            Gdx.app.log("Upgrade","Player receives gold instead of upgrade.");
                            player.addGold(500);
                            upgradeMessage = "You receive 500 gold and 100 points.";
                            break;
                        case 1: // Cannoneer joins the player's crew
                            Gdx.app.log("Upgrade","Player received an attack upgrade.");
                            player.getPlayerShip().addAttack(1);
                            upgradeMessage = "You earned a Cannoneer - upgrading attack - and 100 points.";
                            break;
                        case 2: // Navigator joins the player's crew
                            Gdx.app.log("Upgrade","Player received a speed upgrade.");
                            player.getPlayerShip().setSpeed(player.getPlayerShip().getSpeed()*1.25f);
                            upgradeMessage = "You earned a Navigator - upgrading speed - and 100 points.";

                            break;
                        case 3: // Shipwright upgrades the player's hull
                            Gdx.app.log("Upgrade","Player received a defence upgrade.");
                            player.getPlayerShip().addDefence(1);
                            upgradeMessage = "You earned a Shipwright - upgrading defence - and 100 points.";

                            break;
                    }

                    // WIN CONDITION - could be improved by removing hardcoding of size.
                    if (player.getPlayerShip().getCollege().getAlly().size() == 5){
                        pirateGame.setScreen(new WinScreen(pirateGame));
                    }
                }
                else {
                    player.addGold(20);
                    player.addPoints(20);
                    upgradeMessage = "You receive 20 gold and 20 points.";
                }
                dialog("Congratulations, you have defeated Enemy " + enemy.getName() + ". " + upgradeMessage, BattleEvent.SCENE_RETURN);

                break;
            case PLAYER_FLEES:
                textBox.setStyle(pirateGame.getSkin().get("red", TextButton.TextButtonStyle.class));
                player.addPoints(-5);
                combatHandler(BattleEvent.SCENE_RETURN);
                break;
            case SCENE_RETURN:
                enemy.setVisible(false);
                player.getPlayerShip().setSpeed(0);
                player.getPlayerShip().setAccelerationXY(0,0);
                player.getPlayerShip().setAnchor(true);
                Gdx.app.debug("Combat","Combat finished. Transitioning back to sailing mode.");
                toggleAttackStage();
                pirateGame.setScreen(new SailingScreen(pirateGame, false));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }
    }

    // Button Listener Classes - creates a hover listener for any button passed through

    private void buttonListener(final AttackButton button){
        button.addListener(new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                descriptionLabel.setText(button.getDesc());
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor){
                descriptionLabel.setText("What would you like to do?");
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                combatStack.push(button.getAttack());
                combatHandler(BattleEvent.PLAYER_MOVE);
            }
        });
    }

    public void buttonListener(final AttackButton button, final String message){
        button.addListener(new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                descriptionLabel.setText(button.getDesc());
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor){
                descriptionLabel.setText("Choose an option");
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                button.setText(message);
            }
        });
    }

    // This method updates the player HP bar and text values
    private void updateHP(){
        enemySailsHP.setAnimateDuration(1);
        enemyHullHP.setAnimateDuration(1);
        playerSailsHP.setAnimateDuration(1);
        playerHullHP.setAnimateDuration(1);

        if (enemy.getSailsHealth() <= 0) {
            enemy.setSailsHealth(0);
        }

        if (enemy.getHullHealth() <= 0) {
            enemy.setHullHealth(0);
        }

        if (player.getPlayerShip().getSailsHealth() <= 0) {
            player.getPlayerShip().setSailsHealth(0);
        }

        if (player.getPlayerShip().getHullHealth() <= 0) {
            player.getPlayerShip().setHullHealth(0);
        }

        
        enemySailsHPLabel.setText("Sails: " + enemy.getSailsHealth() + "/" + enemy.getHealthMax());
        enemySailsHP.setValue(enemy.getSailsHealth());
        enemyHullHPLabel.setText("Hull: " + enemy.getHullHealth() + "/" + enemy.getHealthMax());
        enemyHullHP.setValue(enemy.getHullHealth());

        playerSailsHPLabel.setText("Sails: " + player.getPlayerShip().getSailsHealth() + "/" + player.getPlayerShip().getHealthMax());
        playerSailsHP.setValue(player.getPlayerShip().getSailsHealth());
        playerHullHPLabel.setText("Hull: " + player.getPlayerShip().getHullHealth() + "/" + player.getPlayerShip().getHealthMax());
        playerHullHP.setValue(player.getPlayerShip().getHullHealth());
    }

    // Updates and displays text box
    private void dialog(String message, final BattleEvent nextEvent){
        queuedCombatEvent = nextEvent;

        if (background_wood.isVisible()){
            toggleAttackStage();
        }

        displayText = message;
        animationIndex = 0;
        textAnimation = true;
    }

    // This method controls the animation of the dialog label
    private void labelAnimationUpdate(float dt){
        if (textAnimation) {
            delayTime += dt;

            if (animationIndex > displayText.length()){
                textAnimation = false;
            }

            if (delayTime >= 0.05f){
                textBox.setText(displayText.substring(0,animationIndex));
                animationIndex++;
                delayTime = 0;
            }
        }
    }
}

