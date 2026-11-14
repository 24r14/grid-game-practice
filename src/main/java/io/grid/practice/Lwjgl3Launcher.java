package io.grid.practice;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Run this class to open the grid window. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Grid Practice");
        config.setWindowedMode(920, 500);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new Main(), config);
    }
}
