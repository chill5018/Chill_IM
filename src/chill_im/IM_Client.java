package chill_im;

/**
 * Created by chill on 9/10/16.
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

    public IM_Client(Socket socket, String userName) {
        host = socket;
        IM_Client.userName = userName;
    }
    // Client has a thread to send Heart Beat to the Server

    public static void main(String[] args) {

        setUserName();
        networkConfigMenu();

        IM_Client client = new IM_Client(host, userName);

        while (!client.JOIN(userName, host)) {
            setUserName();
            networkConfigMenu();
            client.JOIN(userName, host);
        }

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("QUIT")) {
                sendMessage("QUIT");
                // break to do the disconnect
                break;
            } else if(msg.equalsIgnoreCase("LIST")) {
                // message WhoIsIn
                sendMessage("LIST");
            } else {
                // default to ordinary message
                sendMessage("DATA "+msg);
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

    private boolean JOIN(String userName, Socket host){
        // Containers for messages, and responses
        String response ="";
        try {
            // Send message to server  via network output stream...
            // Receive responses from server via network input stream

            networkInput = new Scanner(host.getInputStream());
            networkOutput = new PrintWriter(host.getOutputStream(), true);

        } catch (IOException ioEx){
            display("Exception creating new Input/output Streams: " + ioEx);
            return false;
        }

        new ListenFromServer().start();

        // JOIN Protocol client --> server
        sendMessage("JOIN "+userName+" , "+host.getInetAddress().getHostAddress()+" : "+
                host.getPort()+"");

//        new ListenFromServer().start();
//
//        //  Authentication Response from Server
//        response = networkInput.nextLine();
//
//        //Display Server's response to user..
//        System.out.println("\nSERVER> " + response);

        // Authentication Message from Server
//        if (response.equals("J_OK")){
//            // Allow Access
//            display("Access Granted");
//        } else if (response.equals("J_ERR")) {
//            // Try a Diff User Name
//            display("Access Denied");
//            return false;
//        }
        return true;
    }

    private static void DATA() {
        // Set up stream for keyboard entry ...
        Scanner userEntry = new Scanner(System.in);

        // Containers for messages, and responses
        String message, response;

        do {
            System.out.print("Enter message('QUIT' to exit): ");
            message = userEntry.nextLine();

            if (message.length() > 250) {
                System.out.println("Max 250 Characters!");
                sendMessage("DATA " + userName + " : " + message.substring(0, 249));

            } else if (message.equals("QUIT")){
                sendMessage("QUIT " + userName + " : " + message + "");
            }else {
                sendMessage("DATA " + userName + " : " + message + "");
            }

            response = networkInput.nextLine();

            //Display Server's response to user..
            System.out.println("\nSERVER> " + response);

        } while (!message.equals("QUIT"));
        try {
            if (host != null) {
                System.out.println(" Client Closing down connection ...");

                host.close();
            }
        } catch (IOException ioEx){
            System.out.println(" Client Unable to disconnect!");
        }
    }

    // Connect to the server
    private boolean connectToServer() {
        // try to connect to the server
        try {
            host = new Socket(host.getInetAddress(), PORT);
        }
        // if it failed not much I can so
        catch(Exception ec) {
            display("Error connecting to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + host.getInetAddress() + ":" + host.getPort();
        display(msg);
		/* Creating both Data Stream */
        try
        {
            networkInput  = new Scanner(host.getInputStream());
            networkOutput = new PrintWriter(host.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        networkOutput.print(userName);
        // success we inform the caller that it worked
        return true;
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

    }

    // a class that waits for the message from the server and append them to the JTextArea
    // if we have a GUI or simply System.out.println() it in console mode

    private class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                String msg = networkInput.nextLine();
                // Print the message and add back the prompt

                if (msg.equals("J_ERR")) {
                    // validation failed
                    setUserName();
                } else if (msg.equals("J_OK")) {
                    // validation successful;
                }

                System.out.println(msg);
                System.out.print("> ");

            }

        }
    }
}
