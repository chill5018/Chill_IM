package chill_im;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by chill on 9/17/16.
 */
public class IM_Server {
    //Globals
    private static ArrayList clientOutputStreams;
    ArrayList<String> users;

    public static void main(String[] args) {
        IMServer server = new IMServer();
        server.run();
    }

    // TODO: Move this to a separate File
    // Client Handler START
    public static class ClientHandler implements Runnable {
        BufferedReader reader;
        Socket clientSocket;
        PrintWriter clientWriter;

        public ClientHandler(Socket clientSocket, PrintWriter clientWriter) {
            this.clientSocket = clientSocket;
            this.clientWriter = clientWriter;

            try {
                InputStreamReader isReader = new InputStreamReader(clientSocket.getInputStream());
                reader = new BufferedReader(isReader);
            } catch (IOException io){
                System.out.println("Unexpected Error ");
            }
        }

        @Override
        public void run() {
            // Switch to route the protocols to the correct place

        }
    } // End --> ClientHandler



    // Server Thread
    public static class IMServer implements Runnable {
        @Override
        public  void run() {

            clientOutputStreams = new ArrayList();
            // "Create the server"

            try {
                ServerSocket serverSocket = new ServerSocket(1234);

                System.out.println(serverSocket.getInetAddress().getHostAddress() +" "+ serverSocket.getLocalPort());


                do {
                    // Accept incoming client connections
                    Socket clientSocket = serverSocket.accept();

                    // Get the clients outputWriter (meaning messages client sends to server)
                    PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream());

                    // Add client Writer to the list of client output streams
                    clientOutputStreams.add(clientWriter);

                    // Create new Client Handler Thread to listen to Clients
                    Thread listener = new Thread(new ClientHandler(clientSocket, clientWriter));
                    listener.start();

                    System.out.println("Connection Made...");


                } while (true);


            } catch (IOException io) {
                System.out.println("Error Making Connection...");
            }

        }
    }


        // Create all Protocol Methods
            // User Add
            // User Remove
            // List
            // Tell Everyone
            // ALVE
}
