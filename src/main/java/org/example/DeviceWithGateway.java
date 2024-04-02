package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class DeviceWithGateway {

    private String targetAddress;
    private int targetPort;
    private int sendFrequencyMilliseconds;
    private int minGeneratedValue;
    private int maxGeneratedValue;
    private boolean hasWatchdog;
    private int watchdogResetInterval;
    private boolean acceptGatewayMessages;
    private int deviceCanId;
    private volatile boolean watchdogExpired;


    public DeviceWithGateway(String targetAddress, int targetPort, int sendFrequencyMilliseconds, int minGeneratedValue, int maxGeneratedValue, boolean hasWatchdog, int watchdogResetInterval, boolean acceptGatewayMessages, int deviceCanId) {
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
        this.sendFrequencyMilliseconds = sendFrequencyMilliseconds;
        this.minGeneratedValue = minGeneratedValue;
        this.maxGeneratedValue = maxGeneratedValue;
        this.hasWatchdog = hasWatchdog;
        this.watchdogResetInterval = watchdogResetInterval;
        this.acceptGatewayMessages = acceptGatewayMessages;
        this.deviceCanId = deviceCanId;
    }

    public void startGateway() {
        new Thread(() -> {
            watchdogExpired = false;
            Random random = new Random();
            AtomicLong lastWatchdogResetTime = new AtomicLong(System.currentTimeMillis());

            try {
                boolean connected = false;
                Socket clientSocket;
                DataInputStream inFromClient = null;
                DataOutputStream outToClient = null;

                // Try to connect before doing anything else
                while (!connected) {
                    try {
                        clientSocket = new Socket(targetAddress, targetPort);
                        System.out.println("Device: " + deviceCanId + ". Connected to server.");
                        inFromClient = new DataInputStream(clientSocket.getInputStream());
                        outToClient = new DataOutputStream(clientSocket.getOutputStream());
                        connected = true;
                    } catch (Exception e) {
                        System.out.println("Device: " + deviceCanId + ". Error connecting to: " + targetAddress + ":" + targetPort);
                        Thread.sleep(3000);
                    }
                }

                // Watchdog thread
                if (hasWatchdog) {
                    DataInputStream finalInFromClient = inFromClient;
                    Thread watchdogThread = new Thread(() -> {
                        while (true) {
                            try {
                                // Wait for incoming messages
                                if (acceptGatewayMessages && finalInFromClient.available() > 0) {
                                    byte[] frameBytes = new byte[13];
                                    finalInFromClient.read(frameBytes);
                                    String message = readDataString(frameBytes);

                                    // Check for RESET message
                                    if (message.equals("RESET")) {
                                        lastWatchdogResetTime.set(System.currentTimeMillis());
                                    }
                                }

                                // Check if watchdog timer expired
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastWatchdogResetTime.get() > watchdogResetInterval) {
                                    System.out.println("Device: " + deviceCanId + ". Watchdog timer expired. Restarting outer thread...");
                                    watchdogExpired = true;
                                    return;
                                }

                                Thread.sleep(100); // Check every 100 milliseconds
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    watchdogThread.start();
                }

                while (true) {
                    // Check if the watchdog expired
                    if (watchdogExpired) {
                        throw new WatchdogException("Device: " + deviceCanId + ". Watchdog timer expired. Restarting outer thread...");
                    }

                    // Check for incoming messages
                    if (acceptGatewayMessages && inFromClient.available() > 0) {
                        System.out.println("Device: " + deviceCanId + ". Reading message.");
                        byte[] frameBytes = new byte[13];
                        inFromClient.read(frameBytes);
                        String message = readDataString(frameBytes);

                        // Handle change frequency message
                        if (message.startsWith("F")) {
                            String[] parts = message.split(" ");
                            if (parts.length == 2) {
                                int newFrequency = Integer.parseInt(parts[1]);
                                sendFrequencyMilliseconds = newFrequency;
                                System.out.println("Device: " + deviceCanId + ". New frequency set to: " + newFrequency + " ms");
                            }
                        }
                    }

                    // Create and send frame
                    int data = minGeneratedValue + random.nextInt(maxGeneratedValue - minGeneratedValue + 1);
                    byte[] frame = createFrame(data);
                    outToClient.write(frame);
                    outToClient.flush();
                    System.out.println("Device: " + deviceCanId + ". Frame sent. " + Arrays.toString(frame));

                    // Repeat sending
                    Thread.sleep(sendFrequencyMilliseconds);
                }
            } catch (IOException | InterruptedException | WatchdogException e) {
                e.printStackTrace();
            } finally {
                startGateway();
            }
        }).start();
    }

    private byte[] createFrame(int number) {
        byte[] frame = new byte[13];
        frame[0] = (byte) 0x85; // Frame format: extended frame; frame type: data frame; data length: 5
        frame[1] = (byte) (deviceCanId >> 24);  // CAN ID
        frame[2] = (byte) (deviceCanId >> 16);  // CAN ID
        frame[3] = (byte) (deviceCanId >> 8);   // CAN ID
        frame[4] = (byte) deviceCanId;          // CAN ID
        frame[5] = (byte) ((number >> 32) & 0xFF);  // Data bytes
        frame[6] = (byte) ((number >> 24) & 0xFF);  // Data bytes
        frame[7] = (byte) ((number >> 16) & 0xFF);   // Data bytes
        frame[8] = (byte) ((number >> 8) & 0xFF);    // Data bytes
        frame[9] = (byte) (number & 0xFF);
        frame[10] = 0x00;       // Padding
        frame[11] = 0x00;       // Padding
        frame[12] = 0x00;       // Padding
        return frame;
    }

    private String readDataString(byte[] frame) {
        // Extract the data bytes from the frame
        byte[] dataBytes = new byte[8];
        System.arraycopy(frame, 5, dataBytes, 0, 8);

        // Decode the data bytes as string
        String dataString = new String(dataBytes);

        // Trim any trailing null characters from the string
        dataString = dataString.trim();

        return dataString;
    }

}
