package com.nclab.ncmultipeerchat;

/**
 * control the whole chat
 */
public class MultiplayerController {

    private static MultiplayerController ourInstance = new MultiplayerController();

    public static MultiplayerController getInstance() {
        return ourInstance;
    }

    private MultiplayerController() {
    }
}
