package com.dtbafrica.scf.ussd.engine;

/**
 * The Infobip response body. Field names match the gateway contract so this maps
 * straight onto JSON.
 */
public record UssdResponse(
        boolean shouldClose,
        String ussdMenu,
        int responseExitCode,
        String responseMessage) {

    public static UssdResponse ok(boolean shouldClose, String menu) {
        return new UssdResponse(shouldClose, menu, 200, "");
    }
}
