package com.javafx.game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

abstract class Obstacle extends Rectangle {
    public Obstacle(double x, double y, double width, double height, Color color) {
        super(width, height, color);
        setX(x);
        setY(y);
    }

    abstract void handleCollision();
}

interface Collectible {
    void collect();
}

class Cherry extends Circle implements Collectible {
    private static final Random random = new Random();
    private int cherriesCollected;
    private int score = 0;



    public Cherry(double radius, double centerX, double centerY, double platformHeight, Color color) {
        super(radius, color);
        boolean randomizePosition = random.nextBoolean();
        if (randomizePosition) {
            centerY = randomizeCherryPosition(centerY, radius, platformHeight);
        }
        setCenterX(centerX);
        setCenterY(centerY);
    }

    private double randomizeCherryPosition(double centerY, double radius, double platformHeight) {
        if (random.nextBoolean()) {
            // Place the cherry below the platform
            return centerY - radius + 100;
        } else {
            // Place the cherry above the platform
            return centerY - radius - 20; // Adjust '20' for desired distance above the platform
        }
    }

    @Override
    public void collect() {
        cherriesCollected++;
        score += 10;  // Adjust points based on your design
        //        updateScoreLabel();
    }
}

interface ScoreObserver {
    void update(int score);
}

class ScoreSubject {
    private List<ScoreObserver> observers = new ArrayList<>();

    public void addObserver(ScoreObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(int score) {
        for (ScoreObserver observer : observers) {
            observer.update(score);
        }
    }
}

class ScoreLabel implements ScoreObserver {
    private Label scoreLabel;

    public ScoreLabel(Label scoreLabel) {
        this.scoreLabel = scoreLabel;
    }

    @Override
    public void update(int score) {
        Platform.runLater(() -> scoreLabel.setText(String.valueOf(score)));
    }
}

class HighScoreLabel implements ScoreObserver {
    private Label highScoreLabel;

    public HighScoreLabel(Label highScoreLabel) {
        this.highScoreLabel = highScoreLabel;
    }

    @Override
    public void update(int score) {
        int currentHighScore = getHighScore();
        if (score > currentHighScore) {
            // Update the high score
            // Implement logic to store the new high score in your storage
            Platform.runLater(() -> highScoreLabel.setText("High Score: " + score));
        }
    }

    private int getHighScore() {
        // Implement logic to retrieve the high score from your storage (file, database, etc.)
        // Return 0 if high score retrieval fails or if no high score is stored
        return 0;
    }
}

// Factory Pattern
interface ObstacleFactory {
    Cherry createObstacle(double x, double y, double width, double height, Color color);
}

class CherryFactory implements ObstacleFactory {
    private final Random random = new Random();
    @Override
    public Cherry createObstacle(double centerX, double centerY, double width, double height, Color color) {



        double radius = width / 2;

        // Randomize cherry position to be above or below the platform
        boolean placeBelow = random.nextBoolean();
        if (placeBelow) {
            // Place the cherry below the platform
            centerY += height + radius; // Adjust 'height' based on the platform's height
        } else {
            // Place the cherry above the platform
            centerY -= radius + 20; // Adjust '20' for desired distance above the platform
        }

        return new Cherry(radius, centerX, centerY, 100, color);
    }
}

// Singleton Pattern
//class MediaPlayerSingleton {
//    private static MediaPlayer mediaPlayer;
//
//    private MediaPlayerSingleton() {
//        // Private constructor to prevent instantiation
//    }
//
//    public static MediaPlayer getInstance() {
//        if (mediaPlayer == null) {
//            // Replace "game_audio.mp3" with the actual path to your audio file
//            String audioFile = "com/javafx/game/game_audio.mp3";
//            Media sound = new Media(new File(audioFile).toURI().toString());
//            mediaPlayer = new MediaPlayer(sound);
//            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
//            mediaPlayer.play();
//        }
//        return mediaPlayer;
//    }
//}

class SimpleAudioPlayer
{

    // to store current position
    Long currentFrame;
    Clip clip;

    // current status of clip
    String status;

    AudioInputStream audioInputStream;
//    static String filePath;

    // constructor to initialize streams and clip
    public SimpleAudioPlayer(String filePath)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        // create AudioInputStream object using the provided filePath
        audioInputStream = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());

        // create clip reference
        clip = AudioSystem.getClip();

        // open audioInputStream to the clip
        clip.open(audioInputStream);

        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void play()
    {
        //start the clip
        clip.start();

        status = "play";
    }
}


public class App extends Application {
    private static final double GROUND_Y_POS = 600;
    private static final int CHERRY_SCORE = 0;
    private AtomicInteger counter = new AtomicInteger(0);

    final double BAR_WIDTH = 10;
    final double START_BAR_SIZE = BAR_WIDTH;
    final double BAR_ROTATION_SPEED = 2.5;
    double barGrowthSpeed = 5;
    double startBarPosition = 0;
    double barSize;
    double barRotationAngle = 0;
    private Pane gameScrollPane;
    private int cherriesCollected = 0;
    Rectangle barRectangle;
    final double CHERRY_RADIUS = 10;
    final double PLAYER_RUN_SPEED = 8;
    double playerXPosition = 0;
    double playerXIncrement = 0;
    double playerYPosition = 500;
    double playerYIncrement = 0;

    final double PLATFORM_1_WIDTH = 100;
    double minStartPlatforms = 400;
    double minPlatformWidth = 50;
    List<Cherry> cherries = new ArrayList<>();

    double holeXPosition;
    double newXPlatformPosition;
    double newPlatformWidth;

    Group playerGroup;
    Canvas currentBarCanvas;
    Rotate rotateCurrentBar;

    int score = 0;
    Label scoreLabel;
    Label highScoreLabel;
    Label gameoverLabel;
    boolean gameOver = false;
    boolean revive =false;

    Timeline barGrowTimeline;
    Timeline barRotateTimeline;
    Timeline playerRunTimeline;
    Timeline playerDieTimeline;

    Button replayButton;
    Button homeButton;
    List<Integer> scoresList = new ArrayList<>();

    public AtomicInteger getCounter() {
        return counter;
    }

    public void setCounter(AtomicInteger counter) {
        this.counter = counter;
    }

    public double getBAR_WIDTH() {
        return BAR_WIDTH;
    }

    public double getSTART_BAR_SIZE() {
        return START_BAR_SIZE;
    }

    public double getBAR_ROTATION_SPEED() {
        return BAR_ROTATION_SPEED;
    }

    public double getBarGrowthSpeed() {
        return barGrowthSpeed;
    }

    public void setBarGrowthSpeed(double barGrowthSpeed) {
        this.barGrowthSpeed = barGrowthSpeed;
    }

    public double getStartBarPosition() {
        return startBarPosition;
    }

    public void setStartBarPosition(double startBarPosition) {
        this.startBarPosition = startBarPosition;
    }

    public double getBarSize() {
        return barSize;
    }

    public void setBarSize(double barSize) {
        this.barSize = barSize;
    }

    public double getBarRotationAngle() {
        return barRotationAngle;
    }

    public void setBarRotationAngle(double barRotationAngle) {
        this.barRotationAngle = barRotationAngle;
    }

    public Pane getGameScrollPane() {
        return gameScrollPane;
    }

    public void setGameScrollPane(Pane gameScrollPane) {
        this.gameScrollPane = gameScrollPane;
    }

    public int getCherriesCollected() {
        return cherriesCollected;
    }

    public void setCherriesCollected(int cherriesCollected) {
        this.cherriesCollected = cherriesCollected;
    }

    public Rectangle getBarRectangle() {
        return barRectangle;
    }

    public void setBarRectangle(Rectangle barRectangle) {
        this.barRectangle = barRectangle;
    }

    public double getCHERRY_RADIUS() {
        return CHERRY_RADIUS;
    }

    public double getPLAYER_RUN_SPEED() {
        return PLAYER_RUN_SPEED;
    }

    public double getPlayerXPosition() {
        return playerXPosition;
    }

    public void setPlayerXPosition(double playerXPosition) {
        this.playerXPosition = playerXPosition;
    }

    public double getPlayerXIncrement() {
        return playerXIncrement;
    }

    public void setPlayerXIncrement(double playerXIncrement) {
        this.playerXIncrement = playerXIncrement;
    }

    public double getPlayerYPosition() {
        return playerYPosition;
    }

    public void setPlayerYPosition(double playerYPosition) {
        this.playerYPosition = playerYPosition;
    }

    public double getPlayerYIncrement() {
        return playerYIncrement;
    }

    public void setPlayerYIncrement(double playerYIncrement) {
        this.playerYIncrement = playerYIncrement;
    }

    public double getPLATFORM_1_WIDTH() {
        return PLATFORM_1_WIDTH;
    }

    public double getMinStartPlatforms() {
        return minStartPlatforms;
    }

    public void setMinStartPlatforms(double minStartPlatforms) {
        this.minStartPlatforms = minStartPlatforms;
    }

    public double getMinPlatformWidth() {
        return minPlatformWidth;
    }

    public void setMinPlatformWidth(double minPlatformWidth) {
        this.minPlatformWidth = minPlatformWidth;
    }

    public List<Cherry> getCherries() {
        return cherries;
    }

    public void setCherries(List<Cherry> cherries) {
        this.cherries = cherries;
    }

    public double getHoleXPosition() {
        return holeXPosition;
    }

    public void setHoleXPosition(double holeXPosition) {
        this.holeXPosition = holeXPosition;
    }

    public double getNewXPlatformPosition() {
        return newXPlatformPosition;
    }

    public void setNewXPlatformPosition(double newXPlatformPosition) {
        this.newXPlatformPosition = newXPlatformPosition;
    }

    public double getNewPlatformWidth() {
        return newPlatformWidth;
    }

    public void setNewPlatformWidth(double newPlatformWidth) {
        this.newPlatformWidth = newPlatformWidth;
    }

    public Group getPlayerGroup() {
        return playerGroup;
    }

    public void setPlayerGroup(Group playerGroup) {
        this.playerGroup = playerGroup;
    }

    public Canvas getCurrentBarCanvas() {
        return currentBarCanvas;
    }

    public void setCurrentBarCanvas(Canvas currentBarCanvas) {
        this.currentBarCanvas = currentBarCanvas;
    }

    public Rotate getRotateCurrentBar() {
        return rotateCurrentBar;
    }

    public void setRotateCurrentBar(Rotate rotateCurrentBar) {
        this.rotateCurrentBar = rotateCurrentBar;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Label getScoreLabel() {
        return scoreLabel;
    }

    public void setScoreLabel(Label scoreLabel) {
        this.scoreLabel = scoreLabel;
    }

    public Label getHighScoreLabel() {
        return highScoreLabel;
    }

    public void setHighScoreLabel(Label highScoreLabel) {
        this.highScoreLabel = highScoreLabel;
    }

    public Label getGameoverLabel() {
        return gameoverLabel;
    }

    public void setGameoverLabel(Label gameoverLabel) {
        this.gameoverLabel = gameoverLabel;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isRevive() {
        return revive;
    }

    public void setRevive(boolean revive) {
        this.revive = revive;
    }

    public Timeline getBarGrowTimeline() {
        return barGrowTimeline;
    }

    public void setBarGrowTimeline(Timeline barGrowTimeline) {
        this.barGrowTimeline = barGrowTimeline;
    }

    public Timeline getBarRotateTimeline() {
        return barRotateTimeline;
    }

    public void setBarRotateTimeline(Timeline barRotateTimeline) {
        this.barRotateTimeline = barRotateTimeline;
    }

    public Timeline getPlayerRunTimeline() {
        return playerRunTimeline;
    }

    public void setPlayerRunTimeline(Timeline playerRunTimeline) {
        this.playerRunTimeline = playerRunTimeline;
    }

    public Timeline getPlayerDieTimeline() {
        return playerDieTimeline;
    }

    public void setPlayerDieTimeline(Timeline playerDieTimeline) {
        this.playerDieTimeline = playerDieTimeline;
    }

    public Button getReplayButton() {
        return replayButton;
    }

    public void setReplayButton(Button replayButton) {
        this.replayButton = replayButton;
    }

    public Button getHomeButton() {
        return homeButton;
    }

    public void setHomeButton(Button homeButton) {
        this.homeButton = homeButton;
    }

    public List<Integer> getScoresList() {
        return scoresList;
    }

    public void setScoresList(List<Integer> scoresList) {
        this.scoresList = scoresList;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public ScoreSubject getScoreSubject() {
        return scoreSubject;
    }

    public void setScoreSubject(ScoreSubject scoreSubject) {
        this.scoreSubject = scoreSubject;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isUpsideDown() {
        return upsideDown;
    }

    public void setUpsideDown(boolean upsideDown) {
        this.upsideDown = upsideDown;
    }

    private MediaPlayer mediaPlayer;
    private ScoreSubject scoreSubject = new ScoreSubject();


    private Label label = new Label("Initial Value");
    private double y;
    private boolean upsideDown;


//        private void startBackgroundThread() {
//            playBackgroundAudio();
//
//            // Assuming score is an instance variable that gets updated during the game
//            if (score % 5 == 0) {
//
//            } else if (score % 2 == 0) {
//
//            } else {
//
//            }
//        }


    // Call this method whenever a new score is achieved
    private void addScore(int score) {
        scoresList.add(score);
        scoresList.sort(Collections.reverseOrder());
    }



    public void start(Stage primaryStage) {

//        MediaPlayerSingleton.getInstance();
        try{
            String filePath = "src/main/java/com/javafx/game/game_audio.wav";
            SimpleAudioPlayer audioPlayer =new SimpleAudioPlayer(filePath);
            audioPlayer.play();
        }

        catch (Exception ex)
        {
            System.out.println("Error with playing sound.");
            ex.printStackTrace();
        }

        Button playGameButton = new Button("Play Game");
        playGameButton.setFont(new Font("Arial", 45));
        playGameButton.setTextFill(Color.PINK);
        playGameButton.setPadding(new Insets(30, 45, 30, 45));
        playGameButton.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(2))));

        Rectangle backgroundShape = new Rectangle(300, 120);
        backgroundShape.setArcWidth(60); // Rounded corners
        backgroundShape.setArcHeight(60);
        playGameButton.setShape(backgroundShape);
        playGameButton.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(20), Insets.EMPTY)));

        Button highScoresButton = new Button("High Scores");
        highScoresButton.setFont(new Font("Arial", 16));
        highScoresButton.setTextFill(Color.PINK);
        highScoresButton.setPadding(new Insets(10, 20, 10, 20));
        highScoresButton.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(2))));

        Rectangle highscoresBackground = new Rectangle(100, 40);
        highscoresBackground.setArcWidth(20); // Rounded corners
        highscoresBackground.setArcHeight(20);
        highScoresButton.setShape(highscoresBackground);
        highScoresButton.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(20), Insets.EMPTY)));



        playGameButton.setOnMouseClicked(event -> showGameScreen(primaryStage));
        highScoresButton.setOnMouseClicked(event -> showHighScoreScreen(primaryStage));

        VBox buttonPanel = new VBox(10);
        buttonPanel.getChildren().addAll(playGameButton, highScoresButton);
        buttonPanel.setAlignment(Pos.CENTER);

        Image backgroundImage = new Image(getClass().getResourceAsStream("/images/background.jpg"));
        BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, true);
        BackgroundImage background = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);

        StackPane panelStackPane = new StackPane();
        panelStackPane.setBackground(new Background(background));
        panelStackPane.getChildren().add(buttonPanel);

        Scene panelScene = new Scene(panelStackPane, 1600, 800);

        primaryStage.setScene(panelScene);
        primaryStage.setTitle("Game Panel");
        primaryStage.show();

        // Start the background thread
//            startBackgroundThread();
    }

    private void showHighScoreScreen(Stage primaryStage) {
        Stage highScoreStage = new Stage();
        highScoreStage.initModality(Modality.APPLICATION_MODAL);
        highScoreStage.initOwner(primaryStage);

        Text titleLabel = new Text("High Scores");
        titleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        titleLabel.setFill(Color.DARKBLUE);
        titleLabel.setTranslateY(10);

        // Display top 5 scores
        List<Integer> topScores = getTopScores();

        VBox scoreBox = new VBox(10);
        System.out.println(topScores.size());
        for (int i = 0; i < topScores.size(); i++) {
            Text scoreText = new Text((i + 1) + ". " + topScores.get(i));
            scoreText.setFont(Font.font("Verdana", FontWeight.NORMAL, 16));
            scoreText.setFill(Color.DARKGREEN);
            scoreBox.getChildren().add(scoreText);
        }
        scoreBox.setAlignment(Pos.CENTER);

        Button closeButton = new Button("Close");
        closeButton.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        closeButton.setOnAction(e -> highScoreStage.close());
        closeButton.setStyle("-fx-background-color: darkblue; -fx-text-fill: white;");
        closeButton.setPadding(new Insets(10, 20, 10, 20));

        Button homeButton = new Button("Home");
        homeButton.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        homeButton.setOnAction(e -> {
            highScoreStage.close();
            start(primaryStage);
        });
        homeButton.setStyle("-fx-background-color: darkblue; -fx-text-fill: green;");
        homeButton.setPadding(new Insets(10, 20, 10, 20));

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        layout.getChildren().addAll(titleLabel, scoreBox, homeButton, closeButton);

        Scene highScoreScene = new Scene(layout, 400, 400);
        highScoreStage.setScene(highScoreScene);
        highScoreStage.setTitle("High Scores");
        highScoreStage.showAndWait();

        highScoreScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                restartGame();
                highScoreStage.close();
            }
        });
    }

    private List<Integer> getTopScores() {
        // Assuming scoresList is sorted in descending order
        return scoresList.subList(0, Math.min(5, scoresList.size()));
    }

//        private void playBackgroundAudio() {
//            // Replace "game_audio.mp3" with the actual path to your audio file
//            String audioFile = "com/javafx/game/game_audio.mp3";
//            Media sound = new Media(new File(audioFile).toURI().toString());
//
//            // Release the previous media player resources before creating a new one
//            if (mediaPlayer != null) {
//                mediaPlayer.stop();
//                mediaPlayer.dispose();
//                mediaPlayer.dispose();
//                mediaPlayer.dispose();
//            }
//
//            mediaPlayer = new MediaPlayer(sound);
//
//            // Set the audio to loop indefinitely
//            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
//
//            // Play the audio
//            mediaPlayer.play();
//        }

    private void updateHighScores(int score) {
        scoresList.add(score);
        scoresList.sort(Collections.reverseOrder());
    }

    private Rectangle createPlatform(double xPosition, double width, Pane gameScrollPane) {
        Rectangle platform = new Rectangle(xPosition, GROUND_Y_POS, width, 200);
        platform.setFill(Color.web("#424242"));

        double redPortionWidth = width / 5;
        Rectangle redPortion = new Rectangle(xPosition + width / 2 - redPortionWidth / 2, GROUND_Y_POS, redPortionWidth, 20);
        redPortion.setFill(Color.RED);

        gameScrollPane.getChildren().addAll(platform, redPortion);

        return platform;
    }


    private void showGameScreen(Stage primaryStage) {
        Pane rootPane = new Pane();

        Scene scene = new Scene(rootPane, 1600, 800, Color.LIGHTGRAY);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Stick Hero FX");
        primaryStage.show();

        Image backgroundImage = new Image(getClass().getResourceAsStream("/images/background.jpg"));
        ImageView backgroundImageView = new ImageView(backgroundImage);
        rootPane.getChildren().add(backgroundImageView);

        Pane gameScrollPane = new Pane();
        rootPane.getChildren().add(gameScrollPane);

        playerGroup = new Group();
        gameScrollPane.getChildren().add(playerGroup);

        Rectangle body = new Rectangle(48, 60, Color.BLACK);
        body.setX(20);
        body.setY(0);
        body.setArcWidth(30);
        body.setArcHeight(30);
        playerGroup.getChildren().add(body);

        Rectangle leftLeg = new Rectangle(10, 20, Color.BLACK);
        leftLeg.setX(21);
        leftLeg.setY(50);
        leftLeg.setArcWidth(10);
        leftLeg.setArcHeight(10);
        playerGroup.getChildren().add(leftLeg);

        Rectangle rightLeg = new Rectangle(10, 20, Color.BLACK);
        rightLeg.setX(57);
        rightLeg.setY(50);
        rightLeg.setArcWidth(10);
        rightLeg.setArcHeight(10);
        playerGroup.getChildren().add(rightLeg);

        Circle eye = new Circle(5, Color.WHITE);
        eye.setCenterX(57);
        eye.setCenterY(22);
        playerGroup.getChildren().add(eye);

        Rectangle scarf = new Rectangle(50, 13, Color.RED);
        scarf.setX(19);
        scarf.setY(7);
        playerGroup.getChildren().add(scarf);

        Polygon peak1 = new Polygon(new double[]{
                20.0, 15.0,
                10.0, 32.0,
                25.0, 30.0
        });
        peak1.setFill(Color.RED);
        playerGroup.getChildren().add(peak1);

        Polygon peak2 = new Polygon(new double[]{
                20.0, 13.0,
                0.0, 10.0,
                7.0, 25.0
        });
        peak2.setFill(Color.RED);
        playerGroup.getChildren().add(peak2);

        playerXPosition = PLATFORM_1_WIDTH - BAR_WIDTH - playerGroup.getLayoutBounds().getWidth();
        playerYPosition = GROUND_Y_POS - playerGroup.getLayoutBounds().getHeight();
        playerGroup.setLayoutX(playerXPosition);
        playerGroup.setLayoutY(playerYPosition);

        VBox infoPane = new VBox();
        infoPane.setPrefWidth(scene.getWidth());
        infoPane.setAlignment(Pos.CENTER);
        scoreLabel = new Label(String.valueOf(score));
        scoreLabel.setFont(new Font(60));

        highScoreLabel = new Label("High Score: " + getHighScore());
        highScoreLabel.setFont(new Font(30));

        gameoverLabel = new Label("Game Over");
        gameoverLabel.setVisible(false);
        gameoverLabel.setFont(new Font(60));

        infoPane.getChildren().addAll(scoreLabel, highScoreLabel, gameoverLabel);
        rootPane.getChildren().add(infoPane);

        Rectangle platform1 = new Rectangle(0, GROUND_Y_POS, PLATFORM_1_WIDTH, 200);
        platform1.setFill(Color.web("#424242"));
        gameScrollPane.getChildren().add(platform1);

        Random random = new Random();
        newXPlatformPosition = minStartPlatforms + random.nextInt(250);
        newPlatformWidth = minPlatformWidth + random.nextInt(25);

        createPlatform(newXPlatformPosition, newPlatformWidth, gameScrollPane);

        holeXPosition = PLATFORM_1_WIDTH;
        barSize = START_BAR_SIZE;
        barRectangle = new Rectangle(
                holeXPosition - BAR_WIDTH, GROUND_Y_POS,
                BAR_WIDTH, barSize);
        barRectangle.setFill(Color.web("#DEB887"));
        barRotationAngle = 0;
        rotateCurrentBar = new Rotate(barRotationAngle, holeXPosition, GROUND_Y_POS + START_BAR_SIZE);
        barRectangle.getTransforms().add(rotateCurrentBar);
        gameScrollPane.getChildren().add(barRectangle);
        playerGroup.toFront();

        barGrowTimeline = new Timeline(new KeyFrame(Duration.seconds(0.017), (ActionEvent ae) -> {
            barSize += barGrowthSpeed;
            barRectangle.setY(GROUND_Y_POS + START_BAR_SIZE - barSize);
            barRectangle.setHeight(barSize);
        }));
        barGrowTimeline.setCycleCount(Timeline.INDEFINITE);

        barRotateTimeline = new Timeline(new KeyFrame(Duration.seconds(0.017), (ActionEvent ae) -> {
            barRotationAngle += BAR_ROTATION_SPEED;
            rotateCurrentBar.setAngle(barRotationAngle);
            if (barRotationAngle >= 90) {
                barRotateTimeline.stop();
                playerRunTimeline.play();
            }
        }));
        barRotateTimeline.setCycleCount(Timeline.INDEFINITE);

        double backgroundScrollSpeed = 0.01;

        scene.setOnKeyPressed(event -> {
            if (!gameOver) {
                if (event.getCode() == KeyCode.SPACE) {
                    // Shift the character vertically
                    playerYPosition += 60;  // You can adjust the value based on your design
                    playerGroup.setLayoutY(playerYPosition);
                }
                // Rotate the player upside down
                if (!upsideDown) {
                    playerGroup.setRotate(180);
                    playerYPosition += 60;// Rotate 180 degrees
                } else {
                    playerGroup.setRotate(0);
                    playerYPosition -= 60;
                    // Reset rotation
                }
                upsideDown = !upsideDown;
                Timeline resetPositionTimeline = new Timeline(new KeyFrame(Duration.seconds(0.2), e -> {
                    playerYPosition -= 60;  // Adjust back to the original position
                    playerGroup.setLayoutY(playerYPosition);
                }));
                resetPositionTimeline.setCycleCount(1); // Play only once
                resetPositionTimeline.play();
            }
        });
        playerRunTimeline = new Timeline(new KeyFrame(Duration.seconds(0.017), (ActionEvent ae) -> {
            playerGroup.setLayoutX(playerXPosition + playerXIncrement);
            playerXIncrement += PLAYER_RUN_SPEED;

            backgroundImageView.setLayoutX(backgroundImageView.getLayoutX() - playerXIncrement * backgroundScrollSpeed);
            if (backgroundImageView.getLayoutX() < -backgroundImage.getWidth()) {
                backgroundImageView.setLayoutX(0);
            }

            if (playerXIncrement >= barSize) {
                double holeSize = newXPlatformPosition - holeXPosition;
                if (barSize < holeSize || barSize > holeSize + newPlatformWidth) {
                    if (playerXIncrement >= barSize + playerGroup.getLayoutBounds().getWidth() * 0.5) {
                        playerDieTimeline.play();
                        playerRunTimeline.stop();
                        gameOver = true;
                        gameoverLabel.setVisible(true);
                        barGrowthSpeed = BAR_ROTATION_SPEED;
                        updateHighScore();
                    }
                } else {
                    if (playerXIncrement >= holeSize + newPlatformWidth) {
                        playerRunTimeline.stop();
                        holeXPosition = newXPlatformPosition + newPlatformWidth;
                        barSize = START_BAR_SIZE;
                        barRectangle = new Rectangle(
                                holeXPosition - BAR_WIDTH, GROUND_Y_POS,
                                BAR_WIDTH, barSize);
                        barRectangle.setFill(Color.web("#DEB887"));
                        barRotationAngle = 0;
                        rotateCurrentBar = new Rotate(barRotationAngle, holeXPosition, GROUND_Y_POS + START_BAR_SIZE);
                        barRectangle.getTransforms().add(rotateCurrentBar);
                        playerGroup.toFront();

                        TranslateTransition translateTransition =
                                new TranslateTransition(Duration.millis(1000), gameScrollPane);
                        translateTransition.setByX(-holeSize - newPlatformWidth);
                        translateTransition.play();

                        newXPlatformPosition += minStartPlatforms + random.nextInt(250);
                        newPlatformWidth = minPlatformWidth + random.nextInt(25);
                        createPlatform(newXPlatformPosition, newPlatformWidth, gameScrollPane);

                        gameScrollPane.getChildren().add(barRectangle);
                        playerGroup.toFront();

                        playerXPosition += playerXIncrement;
                        playerXIncrement = 0;

                        score++;
                        scoreLabel.setText(String.valueOf(score));

                        if (score % 5 == 0) {
                            barGrowthSpeed++;
                        }
                    }
                }
            }
        }));
        playerRunTimeline.setCycleCount(Timeline.INDEFINITE);

        playerDieTimeline = new Timeline(new KeyFrame(Duration.seconds(0.017), (ActionEvent ae) -> {
            playerGroup.setLayoutY(playerYPosition + playerYIncrement);
            playerYIncrement += 5;
            if (playerYPosition + playerYIncrement > scene.getHeight()) {
                playerDieTimeline.stop();
            }
        }));
        playerDieTimeline.setCycleCount(Timeline.INDEFINITE);

        Timeline cherryTimeline = new Timeline(new KeyFrame(Duration.seconds(2), (ActionEvent ae) -> {
            // Only create a new cherry if there isn't one already
            if (cherries.isEmpty()) {
                double endOfCurrentPlatform = holeXPosition + PLATFORM_1_WIDTH;
                double startOfNextPlatform = newXPlatformPosition;
                double cherryXPosition = (endOfCurrentPlatform + startOfNextPlatform) / 2;
                double cherryYPosition = GROUND_Y_POS - CHERRY_RADIUS - 20;

                Cherry cherry = new Cherry(CHERRY_RADIUS, cherryXPosition, cherryYPosition, 100, Color.RED);
                cherries.add(cherry);
                gameScrollPane.getChildren().add(cherry);

                TranslateTransition translateCherry = new TranslateTransition(Duration.seconds(5), cherry);
                translateCherry.setToX(-CHERRY_RADIUS * 2);
                translateCherry.play();
            }
        }));

        cherryTimeline.setCycleCount(Timeline.INDEFINITE);
        cherryTimeline.play();

        scene.setOnMousePressed((MouseEvent mouseEvent) -> {
            if (!gameOver) {
                barSize = START_BAR_SIZE;
                barGrowTimeline.play();
            }
        });

        scene.setOnMouseReleased((MouseEvent mouseEvent) -> {
            if (!gameOver)
            {
                barGrowTimeline.stop();
                barRotateTimeline.play();
            }
            else if (revive)
            {
                revivePlayer();
            }
             else
             { // RESTART GAME
                System.out.println("Replay");
                replayButton = new Button("Replay");
                scoresList.add(score);
                replayButton.setOnAction(event -> {
                    restartGame();
                    replayButton.setVisible(false);
                    homeButton.setVisible(false); // Make sure homeButton is hidden on replay
                });
                replayButton.setLayoutX(scene.getWidth() / 2 - 50);
                replayButton.setLayoutY(scene.getHeight() - 50);
                rootPane.getChildren().add(replayButton);

                homeButton = new Button("Home");
                homeButton.setOnAction(event -> {
                    start(primaryStage);
                });
                homeButton.setLayoutX(scene.getWidth() / 2 + 50);
                homeButton.setLayoutY(scene.getHeight() - 50);
                rootPane.getChildren().add(homeButton);
                homeButton.setVisible(true); // Set homeButton to visible
            }
        });
        Timeline cherryCollisionTimeline = new Timeline(new KeyFrame(Duration.seconds(0.017), (ActionEvent ae) -> {
//            checkCherryCollisions();
            collectCherry(cherries, gameScrollPane);  // Pass the gameScrollPane as a parameter

        }));
        cherryCollisionTimeline.setCycleCount(Timeline.INDEFINITE);
        cherryCollisionTimeline.play();
    }

    private void checkCherryCollisions() {
        Iterator<Cherry> iterator = cherries.iterator();
        while (iterator.hasNext()) {
            Cherry cherry = iterator.next();
            if (playerGroup.getBoundsInParent().intersects(cherry.getBoundsInParent())) {
                // Player has collected a cherry
                cherry.collect(); // Perform any action on cherry collection, like updating the score

                // Remove the cherry from the scene and the list
                gameScrollPane.getChildren().remove(cherry);
                iterator.remove();
            }
        }
    }


    private void collectCherry(List<Cherry> cherries, Pane gameScreenPane) {
        Iterator<Cherry> iterator = cherries.iterator();
        while (iterator.hasNext()) {
            Cherry cherry = iterator.next();
            if (playerGroup.getBoundsInParent().intersects(cherry.getBoundsInParent())) {
                // Player has collected the cherry
                iterator.remove();
                gameScreenPane.getChildren().remove(cherry);
                // Update your score or perform any other actions
                score += CHERRY_SCORE;
                scoreLabel.setText(String.valueOf(score));

                // Increment cherriesCollected
                cherriesCollected++;

                // Check if cherriesCollected is greater than 5
                if (cherriesCollected > 5) {
                    cherriesCollected = 0;
                    revive=true;
                }
            }
        }
    }

    private void revivePlayer() {
        // Implement the logic to revive the player
        // For example, reset player position, clear obstacles, etc.
        gameOver = false;
        gameoverLabel.setVisible(false);
        replayButton.setVisible(false); // Make sure replayButton is hidden when going back home
        homeButton.setVisible(false);
        scoreLabel.setText(String.valueOf(score));
        playerGroup.setLayoutY(playerYPosition);
        playerGroup.setLayoutX(playerXPosition);

        playerYIncrement = 0;
        playerXIncrement = 0;
        barSize = START_BAR_SIZE;
        barRectangle.setHeight(barSize);
        barRectangle.setY(GROUND_Y_POS + START_BAR_SIZE - barSize);
        barRotationAngle = 0;
        rotateCurrentBar.setAngle(barRotationAngle);
        updateHighScore();
    }

    private void restartGame() {

        gameOver = false;
        gameoverLabel.setVisible(false);
        replayButton.setVisible(false); // Make sure replayButton is hidden when going back home
        homeButton.setVisible(false);
        score = 0;
        scoreLabel.setText(String.valueOf(score));
        playerGroup.setLayoutY(playerYPosition);
        playerGroup.setLayoutX(playerXPosition);

        playerYIncrement = 0;
        playerXIncrement = 0;
        barSize = START_BAR_SIZE;
        barRectangle.setHeight(barSize);
        barRectangle.setY(GROUND_Y_POS + START_BAR_SIZE - barSize);
        barRotationAngle = 0;
        rotateCurrentBar.setAngle(barRotationAngle);
        updateHighScore();
    }

    private int getHighScore() {
        // Implement logic to retrieve the high score from your storage (file, database, etc.)
        // Return 0 if high score retrieval fails or if no high score is stored
        return 0;
    }


    private void updateHighScore() {
        int currentHighScore = getHighScore();
        if (score > currentHighScore) {
            // Update the high score
            // Implement logic to store the new high score in your storage
            highScoreLabel.setText("High Score: " + score);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}