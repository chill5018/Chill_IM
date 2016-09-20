package chill_im;

/**
 * Created by chill on 9/10/16.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class IM_Server {
    private static ServerSocket serverSocket;
    private static final int PORT = 1234;
    private static int port = 1234;
    static ArrayList<ClientHandler> clients;
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

            display("\nNew Client Attempting to connect...");

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
            //String bufferToken = token.next();
            hostIP = token.next();
            //bufferToken += token.next();
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
        return "J_OK";
    }

    // for a clientSocket who logoff using the QUIT message
    static synchronized void removeUserFromList(String username) {
        int index;
        for (index = 0; index < clients.size() ; index++) {
            if (username.toLowerCase().equals(clients.get(index).username.toLowerCase()))
                break;
        }
        clients.remove(index);
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

        //System.out.print(messageLf);


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
