package smartcity.smartcar.model;

/**
 * Eventi gestiti dall'applicazione.
 */
public enum Event {
    CONNECTION_ESTABLISHED,
    TRYING_TO_CONNECT,
    DISCONNECTED,
    CONNECTION_FAILED,
    CAR_CLOSED,
    CAR_NOT_CLOSED,
    DISCONNECT, // Applicazione stoppata dall'utente attraverso il comando "Disconnect" dalla MainActivity
    BLUETOOTH_DISABLED,
    MESSAGE_RECEIVED,
}
