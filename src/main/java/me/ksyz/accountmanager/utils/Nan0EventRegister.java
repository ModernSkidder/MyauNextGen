package me.ksyz.accountmanager.utils;

import me.ksyz.accountmanager.Events;

public class Nan0EventRegister {

    public static void register(Object target) {
        if (target instanceof Events) {
            ((Events) target).init();
        }
    }
}
