/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package charlie.actor;

import charlie.message.Message;
import charlie.server.GameServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Ron.Coleman
 */
abstract public class LastActor implements Runnable {
    protected Logger LOG = Logger.getLogger(LastActor.class);
    protected Listener listener;
    protected ServerSocket serverSocket;
    private final String myHost;
    private String remoteHost;
    
    public LastActor(String myHost, String remoteHost) {
        this.myHost = myHost;
        this.remoteHost = remoteHost;
    }
    
    public LastActor(String myHost) {
        this(myHost,"");
    }
    
    public final void start() {
        new Thread(this).start();
    }
    
    @Override
    public void run() {
        receive();
    }
    
    public final void setListener(Listener listener) {
        this.listener = listener;
    }
    
    public final void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }
    
    public InetAddress getMyAddress() {
        try {
            String[] params = myHost.split(":");
            
            return InetAddress.getByName(params[0]);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public void receive() {
        int portno = Integer.parseInt(myHost.split(":")[1]);
        try {

            serverSocket = new ServerSocket(portno);
            
            while(true) {
                LOG.info(this.getClass().getSimpleName()+" waiting for connection on port "+portno);
                Socket clientSocket = serverSocket.accept();
                LOG.info(this.getClass().getSimpleName()+" accepted connection on port "+portno);
                
                InputStream is = clientSocket.getInputStream();
                
                ObjectInputStream ois = new ObjectInputStream(is);
                
                Message msg = (Message) ois.readObject();
                LOG.info("got message "+msg.getClass().getSimpleName());
                
                if(listener != null) {
                    LOG.info(this.getClass().getSimpleName()+" invoking listener for "+msg.getClass().getSimpleName());
                    listener.received(msg);
                }
                else
                    LOG.error(this.getClass().getSimpleName()+" dropped "+msg.getClass().getSimpleName());
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        } 
    }
    
    public void send(Message msg) {
        try {
            LOG.info(this.getClass().getSimpleName()+" sending "+msg.getClass().getSimpleName()+" to "+remoteHost);
            String[] params = remoteHost.split(":");

            String addr = params[0];
            int outPort = Integer.parseInt(params[1]);
            
            Socket socket = new Socket(addr, outPort);

            OutputStream os = socket.getOutputStream();

            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(msg);

            oos.flush();

            socket.close();
            
            LOG.info(this.getClass().getSimpleName()+" sent successfully "+msg.getClass().getSimpleName()+" to "+remoteHost);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
