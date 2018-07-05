/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import charlie.actor.House;
import charlie.server.Login;
import charlie.server.Ticket;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

/**
 *
 * @author Ron.Coleman
 */
public class Server {

    public static Random ran = new Random(0);

    public static void main(String[] args) {
        try {

            System.getProperties().load(new FileInputStream("charlie.props"));
            String loginHost = System.getProperty("charlie.server.login");

            House house = new House(null);

            int loginPort = Integer.parseInt(loginHost.split(":")[1]);

            ServerSocket serverSocket = new ServerSocket(loginPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                InputStream is = clientSocket.getInputStream();

                ObjectInputStream ois = new ObjectInputStream(is);

                Login login = (Login) ois.readObject();
                System.out.println(login);

                OutputStream os = clientSocket.getOutputStream();

                ObjectOutputStream oos = new ObjectOutputStream(os);

                Ticket ticket = validate(InetAddress.getLocalHost(), login);

                oos.writeObject(ticket);

            }

        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Ticket validate(InetAddress house, Login login) {
        if (login.getLogname() != null && login.getPassword() != null) {
            return new Ticket(house, ran.nextLong(), Constant.PLAYER_BANKROLL);
        }

        return null;
    }
}
