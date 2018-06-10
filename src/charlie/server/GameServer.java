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
import com.googlecode.actorom.Address;
import com.googlecode.actorom.Topology;
import com.googlecode.actorom.remote.ServerTopology;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the game server.
 * @author Ron Coleman
 */
public class GameServer {
    static {
        // For properties see http://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html
        Properties _props = System.getProperties();
        _props.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        _props.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        _props.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        _props.setProperty("org.slf4j.simpleLogger.dateTimeFormat","HH:mm:ss");
    }
    protected static  Logger LOG = LoggerFactory.getLogger(GameServer.class);
    protected final static String HOUSE_ACTOR = "HOUSE";
    protected final static Random ran = new Random(0);
    protected final static Integer TOPOLOGY_PORT = 1234;
    protected final static Integer LOGIN_PORT = 9000;
    protected final static String HOST = "127.0.0.1";
    protected final Properties props = new Properties();
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
            // Start the actor server
            props.load(new FileInputStream("charlie.props"));
            
            String host = props.getProperty("charlie.server.host", HOST);
            int topologyPort = Integer.parseInt(props.getProperty("charlie.server.topology.port", TOPOLOGY_PORT+""));
            
            Topology serverTopology = new ServerTopology(host, topologyPort);
            LOG.info("server topology started...");

            // Spawn the house
            House house = new House(this,props);

            Address houseAddr = serverTopology.spawnActor(HOUSE_ACTOR, house);

            // Enter the login-loop
            int loginPort = Integer.parseInt(props.getProperty("charlie.server.login.port", LOGIN_PORT+""));
            
            ServerSocket serverSocket = new ServerSocket(loginPort);
            
            LOG.info("game server started...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                LOG.info("processing login from "+clientSocket.getInetAddress());
                
                process(clientSocket, houseAddr);

            }
        } catch (IOException | NumberFormatException ex) {
            LOG.error("exception thrown: "+ex);
        }
    }

    /**
     * Processes a login request
     * @param clientSocket
     * @param house House
     */
    private void process(final Socket clientSocket, final Address house) {
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream os = null;
                    ObjectOutputStream oos = null;
                    try (InputStream is = clientSocket.getInputStream();
                            ObjectInputStream ois = new ObjectInputStream(is)) {
                        Login login = (Login) ois.readObject();
                        LOG.info("got login...");

                        Ticket ticket = validate(house, login);
                        
                        if (ticket != null) {  
                            LOG.info("validated login");
                            os = clientSocket.getOutputStream();
                            oos = new ObjectOutputStream(os);
                            oos.writeObject(ticket);
                            oos.flush();
                            LOG.info("sent ticket to client");
                            
                            logins.add(ticket);
                        }

                        ois.close();
                        is.close();

                    }
                    if (oos != null) {
                        oos.close();
                    }

                    if (os != null) {
                        os.close();
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    LOG.error("exception thrown: "+ex);
                }
            }
        };

        new Thread(thread).start();
    }

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
    private Ticket validate(Address house, Login login) {
        if (login.getLogname() != null && login.getPassword() != null) {
            return new Ticket(house,ran.nextLong(),Constant.PLAYER_BANKROLL);
        }

        return null;
    }
}
