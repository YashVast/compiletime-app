package com.compiletime.infrastructure;

import com.compiletime.application.QuestionService;
import com.compiletime.domain.Question;
import com.compiletime.domain.XpRecord;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * A transparent, always-on-top JavaFX overlay window that shows the quiz card.
 *
 * Appears in the bottom-right corner of the primary screen over any application
 * — CMD, File Explorer, VS Code, browser, anything.
 *
 * This is infrastructure — it handles the "how to display" not the "what to display".
 */
@Component
public class OverlayWindow {

    private static final Logger log = LoggerFactory.getLogger(OverlayWindow.class);
    private static final int WIDTH = 400;
    private static final int QUIZ_SECONDS = 45;

    private final QuestionService questionService;
    private final XpRecordRepository xpRecordRepository;

    private Stage stage;
    private Label timerLabel;
    private Label commandLabel;
    private Label questionLabel;
    private VBox optionsBox;
    private Label feedbackLabel;
    private Label xpLabel;

    private Question currentQuestion;
    private Long currentSessionId;
    private Timeline countdown;
    private int secondsLeft;

    public OverlayWindow(QuestionService questionService, XpRecordRepository xpRecordRepository) {
        this.questionService = questionService;
        this.xpRecordRepository = xpRecordRepository;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    public void show(String command, Long sessionId) {
        Optional<Question> q = questionService.getRandomQuestion();
        if (q.isEmpty()) {
            log.warn("[OverlayWindow] No questions available — skipping overlay");
            return;
        }

        currentQuestion = q.get();
        currentSessionId = sessionId;

        Platform.runLater(() -> {
            if (stage == null) buildStage();
            populate(command);
            positionBottomRight();
            stage.show();
            stage.toFront();
            fadeIn();
            startCountdown();
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            if (stage == null || !stage.isShowing()) return;
            stopCountdown();
            fadeOut(() -> stage.hide());
        });
    }

    // ── Stage Construction ──────────────────────────────────────────────────

    private void buildStage() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);

        VBox root = new VBox();
        root.getStyleClass().add("overlay-root");
        root.setPrefWidth(WIDTH);

        root.getChildren().addAll(buildHeader(), buildBody());

        Scene scene = new Scene(root, WIDTH, -1);
        scene.setFill(null); // transparent background
        scene.getStylesheets().add(
                getClass().getResource("/overlay.css").toExternalForm()
        );

        stage.setScene(scene);
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("⚡ CompileTime");
        title.getStyleClass().add("title-label");

        timerLabel = new Label(QUIZ_SECONDS + "s");
        timerLabel.getStyleClass().add("timer-label");

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> hide());

        HBox spacer = new HBox();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, timerLabel, closeBtn);
        return header;
    }

    private VBox buildBody() {
        VBox body = new VBox(10);
        body.getStyleClass().add("body-pane");

        commandLabel = new Label();
        commandLabel.getStyleClass().add("command-label");

        questionLabel = new Label();
        questionLabel.getStyleClass().add("question-label");
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(WIDTH - 32);

        optionsBox = new VBox(6);

        feedbackLabel = new Label();
        feedbackLabel.getStyleClass().add("feedback-label");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setMaxWidth(WIDTH - 32);
        feedbackLabel.setVisible(false);

        xpLabel = new Label();
        xpLabel.getStyleClass().add("xp-label");
        xpLabel.setMaxWidth(Double.MAX_VALUE);
        xpLabel.setAlignment(Pos.CENTER);
        xpLabel.setVisible(false);

        body.getChildren().addAll(commandLabel, questionLabel, optionsBox, feedbackLabel, xpLabel);
        return body;
    }

    // ── Population ──────────────────────────────────────────────────────────

    private void populate(String command) {
        commandLabel.setText("$ " + command);
        questionLabel.setText(currentQuestion.question());

        optionsBox.getChildren().clear();
        feedbackLabel.setVisible(false);
        xpLabel.setVisible(false);
        timerLabel.getStyleClass().remove("timer-urgent");
        timerLabel.setText(QUIZ_SECONDS + "s");

        List<String> options = currentQuestion.options();
        for (int i = 0; i < options.size(); i++) {
            Button btn = new Button(options.get(i));
            btn.getStyleClass().add("option-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setWrapText(true);
            final int index = i;
            btn.setOnAction(e -> onAnswer(index));
            optionsBox.getChildren().add(btn);
        }
    }

    // ── Answer Handling ─────────────────────────────────────────────────────

    private void onAnswer(int selectedIndex) {
        if (currentQuestion == null) return;
        stopCountdown();

        boolean correct = selectedIndex == currentQuestion.correctIndex();
        int xpAwarded = correct ? currentQuestion.xp() : 2;
        int elapsed = QUIZ_SECONDS - secondsLeft;

        // Disable all buttons and highlight correct/incorrect
        for (int i = 0; i < optionsBox.getChildren().size(); i++) {
            Button btn = (Button) optionsBox.getChildren().get(i);
            btn.setDisable(true);
            btn.getStyleClass().remove("option-button");
            if (i == currentQuestion.correctIndex()) {
                btn.getStyleClass().addAll("option-button", "option-correct");
            } else if (i == selectedIndex && !correct) {
                btn.getStyleClass().addAll("option-button", "option-incorrect");
            }
        }

        // Show explanation
        feedbackLabel.setText(currentQuestion.explanation());
        feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-incorrect");
        feedbackLabel.getStyleClass().add(correct ? "feedback-correct" : "feedback-incorrect");
        feedbackLabel.setVisible(true);

        // Show XP
        xpLabel.setText(correct ? "+" + xpAwarded + " XP ✓" : "+" + xpAwarded + " XP");
        xpLabel.setVisible(true);

        // Resize stage to fit new content
        Platform.runLater(() -> stage.sizeToScene());

        // Save XP record to database
        XpRecord record = new XpRecord(currentSessionId, currentQuestion.id(), correct, elapsed * 1000, xpAwarded);
        try {
            xpRecordRepository.save(record);
            log.info("[OverlayWindow] Saved XP record: correct={}, xp={}", correct, xpAwarded);
        } catch (Exception e) {
            log.error("[OverlayWindow] Failed to save XP record", e);
        }

    }

    private void onTimeout() {
        if (currentQuestion == null) return;

        // Reveal the correct answer
        for (int i = 0; i < optionsBox.getChildren().size(); i++) {
            Button btn = (Button) optionsBox.getChildren().get(i);
            btn.setDisable(true);
            if (i == currentQuestion.correctIndex()) {
                btn.getStyleClass().addAll("option-button", "option-correct");
            }
        }

        feedbackLabel.setText("Time's up! " + currentQuestion.explanation());
        feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-incorrect");
        feedbackLabel.getStyleClass().add("feedback-incorrect");
        feedbackLabel.setVisible(true);
        Platform.runLater(() -> stage.sizeToScene());

        XpRecord record = new XpRecord(currentSessionId, currentQuestion.id(), false, QUIZ_SECONDS * 1000, 0);
        try {
            xpRecordRepository.save(record);
        } catch (Exception e) {
            log.error("[OverlayWindow] Failed to save timeout XP record", e);
        }

    }

    // ── Countdown ───────────────────────────────────────────────────────────

    private void startCountdown() {
        secondsLeft = QUIZ_SECONDS;
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft--;
            timerLabel.setText(secondsLeft + "s");

            if (secondsLeft <= 10) {
                if (!timerLabel.getStyleClass().contains("timer-urgent")) {
                    timerLabel.getStyleClass().add("timer-urgent");
                }
            }

            if (secondsLeft <= 0) {
                stopCountdown();
                onTimeout();
            }
        }));
        countdown.setCycleCount(QUIZ_SECONDS);
        countdown.play();
    }

    private void stopCountdown() {
        if (countdown != null) {
            countdown.stop();
        }
    }

    // ── Positioning & Animation ─────────────────────────────────────────────

    private void positionBottomRight() {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.sizeToScene();
        stage.setX(screen.getMaxX() - WIDTH - 24);
        stage.setY(screen.getMaxY() - stage.getHeight() - 24);
    }

    private void fadeIn() {
        stage.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), stage.getScene().getRoot());
        ft.setFromValue(0);
        ft.setToValue(1);
        stage.setOpacity(1);
        ft.play();
    }

    private void fadeOut(Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> onFinished.run());
        ft.play();
    }
}
