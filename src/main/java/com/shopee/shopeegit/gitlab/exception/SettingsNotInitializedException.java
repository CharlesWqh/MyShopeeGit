package com.shopee.shopeegit.gitlab.exception;

public class SettingsNotInitializedException extends RuntimeException {

    public SettingsNotInitializedException() {
        super("Settings were not initialized in Configuration");
    }

}