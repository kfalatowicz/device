package org.example;

public class Main {
    public static void main(String[] args) {

        String targetAddress = System.getenv("TARGET_ADDRESS");
        if (targetAddress == null) targetAddress = "127.0.0.1";

        String targetPortEnv = System.getenv("TARGET_PORT");
        int targetPort = 1234;
        if (targetPortEnv != null) {
            targetPort = Integer.parseInt(targetPortEnv);
        }

        String deviceIdEnv = System.getenv("DEVICE_ID");
        int deviceId = 0;
        if (deviceIdEnv != null) {
            deviceId = Integer.parseInt(deviceIdEnv);
        }

        String hasWatchdogEnv = System.getenv("WATCHDOG");
        boolean watchdog = false;
        if (hasWatchdogEnv != null) {
            watchdog = Boolean.parseBoolean(hasWatchdogEnv);
        }

        String acceptMessagesEnv = System.getenv("ACCEPT_MESSAGES");
        boolean acceptMessages = false;
        if (acceptMessagesEnv != null) {
            acceptMessages = Boolean.parseBoolean(acceptMessagesEnv);
        }

        DeviceWithGateway deviceA = new DeviceWithGateway(targetAddress, targetPort, 500, 0, 100, watchdog, 500, acceptMessages, deviceId);
        deviceA.startGateway();

    }
}
