/*
 Copyright (c) 2014 Ron Coleman

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package charlie.server;

import charlie.actor.House;
import charlie.util.Constant;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import org.apache.log4j.Logger;


/**
 * This class implements the game server.
 * @author Ron Coleman
 */
public class GameServer {
    static {
        // For properties see http://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html
        System.getProperties().setProperty("LOGFILE","log-server.out");
    }
    protected static  Logger LOG = Logger.getLogger(GameServer.class);
    protected final static String HOUSE_ACTOR = "HOUSE";
    protected final static Random ran = new Random(0);
    protected final static Integer TOPOLOGY_PORT = 1234;
    protected final static String HOST = "127.0.0.1";
    protected final List<Ticket> logins = new ArrayList<>();
    
    /**
     * This method is the main entry point for the server.
     * @param args Command line arguments (currently not used)
     */
    public static void main(String[] args) {
        new GameServer().go();
    }
    
    /**
     * Starts the server processing loop
     */
    protected void go() {
        try {
            LOG.info("game server started...");
            
            // Start the actor server
            Properties props = System.getProperties();
            
            props.load(new FileInputStream("charlie.props"));

            // Spawn the house
            House house = new House(this);
            house.setListener(house);
            house.start();
            LOG.info("house started...");

            InetAddress houseAddr = house.getMyAddress();

            // Enter the login-loop
            
            String loginHost = props.getProperty("charlie.server.login");
            int loginPort = Integer.parseInt(loginHost.split(":")[1]);
            
            ServerSocket serverSocket = new ServerSocket(loginPort);
            
            while(true) {
                LOG.info(this.getClass().getSimpleName()+" waiting for login connection on port "+loginPort);
                Socket clientSocket = serverSocket.accept();
                
                LOG.info("processing login from "+clientSocket.getInetAddress());
                process2(clientSocket, houseAddr);
            }
        } catch (IOException | NumberFormatException ex) {
            LOG.error("exception thrown: "+ex);
        }
    }

    private void process2(final Socket clientSocket, final InetAddress house) {
        try {
            InputStream is = clientSocket.getInputStream();

            ObjectInputStream ois = new ObjectInputStream(is);

            Login login = (Login) ois.readObject();
            LOG.info("got login...");

            Ticket ticket = validate(house, login);

            if (ticket != null) {
                LOG.info("validated login...");
                
                logins.add(ticket);
                LOG.info("added ticket "+ticket+" to login databzse");                

                OutputStream os = clientSocket.getOutputStream();
                LOG.info("got output stream");

                ObjectOutputStream oos = new ObjectOutputStream(os);
                LOG.info("got object output stream");

                oos.writeObject(ticket);
                LOG.info("wrote ticket to object output stream");

                oos.flush();
                LOG.info("sent ticket to client");

                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

//                        ois.close();
//                        is.close();
//                    if (oos != null) {
//                        oos.close();
//                    }
//
//                    if (os != null) {
//                        os.close();
//                    }
        } catch (IOException | ClassNotFoundException ex) {
            LOG.error("exception thrown: " + ex);
        }
    }
    
    /**
     * Processes a login request
     * @param clientSocket
     * @param house House
     */
//    private void process(final Socket clientSocket, final InetAddress house) {
//        Runnable thread = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    InputStream is = clientSocket.getInputStream();
//                    
//                    ObjectInputStream ois = new ObjectInputStream(is);
//                    
//                    Login login = (Login) ois.readObject();
//                    LOG.info("got login...");
//
//                    Ticket ticket = validate(house, login);
//
//                    if (ticket != null) {
//                        LOG.info("validated login...");
//                        
//                        OutputStream os = clientSocket.getOutputStream();
//                        LOG.info("got output stream...");
//                        
//                        ObjectOutputStream oos = new ObjectOutputStream(os);
//                        LOG.info("got object output stream...");
//                        
//                        oos.writeObject(ticket);
//                        LOG.info("wrote ticket to object output stream...");
//                        
//                        oos.flush();
//                        LOG.info("flushed the object output stream...");
//                        
//                        LOG.info("sent ticket to client...");
//
//                        logins.add(ticket);
//                    }
//
////                        ois.close();
////                        is.close();
//
//
////                    if (oos != null) {
////                        oos.close();
////                    }
////
////                    if (os != null) {
////                        os.close();
////                    }
//                } catch (IOException | ClassNotFoundException ex) {
//                    LOG.error("exception thrown: "+ex);
//                }
//            }
//        };
//
//        new Thread(thread).start();
//    }

    /**
     * Gets the logins by ticket
     * @return Tickets
     */
    public List<Ticket> getLogins() {
        return logins;
    }
    
    /**
     * Validates a login
     * @param house House actor address
     * @param login Login credentials to authenticate
     * @return Ticket or null if login fails
     */
    private Ticket validate(InetAddress house, Login login) {
        if (login.getLogname() != null && login.getPassword() != null) {
            return new Ticket(house,ran.nextLong(),Constant.PLAYER_BANKROLL);
        }

        return null;
    }
}
