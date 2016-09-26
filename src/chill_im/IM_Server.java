package chill_im;

/**
 * Created by chill / doggy on 9/10/16.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class IM_Server {
    private static ServerSocket serverSocket;
    private static final int PORT = 1234;
    static ArrayList<ClientHandler> clients;
    private static boolean keepGoing = false;
    static SimpleDateFormat sdf;

    public static void main(String[] args) {
        try {
            sdf = new SimpleDateFormat("HH:mm:ss");
            clients = new ArrayList<>();
            serverSocket = new ServerSocket(PORT);
            keepGoing = true;
            display("Ready to connect at Port: "+ PORT);
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

    static boolean validateIncoming(String incoming) {
        //System.out.println("String to Validate: " + incoming);

        String token1 = "", username = "";

        Scanner token = new Scanner(incoming);

        token1 = token.next();
        username = token.next();
        username = username.substring(0,username.length()-1);

        System.out.println("User: "+ username);

        int count = 0;


        // loop in reverse order in case we have to remove a Client
        for(int i = 0; i < clients.size(); i++) {
            ClientHandler ct = clients.get(i);

            if (username.toLowerCase().equals(ct.username.toLowerCase())) {
                count++;
                // If we have two instances then the registration is invalid
                if (count == 2) {
                    return false;
                }
            }
        }

        return true;
    }

    // for a clientSocket who logoff using the QUIT message
    static synchronized void removeUserFromList(String username) {
        int index, count = 0;
        for (index = 0; index <= clients.size() ; index++) {
            if (clients.get(index).username.toLowerCase().equals(username.toLowerCase())) {
                count++;
                if (count == 2) {
                    break;
                }
            }
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
