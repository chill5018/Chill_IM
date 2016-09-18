package chill_im;

/**
 * Created by chill on 9/10/16.
 */


import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import static chill_im.IM_Server.*;


public class IM_Server {
    private static ServerSocket serverSocket;
    private static final int PORT = 1234;
    private static int port = 1234;
    static ArrayList<ClientHandler> clients;
    private static ArrayList<String> usernames;
    private static boolean keepGoing = false;

    static SimpleDateFormat sdf;

    private IM_Server(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        clients = new ArrayList<>();
    }


    public static void main(String[] args) {
        try {
            sdf = new SimpleDateFormat("HH:mm:ss");
            usernames = new ArrayList<>();
            clients = new ArrayList<>();
            serverSocket = new ServerSocket(PORT);
            keepGoing = true;

        } catch (IOException ioEX) {
            display("\nUnable to set up port!");
            System.exit(1);
        }
        do {
            // Infinite loop Waiting for Clients....
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            display("\nNew Client Attempting to connect...\n");

            // Create a thread to handle communication with clientSocket
            // Pass the constructor for this thread, A reference
            // to the relevant socket...

            ClientHandler handler = new ClientHandler(socket);
            handler.start();

            // Keep List of "Clients" / Threads
            clients.add(handler);

        } while (keepGoing);
        // KeepGoing == False --> Close Connection
        try {
            serverSocket.close();
            for (ClientHandler tc : clients)
                try {
                    tc.inputClient.close();
                    tc.outputClient.close();
                    tc.clientSocket.close();
                } catch (IOException ignored) {}
        }
        catch(Exception e) {
            display("Exception closing the server and clients: " + e);
        }
    }

    static String validateIncoming(String incoming) {
        System.out.println("String to Validate: " + incoming);

        String username ="";
        String hostIP ="";
        String port ="";
        String cmd = "";

        Scanner token = new Scanner(incoming);

        while (token.hasNext()) {
            cmd = token.next();
            username = token.next();
            String bufferToken = token.next();
            hostIP = token.next();
            bufferToken += token.next();
            port = token.next();


        }

        System.out.println("CMD: "+ cmd);
        System.out.println("User: "+ username);
        System.out.println("Host: "+ hostIP);
        System.out.println("Port: "+ port);


            int count = 0;
        for (int i = 0; i < clients.size() ; i++) {
            if (username.toLowerCase().equals(clients.get(i).username.toLowerCase())) {
                count++;
                if (count == 2) {
                    // If the most recent is already in the list then remove it
                    clients.remove(i);
                    return "J_ERR";
                }
            }
        }
        usernames.add(username);
        return "J_OK";
    }

    // for a clientSocket who logoff using the QUIT message
    static synchronized void removeUserFromList(String username) {
        for (int i = 0; i < clients.size() ; i++) {
            if (username.toLowerCase().equals(clients.get(i).username.toLowerCase()))
                usernames.remove(i);
        }
    }

    // To Display something to the console only
    static void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);

    }

    // To Share with all connected users
    static synchronized void broadcast(String message) {
        // add HH:mm:ss and \n to the message
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI

        System.out.print(messageLf);


        // loop in reverse order in case we have to remove a Client
        for(int i = clients.size(); --i >= 0;) {
            ClientHandler ct = clients.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(messageLf)) {
                clients.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

}

class ClientHandler extends Thread{
    Socket clientSocket;
    Scanner inputClient; // Handles inputClient from Client
    PrintWriter outputClient; // Handles outputClient from Server

    String username;

    // the date of connection
    String date;

    ClientHandler(Socket socket){
        //Set up reference to associated socket..
        this.clientSocket = socket;

        try {
            // Create output first
            outputClient = new PrintWriter(clientSocket.getOutputStream(), true);
            inputClient = new Scanner(clientSocket.getInputStream());


        } catch (IOException ioEx){
            ioEx.printStackTrace();
        }

        date = new Date().toString() + "\n";
    }

    public void run(){
        String received;

        do {
            //Accept Messages from Client on the Socket's Input stream...
            received = inputClient.nextLine();

            // Log What the Users says
            System.out.println(clientSocket.getInetAddress() + ": " + received);

            // Create a token Scanner to reac the first TOKEN == Keyword
            Scanner token = new Scanner(received);

            String cmd = token.next();
            System.out.println("Command: "+ cmd);

            // Create a a switch based on the command tokens SENT FROM CLIENT
            switch (cmd){
                case "JOIN":
                    username =  token.next();
                    writeMsg(validateIncoming(received));
                    break;
                case "DATA":
                    // Echo Message Back to ALL clients on the sockets outputClient stream
                    broadcast("> " + received.substring(4, received.length()));
                    break;
                case "QUIT":
                    // Remove User from List of Users
                    String username = token.next();
                    writeMsg("GoodBye!");
                    display("User to Remove: "+ username);
                    removeUserFromList(username);
                    close();

                    break;
                case "ALVE":
                    // Receive Clients HearBeat --> confirm clientSocket is alive
                    break;
                case "LIST":
                    writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                    // scan al the users connected
                    for(int i = 0; i < clients.size(); ++i) {
                        ClientHandler ct = clients.get(i);
                        writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                    }
            }

            // Repeat above until QUIT sent by clientSocket
        }while (!received.equals("QUIT"));

        try {
            if (clientSocket != null) {
                System.out.println("Closing down connection ...");

                clientSocket.close();
            }
        } catch (IOException ioEx){
            System.out.println("Unable to disconnect!");
        }
    }


    // try to close everything
    private void close() {
        // try to close the connection
        try {
            if(outputClient != null) outputClient.close();
        }
        catch(Exception ignored) {}
        try {
            if(inputClient != null) inputClient.close();
        }
        catch(Exception ignored) {}
        try {
            if(clientSocket != null) clientSocket.close();
        }
        catch (Exception ignored) {}
    }

    // Write a String to an Individual Client outputClient stream
    boolean writeMsg(String msg) {
        // if Client is still connected send the message to them
        if(!clientSocket.isConnected()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        // write the message to the stream
        outputClient.println(msg);
        return true;
    }
}
