package chill_im;


import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by chill on 9/17/16.
 */

public class IM_Client {
    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private static ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;
    
    // the server, the port and the username
    private String server, username;
    private int port;

    private IM_Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    public static void main(String[] args) {
        // default values
        String userName = setUsername();

        // TODO: Make this work
//        Socket cliSckt = configMenu();
//        String serverAddress = cliSckt.getLocalAddress().getHostAddress();
//        System.out.println(serverAddress);
//        int portNumber = cliSckt.getPort();
//        System.out.println(portNumber);
        // TODO:  END MAKE THIS WORK

        String serverAddress = "localhost";
        int portNumber = 1234;


        


        // create the Client object
        IM_Client client = new IM_Client(serverAddress, portNumber, userName);

        // test if we can start the connection to the Server
        if(!client.connectToServer())
            return;

        sendMessage(new ChatMessage(ChatMessage.JOIN, "JOIN "+userName+" , "+ serverAddress+" : "+
                portNumber));

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("QUIT")) {
                client.sendMessage(new ChatMessage(ChatMessage.QUIT, ""));
                // break to do the disconnect
                break;
            } else if(msg.equalsIgnoreCase("LIST")) {
                // message WhoIsIn
                client.sendMessage(new ChatMessage(ChatMessage.LIST, ""));
            } else {
                // default to ordinary message
                client.sendMessage(new ChatMessage(ChatMessage.DATA, msg));
            }
        }
        // done disconnect
        client.disconnect();
    }


    // TODO: Maybe use this for the heartbeat?
    private static void heartBeat(IM_Client client) {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // do stuff
                client.sendMessage(new ChatMessage(ChatMessage.DATA, "ALVE"));
            }
        }, 0, 60, TimeUnit.SECONDS);
    }


//    private static Socket configMenu(){
//        Scanner userInput = new Scanner(System.in);
//
//        Socket s = new Socket();
//
//        String hostIP = "localhost";
//        int port = 1234;
//        // Default Port
//
//        System.out.println(
//                "Enter 1: for Manual Config " +
//                        "\nEnter 2: for LocalHost Config\n");
//        switch (userInput.nextInt()) {
//            case 1:
//                System.out.println("Enter Server IP: ");
//                hostIP = userInput.nextLine();
//                hostIP += userInput.nextLine();
//
//                System.out.print("Enter Server PORT: ");
//                port = userInput.nextInt();
//
//                try {
//                    // Set the Global Host Variable
//                    s = new Socket(hostIP, port);
//                } catch (IOException e) {
//                    System.out.println("\nHost ID not found!");
//                    System.exit(1);
//                }
//                break;
//
//            default:
//                hostIP = "127.0.0.1";
//                port = 1234;
//                try {
//                    // Set the Global Host Variable
//                    s = new Socket(hostIP, port);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                break;
//        }
//
//        userInput.close();
//        return s;
//    }
    private static String setUsername() {
        Scanner userInput = new Scanner(System.in);

        System.out.print("Enter UserName: ");
        String username = userInput.next();

        if (username.length() > 12) {
            System.out.println("Max 12 Characters!");
            System.out.println("Please Try Again...");
            System.out.print("Enter UserName: ");
            username = userInput.next();
        }
        return username;
    }

    // Connect to the server
    private boolean connectToServer() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        }
        // if it failed not much I can so
        catch(Exception ec) {
            display("Error connecting to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

		/* Creating both Data Stream */
        try
        {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try
        {
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    // Show Client a local Message
    private static void display(String msg) {
        System.out.println(msg);
    }

    // Client --> Server Message
    private static void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    //  When something goes wrong
    //  Close the Input/Output streams and disconnect not much to do in the catch clause
    private void disconnect() {
        try {
            if(sInput != null) sInput.close();
        }
        catch(Exception ignored) {}
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception ignored) {}
        try{
            if(socket != null) socket.close();
        }
        catch(Exception ignored) {}

    }

     // a class that waits for the message from the server and append them to the JTextArea
     // if we have a GUI or simply System.out.println() it in console mode

    private class ListenFromServer extends Thread {

        public void run() {
            while(true) try {
                String msg = (String) sInput.readObject();
                // Print the message and add back the prompt

                if (msg.equals("J_ERR")) {
                    // validation failed
                    setUsername();
                } else if (msg.equals("J_OK")) {
                    // validation successful;
                }

                System.out.println(msg);
                System.out.print("> ");

            } catch (IOException e) {
                display("Server has close the connection: " + e);
                break;
            }
            // can't happen with a String object but need the catch anyhow
            catch (ClassNotFoundException ignored) {
            }
        }
    }

}

/*
    // Global Vars
    private static String userName, hostIP;
    private static int port = 1234;

    // Network Globals
    private static Socket clientSocket;

    // I/O Globals
    private static ObjectInputStream clientInput;
    private static ObjectOutputStream clientOutput;
    private static Scanner userInput;

    // Client Constructor
    IM_Client(String hostIP, int port, String userName) {
        this.hostIP = hostIP;
        this.port = port;
        this.userName = userName;
    }


    public static void main(String[] args) {
        userInput = new Scanner(System.in);
        // Set the Username
        String userName = setUserName();

        // Select Network Config
        // Gets the serverAddress and Port
        configMenu();

        // create the Client object
        System.out.printf("User: %s, Port:  %d,  Username: %s,", hostIP, port, userName);
        IM_Client client = new IM_Client(hostIP, port, userName);

        // test if we can connectToServer the connection to the Server
        if(!client.connectToServer())
            return;


        // wait for messages from user
        userInput = new Scanner(System.in);

        // loop forever for message from the user
        // TODO: Refactor Protocols
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = userInput.nextLine();

            String cmd = userInput.next();
            System.out.println("Incoming Client CMD: " + cmd);

            switch (cmd) {
                case "J_OK":
                    System.out.println("Access Granted");
                    // Send Heart Beat Here
                    break;
                case "J_ERR":
                    System.out.println("Access Denied");
                    setUserName();
                    configMenu();
                    client.sendMessage(new ChatMessage(ChatMessage.JOIN, msg));
                    break;
                case "LIST":
                    display(msg);

                    break;
                default:
                    client.sendMessage(new ChatMessage(ChatMessage.DATA, msg));
                    break;

            }
        // done disconnect
        client.disconnect();
        }
    }

    // Connect to Server
    private boolean connectToServer(){
        // Try to Connect to Server

        try {
            clientSocket = new Socket(hostIP, port);
        } catch (IOException e) {
            display("Error Connecting to Server");
            return false;
        }


        String msg = "Connection accepted " + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
        display(msg);

        // Create both Data Streams
        try {

            clientInput  = new ObjectInputStream(clientSocket.getInputStream());
            clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());

        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }


        // creates the Thread to listen from the server
        new ListenForServer().connectToServer();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try
        {
            display(userName);
            clientOutput.writeObject(userName);
            // JOIN Protocol client --> server
           display("JOIN "+userName+" , "+ clientSocket.getInetAddress().getHostAddress()+" : "+
                    clientSocket.getPort());
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    private static void configMenu(){
        // Default Port

        System.out.println(
                "Enter 1: for Manual Config " +
                        "\nEnter 2: for LocalHost Config\n");
        switch (userInput.nextInt()) {
            case 1:
                System.out.println("Enter Server IP: ");
                hostIP = userInput.nextLine();
                hostIP += userInput.nextLine();

                System.out.print("Enter Server PORT: ");
                port = userInput.nextInt();

                try {
                    // Set the Global Host Variable
                    clientSocket = new Socket(hostIP, port);
                } catch (IOException e) {
                    System.out.println("\nHost ID not found!");
                    System.exit(1);
                }
                break;

            default:
                hostIP = "127.0.0.1";
                try {
                    // Set the Global Host Variable
                    clientSocket = new Socket(hostIP, port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    private static String setUserName() {
        userInput = new Scanner(System.in);

        System.out.print("Enter UserName: ");
        userName = userInput.next();

        if (userName.length() > 12) {
            System.out.println("Max 12 Characters!");
            System.out.println("Please Try Again...");
            System.out.print("Enter UserName: ");
            userName = userInput.next();
        }

        return userName;
    }

    private static String DATA() {
        // Containers for messages, and responses
        String message, protocolString;

        do {
            System.out.print("Enter message('QUIT' to exit): ");
            message = userInput.nextLine();

            if (message.length() > 250) {
                System.out.println("Max 250 Characters!");
                return  "DATA " + userName + " : " + message.substring(0, 249);

            } else if (message.equals("QUIT")){
              return "QUIT " + userName + " : " + message + "";
            }else {
               return "DATA " + userName + " : " + message + "";
            }
        } while (!message.equals("QUIT"));
    }


    /*
     *  Show a message to the User without
     *  sending it to the server
     */
/*
    private static void display(String msg) {
        System.out.println(msg);
    }
    /*
     *  To send a message to the server
     */
/*
    static void sendMessage(ChatMessage msg) {
        try {
            clientOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    private void disconnect() {
        try {
            if(clientInput != null) clientInput.close();
        }
        catch(Exception e) {} // not much else I can do
        try {
            if(clientOutput != null) clientOutput.close();
        }
        catch(Exception e) {} // not much else I can do
        try{
            if(clientSocket != null) clientSocket.close();
        }
        catch(Exception e) {} // not much else I can do


    }


    // Thread --> Incoming Reader
    class ListenForServer extends Thread {

        // Run () --> Handles Protocol Commands
        @Override
        public void run() {
            while (true) {
                try {
                    String msg = (String) clientInput.readObject();
                    System.out.println("Listen for Server Thread Msg: "+msg);
                    System.out.print(">");
                } catch (IOException io){
                    display("Server has closed the connection: "+io);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }


            }


        }
    }
}

*/
