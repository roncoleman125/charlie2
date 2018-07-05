/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import charlie.server.Login;
import charlie.server.Ticket;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ron.Coleman
 */
public class Client {
    public static void main(String[] args) {
        try {
            InetAddress home = InetAddress.getLocalHost();
            
            String[] params2 = ":1234".split(":");
            
            System.getProperties().load(new FileInputStream("charlie.props"));
            
            String loginHost = System.getProperty("charlie.server.login");
            String[] params = loginHost.split(":");
            String loginAddr = params[0];
            int loginPort = Integer.parseInt(params[1]);
            
            Socket client = new Socket(loginAddr, loginPort);
            
            OutputStream os = client.getOutputStream();
            
            ObjectOutputStream oos = new ObjectOutputStream(os);
            
            oos.writeObject(new Login("abc","def"));
            
            //////////////////////////
            
            InputStream is = client.getInputStream();
            
            ObjectInputStream ois = new ObjectInputStream(is);
            
            Ticket t = (Ticket) ois.readObject();
            
            System.out.println(t);
            
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
