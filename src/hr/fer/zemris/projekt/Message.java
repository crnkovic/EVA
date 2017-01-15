package hr.fer.zemris.projekt;

import javafx.scene.control.Alert;

public class Message {
    /**
     * Show warning message to the user.
     *
     * @param title Title of the message
     * @param body  Body of the message
     */
    public static void warning(String title, String body) {
        show(title, body, "warning");
    }

    /**
     * Show information message to the user.
     *
     * @param title Title of the message
     * @param body  Body of the message
     */
    public static void info(String title, String body) {
        show(title, body, "info");
    }

    /**
     * Show error message to the user.
     *
     * @param title Title of the message
     * @param body  Body of the message
     */
    public static void error(String title, String body) {
        show(title, body, "error");
    }

    /**
     * Show message of any type to the user.
     *
     * @param title Title of the message
     * @param body  Body of the message
     */
    private static void show(String title, String body, String type) {
        Alert.AlertType alertType;

        switch (type) {
            case "error":
                alertType = Alert.AlertType.ERROR;
                break;
            case "warning":
                alertType = Alert.AlertType.WARNING;
                break;
            default:
                alertType = Alert.AlertType.INFORMATION;
        }

        Alert alert = new Alert(alertType);

        alert.setHeaderText(title);
        alert.setContentText(body);
        alert.showAndWait();
    }
}
