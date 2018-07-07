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
package charlie.actor.last;

import charlie.server.GameServer;
import charlie.server.Login;
import charlie.server.Ticket;
import charlie.util.Constant;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 *
 * @author Ron.Coleman
 */
public class LoginActor {
    protected final Logger LOG = Logger.getLogger(LoginActor.class);
    private final static Random ran = new Random(0);

    public LoginActor() {
    }

    public Ticket send(String logname, String password) {
        try {
            // Login to the server
            String host = System.getProperty("charlie.server.login");

            String[] params = host.split(":");
            String loginAddr = params[0];
            int loginPort = Integer.parseInt(params[1]);

            Socket client = new Socket(loginAddr, loginPort);

            OutputStream os = client.getOutputStream();

            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(new Login(logname, password));
            oos.flush();
            LOG.info("sent login request");

            InputStream is = client.getInputStream();

            ObjectInputStream ois = new ObjectInputStream(is);

            Ticket ticket = (Ticket) ois.readObject();
            LOG.info("received ticket");

            return ticket;

        } catch (IOException | ClassNotFoundException e) {
            LOG.info("failed to connect to server: " + e);

            return null;
        }
    }

    public Ticket receive() {
        try {
            String loginHost = System.getProperties().getProperty("charlie.server.login");
            
            int loginPort = Integer.parseInt(loginHost.split(":")[1]);
            
            ServerSocket serverSocket = new ServerSocket(loginPort);
            
            Socket clientSocket = serverSocket.accept();
            
            InputStream is = clientSocket.getInputStream();
            
            ObjectInputStream ois = new ObjectInputStream(is);
            
            Login login = (Login) ois.readObject();
            LOG.info("got login...");
            
            Ticket ticket = validate(login);
            
            if (ticket != null) {
                LOG.info("validated login...");
                
                LOG.info("added ticket " + ticket + " to login databzse");
                
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
                    LOG.error("exception caught "+ex);
                }
                
                return ticket;
            }
        } catch (IOException | ClassNotFoundException ex) {
            LOG.error("exception caught "+ex);
        }
        
        return null;
    }
    
        /**
     * Validates a login
     * @param house House actor address
     * @param login Login credentials to authenticate
     * @return Ticket or null if login fails
     */
    private Ticket validate(Login login) {
        try {
            InetAddress here = InetAddress.getLocalHost();
            
            if (login.getLogname() != null && login.getPassword() != null) {
                return new Ticket(here, ran.nextLong(),Constant.PLAYER_BANKROLL);
            }
            
        } catch (UnknownHostException ex) {
            LOG.error("exception caught "+ex);
        }
        
        return null;
    }
}
