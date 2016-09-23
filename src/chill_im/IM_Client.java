package chill_im;

/**
 * Created by chill / doggy on 9/10/16.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class IM_Client {
    private static Socket host;
    private static Scanner userInput;
    private static final int PORT = 1234;
    private static String userName;
    private static Scanner networkInput;
    private static PrintWriter networkOutput;

    static boolean isConnected = true;

    public IM_Client(Socket socket, String userName) {
        host = socket;
        IM_Client.userName = userName;
    }
    // Client needs a thread to send Heart Beat to the Server

    public static void main(String[] args) {
        setUserName();
        networkConfigMenu();

        IM_Client client = new IM_Client(host, userName);

        // Tests connection to server
        // Also sends Join Command to Server
        client.connectToServer();

        // SEND COMMANDS
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();


            // logout if message is QUIT
            if(msg.equals("QUIT")) {
                sendMessage("QUIT "+userName);
                isConnected = false;
                break;
                // See all active users
            } else if(msg.equals("LIST")) {
                sendMessage("LIST");
            } else if (msg.length() > 250) {
                System.out.println("Max 250 Characters!");
                sendMessage("DATA " + userName + " : " + msg.substring(0, 249));
            }else {
                sendMessage("DATA " + userName + " : " + msg + "");
            }
        }
        // done disconnect
        client.disconnect();
    }

    private static void networkConfigMenu(){
        String hostIP;
        System.out.println(
                "Enter 1: for Manual Config " +
                        "\nEnter 2: for LocalHost Config\n");
        switch (userInput.nextInt()) {
            case 1:

                System.out.println("Enter Server IP: ");
                hostIP = userInput.nextLine();
                hostIP += userInput.nextLine();

                System.out.print("Enter Server PORT: ");
                int port = userInput.nextInt();

                try {
                    host = new Socket(hostIP, port);
                } catch (IOException e) {
                    System.out.println("\nHost ID not found!");
                    System.exit(1);
                }
                break;
            case 2:
                hostIP = "127.0.0.1";
                try {
                    host = new Socket(hostIP, PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private static void setUserName() {
        userInput = new Scanner(System.in);

        System.out.print("Enter UserName: ");
        userName = userInput.next();

        if (userName.length() > 12) {
            System.out.println("Max 12 Characters!");
            System.out.println("Please Try Again...");
            System.out.print("Enter UserName: ");
            userName = userInput.next();
        }
    }

    private void connectToServer(){
        try {
            // Send message to server  via network output stream...
            // Receive responses from server via network input stream

            networkInput = new Scanner(host.getInputStream());
            networkOutput = new PrintWriter(host.getOutputStream(), true);

        } catch (IOException ioEx){
            display("Exception creating new Input/output Streams: " + ioEx);
            isConnected = false;
        }

        new ListenFromServer().start();

        // JOIN Protocol client --> server
        sendMessage("JOIN "+userName+", "+host.getInetAddress().getHostAddress()+":"+
                host.getPort()+"");

        isConnected = true;

        Thread alive = new Thread(() -> {
            while (true) {
                try {
                    sendMessage("ALVE "+userName);
                    Thread.sleep(1000*60);
                } catch (Exception e) {}
            }
        });
        alive.start();
    }


    // Show Client a local Message
    private static void display(String msg) {
        System.out.println(msg);
    }

    // Client --> Server Message
    private static void sendMessage(String msg) {
        networkOutput.println(msg);
    }

    //  When something goes wrong
    //  Close the Input/Output streams and disconnect not much to do in the catch clause
    private void disconnect() {
        try {
            if(networkInput != null) networkInput.close();
        }
        catch(Exception ignored) {}
        try {
            if(networkOutput != null) networkOutput.close();
        }
        catch(Exception ignored) {}
        try{
            if(host != null) host.close();
        }
        catch(Exception ignored) {}

        System.exit(1);

    }

    // a class that waits for the message from the server and append them to the JTextArea
    // if we have a GUI or simply System.out.println() it in console mode

    private class ListenFromServer extends Thread {

        public void run() {
            while(isConnected) {
                String msg = networkInput.nextLine();

                // RECEIVED PROTOCOLS
                switch (msg) {
                    case "J_ERR":
                        // validation failed
                        display(msg);
                        break;
                    case "J_OK":
                        // validation successful;
                        isConnected = true;
                        break;
                    case "LIST":
                        display(msg);
                        break;
                }

                // Print the message and add back the prompt
                System.out.println(msg);
            }

        }
    }
}
