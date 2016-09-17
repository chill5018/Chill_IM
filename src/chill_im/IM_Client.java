package chill_im;

import com.sun.corba.se.pept.transport.ListenerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by chill on 9/17/16.
 */
public class IM_Client {
    // Global Vars
    static String userName, address;
    AbstractList<String> users = new ArrayList<>();
    private static final int PORT = 1234;
    private static Boolean isConnected = false;

    // Network Globals
    static Socket host;
    private static BufferedReader reader;
    private static PrintWriter networkOutput;
    private static Scanner userInput = new Scanner(System.in);

    public static void main(String[] args) {


        do {
            setUserName();
            configMenu();
            if (JOIN(userName, host)) {
//                ALVE();
//                DATA();
                System.out.println("Success");
            }
        } while (!JOIN(userName, host));

    }

    private static void configMenu(){
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

    private static boolean JOIN(String userName, Socket host){
        // Containers for messages, and responses
        String response ="";
        try {
            // Send message to server  via network output stream...
            // Receive responses from server via network input stream

            InputStreamReader streamReader = new InputStreamReader(host.getInputStream());
            reader = new BufferedReader(streamReader);
            networkOutput = new PrintWriter(host.getOutputStream());



            // JOIN Protocol client --> server
            networkOutput.println("JOIN "+userName+" , "+host.getInetAddress().getHostAddress()+" : "+
                    host.getPort()+"");

            networkOutput.flush();
            isConnected = true;


            //  Authentication Response from Server
//            response = networkInput.nextLine();

            //Display Server's response to user..
//            System.out.println("\nSERVER> " + response);

        } catch (IOException ioEx){
            ioEx.printStackTrace();
        }

        ListenThread();

        // Authentication Message from Server
        if (response.equals("J_OK")){
            // Allow Access
            System.out.println("Access Granted");
        } else if (response.equals("J_ERR")) {
            // Try a Diff User Name
            System.out.println("Access Denied");
            return false;
        }
        return true;
    }

    public static void ListenThread()
    {
        Thread IncomingReader = new Thread(new IncomingReader());
        IncomingReader.start();
    }

    // Contains Methods to handle Protocols

    // userAdd(String data)
    // userRemove(String data)
    // sendDisconnect()
    // writeUsers()

    // Thread --> Incoming Reader

    public static class IncomingReader implements Runnable {

        // Run () --> Handles Protocol Commands
        @Override
        public void run() {
            String rawData;

            // Contains Switch to call methods accordingly
            try {
                //Accept Messages from Client on the Socket's Input stream...
                rawData = reader.readLine();

                // Create a token Scanner to reac the first TOKEN == Keyword
                Scanner token = new Scanner(rawData);

                String cmd = token.next();
                System.out.println("CMD: " + cmd);

                // Log What the Users sends to Server
                System.out.println(host.getLocalAddress().getHostAddress() + ": " + rawData + "\n");
            } catch (IOException io) {
                io.printStackTrace();
            }


        }
    }



}
