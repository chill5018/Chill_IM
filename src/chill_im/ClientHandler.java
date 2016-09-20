package chill_im;

/**
 * Created by chill on 9/20/16.
 */

import java.io.*;
import java.net.*;
import java.util.*;

import static chill_im.IM_Server.*;

/**
 * Created by Andrei on 19/09/2016.
 */
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

        //boolean flag = true;

        do {
            //Accept Messages from Client on the Socket's Input stream...
            received = inputClient.nextLine();

            // Log What the Users says
            System.out.println(clientSocket.getInetAddress() + ": " + received+"\n");

            // Create a token Scanner to reac the first TOKEN == Keyword
            Scanner token = new Scanner(received);

            String cmd = token.next();
            //System.out.println("Command: "+ cmd);

            // Create a a switch based on the command tokens SENT FROM CLIENT
            switch (cmd){
                case "JOIN":
                    username =  token.next();
                    //writeMsg(validateIncoming(received));
                    writeMsg(received);
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
                    //flag = false;
                    close();
                    break;
                case "ALIVE":
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

            //if(flag == false)
            //break;

            // Repeat above until QUIT sent by clientSocket
        }while (!received.substring(0,4).equals("QUIT"));

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

