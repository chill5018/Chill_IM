package chill_im;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by chill on 9/17/16.
 */
public class IM_Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // to display time
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    
    public IM_Server(int port) {
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        al = new ArrayList<ClientThread>();
    }

    public static void main(String[] args) {
        // connectToServer server on port 1500 unless a PortNumber is specified
        int portNumber = 1234;

        // create a server object and connectToServer it
        IM_Server server = new IM_Server(portNumber);
        server.startServer();
    }

    public void startServer() {
        keepGoing = true;
		/* create socket server and wait for connection requests */
        try
        {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while(keepGoing)
            {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();  	// accept connection
                // if I was asked to stop
                if(!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket);  // make a thread of it
                al.add(t);									// save it in the ArrayList
                t.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for(int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                        // not much I can do
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        // something went bad
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    // To Display something to the console only
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);

    }

    // To Share with all connected users
    private synchronized void broadcast(String message) {
        // add HH:mm:ss and \n to the message
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI

        System.out.print(messageLf);


        // loop in reverse order in case we have to remove a Client
        for(int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for(int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // found it
            if(ct.id == id) {
                al.remove(i);
                return;
            }
        }
    }


    /** One instance of this thread will run for each client */
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // the only type of message a will receive
        ChatMessage cm;
        // the date I connect
        String date;

        // Constructor
        ClientThread(Socket socket) {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
			/* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                display(username + " just connected.");
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        // what needs to run forever?
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                // the messaage part of the ChatMessage
                String message = cm.getMessage();

                // Switch on the type of message receive
                switch(cm.getType()) {

                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // scan al the users connected
                        for(int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);
            close();
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {}
            try {
                if(sInput != null) sInput.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }


        // Write a String to the Client output stream
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }




}




    /*
    public static void main(String[] args) {
        clientOutputStreams = new ArrayList();
        users = new ArrayList<>();

        // 1: Start the Server
        IMServer server = new IMServer();
        server.run();
    }

    // TODO: Move this to a separate File
    // 2: Client Handler START
    public static class ClientHandler implements Runnable {
        Socket clientSocket;      // the clients socket
        PrintWriter clientWriter; // the clients incoming message
        BufferedReader reader;    // to Read incoming messages


        public ClientHandler(Socket clientSocket, PrintWriter clientWriter) {
            this.clientSocket = clientSocket;
            this.clientWriter = clientWriter;

            try {
                // 2.1: Get the incoming message sent by the client
                InputStreamReader isReader = new InputStreamReader(clientSocket.getInputStream());

                // 2.2: Read the incoming message sent by the client
                reader = new BufferedReader(isReader);
            } catch (IOException io){
                System.out.println("Unexpected Error ");
            }
        }

        @Override
        public void run() {
            String message;
            try {
                do {

                    // 2.1: Get the incoming message sent by the client
                    InputStreamReader isReader = new InputStreamReader(clientSocket.getInputStream());

                    // 2.2: Read the incoming message sent by the client
                    reader = new BufferedReader(isReader);

                    // 2.3: Assign the message stored on the Buffered reader to a string
                    message = reader.readLine();

                    // 2.4: Create a token Scanner to read the first TOKEN == Keyword
                    Scanner token = new Scanner(message);
                    String cmd = token.next();
                    System.out.println("CMD: " + cmd);

                    // 2.5: Protocol Switch --> Determines how to handle the incoming message
                    // based on the command token that was sent
                    switch (cmd) {
                        case "JOIN":
                            if (addUser(message)) {
                                tellUser("J_OK", clientWriter);
                                tellEveryone("LIST " + LIST());
                            } else {
                                tellUser("J_ERR", clientWriter);
                            }
                            break;
                        case "DATA":
                            tellEveryone(message.substring(4, message.length()));
                            break;
                        case "ALVE":
                            break;
                        case "QUIT":
                            // Remove User
                            break;
                        default:
                            System.out.println("Input was not recognized by the server...");
                            break;
                    }
                } while (true);

            } catch (IOException io){
                io.printStackTrace();
            }

        }
    } // End --> ClientHandler



    // Server Thread
    public static class IMServer implements Runnable {
        @Override
        public  void run() {

            clientOutputStreams = new ArrayList();


            try {
                // 1.1: "Create the server"
                ServerSocket serverSocket = new ServerSocket(1234);

                do {
                    // 1.2: Accept incoming client connections
                    Socket clientSocket = serverSocket.accept();

                    // 1.3: Get the clients outputWriter (meaning messages client sends to server)
                    PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream());

                    // 1.4: Add client Writer to the list of client output streams
                    clientOutputStreams.add(clientWriter);

                    // 2: Create new Client Handler Thread to listen to Clients
                    Thread listener = new Thread(new ClientHandler(clientSocket, clientWriter));
                    listener.connectToServer();

                    System.out.println("Contact Made... but who is there?");

                } while (true);


            } catch (IOException io) {
                System.out.println("Error Making Connection...");
            }

        }
    }


        // Create all Protocol Methods
        // Verify User is Unique
        public static boolean addUser(String data) {
            String username ="";
            String hostIP ="";
            String port ="";
            String cmd = "";

            Scanner token = new Scanner(data);

            while (token.hasNext()) {
                cmd = token.next();
                username = token.next();
                String bufferToken = token.next();
                hostIP = token.next();
                bufferToken += token.next();
                port = token.next();
            }

            // User Details for Logging Purposes
            // TODO: Remove this in Production
            System.out.println("User: "+ username);
            System.out.println("Host: "+ hostIP);
            System.out.println("Port: "+ port +"\n");


            // Check to see if the name already exists
            if (!users.isEmpty()) {
                for (String s : users) {
                    if (s.toLowerCase().equals(username.toLowerCase())) {
                        // User is not unique --> Try again
                        return false;
                    }
                }
            }

            // User is unique --> add them to the list
            users.add(username);
            return true;
        }

        // User Remove

        // List
        public static String LIST() {
            String result = "Currently in the chat room: ";
            for (String s: users) {
                result +=  s + ", ";
            }
            return result;
        }

        // Tell User
        public static void tellUser(String message, PrintWriter clientWriter) {
            try {
                clientWriter.println(message);
                clientWriter.flush();

            }
                catch (Exception ex)
            {
                System.out.println("Error telling user. \n");
            }
        }

        // Tell Everyone
        public static void tellEveryone(String message) {

            for (Object clientOutputStream : clientOutputStreams) {
                try {
                    PrintWriter writer = (PrintWriter) clientOutputStream;
                    writer.println(message);
                    writer.flush();

                } catch (Exception ex) {
                    System.out.println("Error telling everyone. \n");
                }
            }
        }
            // ALVE
}

    */