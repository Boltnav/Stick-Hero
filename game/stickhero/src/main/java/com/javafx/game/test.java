package com.javafx.game;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class test {

    @Test
    public void testScoreLabelUpdate() throws InterruptedException {
        // Create a JavaFX label
        Label scoreLabel = new Label("0");
        ScoreLabel scoreObserver = new ScoreLabel(scoreLabel);

        // Update the score using threading and verify if the label is updated correctly
        runOnJavaFXThread(() -> scoreObserver.update(42));

        // Sleep for a short duration to allow JavaFX thread to process the update
        Thread.sleep(100);

        assertEquals("42", scoreLabel.getText());
    }

    private void runOnJavaFXThread(Runnable runnable) {
        // Use Platform.runLater to execute the provided runnable on the JavaFX application thread
        Platform.runLater(runnable);
    }
}