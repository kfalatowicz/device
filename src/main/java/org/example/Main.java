package org.example;

public class Main {
    public static void main(String[] args) {
        DeviceWithGateway deviceA = new DeviceWithGateway("127.0.0.1", 1234, 500, 0, 100, false,0, false, 12345);
        deviceA.startGateway();

        DeviceWithGateway deviceB = new DeviceWithGateway("127.0.0.1", 1234, 500, 0, 100, true,1000, true, 67890);
        deviceB.startGateway();
    }
}
