package laoqi123.management;

import laoqi123.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File("./config/Myau/", "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}
