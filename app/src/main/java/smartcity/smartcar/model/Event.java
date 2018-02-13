package smartcity.smartcar.model;

/**
 * Eventi gestiti dall'applicazione.
 */
public enum Event {
    CONNECTION_ESTABLISHED,
    TRYING_TO_CONNECT,
    DISCONNECTED,
    CAR_CLOSED,
    CAR_NOT_CLOSED,
    DISCONNECT, // Applicazione stoppata dall'utente attraverso il comando "Disconnect" dalla MainActivity
    MESSAGE_RECEIVED
}
