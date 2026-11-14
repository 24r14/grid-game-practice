package io.grid.practice;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** A two-dimensional array drawn as a 5 by 5 grid. */
public class Main implements ApplicationListener {
    private ShapeRenderer sr;
    private FitViewport viewport;
    private int player_row = 2;
    private int player_col = 2;
    private Sound collectSound;
    private Sound game_over_sound;
    private enum GameState { READY, PLAYING, GAME_OVER }
    private GameState state = GameState.READY;
    private final Rectangle actionButton = new Rectangle(175, 185, 250, 55);
    private final Vector2 pointer = new Vector2();
    private final GlyphLayout textLayout = new GlyphLayout();
    private BitmapFont font;
    private SpriteBatch batch;
    private float time = 15f;
    private int score = 0;
    private FirebaseScores firebaseScores;
    private String cloudStatus = "Cloud: not configured";
    private int cloudOperation = 0;
    private boolean disposed;
    private final Leaderboard leaderboard = new Leaderboard();
    private java.util.List<Leaderboard.Entry> leaders = java.util.List.of();
    private String username = "";
    private String roundUsername = "";
    private String leaderboardStatus = "Loading...";
    private boolean leaderboardBusy;
    private String retryName;
    private int retryScore;

    // grid[row][column]: 0 = gray, 1 = red, 2 = gold. Indexes start at 0.
    private final int[][] grid = new int[5][5];

    @Override
    public void create() {
        sr = new ShapeRenderer();
        viewport = new FitViewport(920, 500, new OrthographicCamera());
        collectSound = Gdx.audio.newSound(Gdx.files.internal("coin_sound.mp3"));
        game_over_sound = Gdx.audio.newSound(Gdx.files.internal("mario_game_over.mp3"));
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        resetBoard();
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override public boolean keyTyped(char c) {
                if (state == GameState.PLAYING) return false;
                if (c == '\b' && !username.isEmpty()) username = username.substring(0, username.length() - 1);
                else if (username.length() < 16 && ((c >= 'a' && c <= 'z')
                        || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) username += c;
                return true;
            }
        });
        refreshLeaderboard(false);
        if (FirebaseScores.isConfigured()) {
            firebaseScores = new FirebaseScores();
            getScoreFromFirebase();
        }
    }

    private void resetBoard() {
        for (int[] row : grid) {
            java.util.Arrays.fill(row, 0);
        }
        time = 15f;
        score = 0;
        player_row = MathUtils.random(grid.length - 1);
        player_col = MathUtils.random(grid[player_row].length - 1);
        grid[player_row][player_col] = 1;
        spawnGold();
    }

    private void startRound() {
        if (!Leaderboard.validName(username) || leaderboardBusy || retryName != null) return;
        roundUsername = username;
        collectSound.stop();
        game_over_sound.stop();
        resetBoard();
        state = GameState.PLAYING;
    }

    private void spawnGold() {
        int row;
        int col;
        do {
            row = MathUtils.random(grid.length - 1);
            col = MathUtils.random(grid[row].length - 1);
        } while (grid[row][col] != 0);
        grid[row][col] = 2;
    }

    @Override
    public void render() {
        if (state != GameState.PLAYING && Gdx.input.isKeyJustPressed(Input.Keys.F5))
            refreshLeaderboard(retryName != null);
        if (state != GameState.PLAYING) {
            boolean clicked = false;
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                pointer.set(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(pointer);
                clicked = actionButton.contains(pointer);
            }
            if (clicked || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                startRound();
            }
        } else {
            advanceTimer(Gdx.graphics.getDeltaTime());
            if (state == GameState.PLAYING) {
                updatePlayer();
            }
        }
        drawGame();
    }

    private void advanceTimer(float delta) {
        if (state != GameState.PLAYING) return;
        time = Math.max(0f, time - delta);
        if (time <= 0f) {
            state = GameState.GAME_OVER;
            game_over_sound.play(0.5f);
            updateScoreInFirebase(score);
            retryName = roundUsername;
            retryScore = score;
            refreshLeaderboard(true);
        }
    }

    private void refreshLeaderboard(boolean submit) {
        if (leaderboardBusy) return;
        leaderboardBusy = true;
        leaderboardStatus = submit ? "Saving best score..." : "Loading...";
        var request = submit ? leaderboard.submit(retryName, retryScore) : leaderboard.load();
        request.whenComplete((rows, error) -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            leaderboardBusy = false;
            if (error == null) {
                leaders = rows;
                if (submit) retryName = null;
                leaderboardStatus = rows.isEmpty() ? "No scores yet. Play a round!" : "Best score per username";
            } else {
                leaderboardStatus = "Connection/rules error. F5 retries.";
                Gdx.app.log("Leaderboard", "Firebase request failed", error);
            }
        }));
    }

    public void getScoreFromFirebase() {
        if (firebaseScores == null) return;
        cloudStatus = "Cloud: loading...";
        watchCloud(firebaseScores.getScoreFromFirebase(), "Cloud saved score: ");
    }

    public void updateScoreInFirebase(int newScore) {
        if (firebaseScores == null) return;
        cloudStatus = "Cloud: saving...";
        watchCloud(firebaseScores.updateScoreInFirebase(newScore), "Cloud saved score: ");
    }

    private void watchCloud(java.util.concurrent.CompletableFuture<Integer> request, String label) {
        final int operation = ++cloudOperation;
        request.whenComplete((saved, error) -> Gdx.app.postRunnable(() -> {
            if (disposed || operation != cloudOperation) return;
            if (error == null) {
                cloudStatus = label + saved;
            } else {
                cloudStatus = "Cloud unavailable - see Run log";
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                Gdx.app.log("Firebase", cause.getMessage());
            }
        }));
    }

    private void updatePlayer() {
        // Work out the requested position before changing the array.
        int temp_row = player_row;
        int temp_col = player_col;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            temp_row--;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            temp_row++;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            temp_col--;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            temp_col++;
        }

        // Check BOTH indexes before accessing the new cell.
        if (temp_row >= 0 && temp_row < grid.length
                && temp_col >= 0 && temp_col < grid[temp_row].length
                && (temp_row != player_row || temp_col != player_col)) {
            boolean collectedGold = grid[temp_row][temp_col] == 2;
            grid[player_row][player_col] = 0;
            grid[temp_row][temp_col] = 1;
            player_row = temp_row;
            player_col = temp_col;
            if (collectedGold) {
                collectSound.play(0.5f);
                score++;
                time += 0.5f;
                spawnGold();
            }
        }

    }

    private void drawGame() {
        ScreenUtils.clear(0.15f, 0.16f, 0.22f, 1f);
        viewport.apply();
        sr.setProjectionMatrix(viewport.getCamera().combined);

        
        // Draw each row, then each column in that row.
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == 1) {
                    sr.setColor(Color.FIREBRICK);
                } else if (grid[i][j] == 2) {
                    sr.setColor(Color.GOLD);
                } else {
                    sr.setColor(Color.DARK_GRAY);
                }
                // Flip the row index so row 0 appears at the top.
                // Spacing 80 and size 76 leave a 4-pixel gap.
                sr.rect(100 + j * 80, 50 + (grid.length - 1 - i) * 80, 76, 76);
            }
        }
        if (state != GameState.PLAYING) {
            sr.setColor(0.10f, 0.11f, 0.16f, 1f);
            sr.rect(65, 130, 470, 235);
            sr.setColor(Color.GOLD);
            sr.rect(actionButton.x, actionButton.y, actionButton.width, actionButton.height);
        }
        sr.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Score: " + score + " | Time: " + (int)Math.ceil(time) + "s", 10, 485);
        if (state != GameState.PLAYING) {
            font.setColor(Color.GOLD);
            drawCentered(state == GameState.READY ? "GRID PRACTICE" : "GAME OVER", 335);
            font.setColor(Color.WHITE);
            drawCentered(state == GameState.READY ? "Collect gold. Each coin adds 0.5s." : "Final score: " + score, 290);
            font.setColor(Color.BLACK);
            drawCentered(state == GameState.READY ? "START" : "PLAY AGAIN", 220);
            font.setColor(Color.WHITE);
            drawCentered(leaderboardBusy ? "Please wait for Firebase..."
                    : retryName != null ? "Press F5 to retry saving your score"
                    : "Type name below, then click or Enter", 163);
        }
        font.setColor(Color.WHITE);
        drawCentered("Arrow keys to move", 28);
        if (state != GameState.PLAYING) {
            drawCentered("Username: " + username + "_", 85);
            font.getData().setScale(1f);
            drawCentered("1-16 letters/numbers/underscore. Backspace to edit.", 62);
            drawCentered(cloudStatus, 110);
            font.getData().setScale(1.5f);
        }
        font.setColor(Color.GOLD);
        font.draw(batch, "TOP 3 PLAYERS", 610, 440);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        for (int i = 0; i < 3; i++) {
            String line = (i + 1) + ".  --";
            if (i < leaders.size()) line = (i + 1) + ". " + leaders.get(i).username() + " - " + leaders.get(i).score();
            font.draw(batch, line, 610, 385 - i * 45);
        }
        font.getData().setScale(1f);
        font.draw(batch, leaderboardStatus, 610, 220);
        font.draw(batch, "F5: refresh / retry on menu", 610, 190);
        font.draw(batch, "Player: " + (state == GameState.PLAYING ? roundUsername : username), 610, 160);
        font.getData().setScale(1.5f);
        batch.end();
    }

    private void drawCentered(String text, float y) {
        textLayout.setText(font, text);
        font.draw(batch, textLayout, (600 - textLayout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void dispose() {
        disposed = true;
        sr.dispose();
        collectSound.dispose();
        game_over_sound.dispose();
        font.dispose();
        batch.dispose();
    }
}
