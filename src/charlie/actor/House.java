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
package charlie.actor;

import charlie.card.Hid;
import charlie.dealer.Dealer;
import charlie.plugin.IPlayer;
import charlie.message.view.from.Arrival;
import charlie.server.GameServer;
import charlie.server.Ticket;
import com.googlecode.actorom.Address;
import com.googlecode.actorom.Topology;
import com.googlecode.actorom.annotation.OnMessage;
import com.googlecode.actorom.annotation.TopologyInstance;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the house.
 * @author Ron Coleman
 */
public class House implements Serializable {
    private final Logger LOG = LoggerFactory.getLogger(House.class);
    @TopologyInstance private Topology topology;
    private final String PLAYER_ACTOR = "PLAYER-";
    protected List<RealPlayer> players = new ArrayList<>();
    private Integer nextPlayerId = 0;
    private final Properties props;
    private final GameServer server;
    protected HashMap<IPlayer,Ticket> accounts = new HashMap<>();

    /**
     * Constructor
     * @param server Game server
     * @param props Properties used by the server
     */
    public House(GameServer server,Properties props) {    
        this.server = server;
        this.props = props;
    }
    
    /**
     * Receives an arrival by a a myAddress.
     * At login the user gets a ticket from the server which the
     * house uses to validate. If the ticket is valid, the house
     * allocates a dealer and spawns a myAddress actor. The dealer
     * then waits for contact from a courier through the myAddress.
     * In other words, the whole design is largely passive in nature.
     * @param arrival 
     */
    @OnMessage(type = Arrival.class)
    public void onReceive(Arrival arrival) {
        Address courierAddress = arrival.getSource();
        LOG.info("arrival from "+courierAddress);
        
        Ticket ticket = arrival.getTicket();
        
        if(!valid(ticket)) {
            LOG.error("invalid ticket = "+ticket);
            return;
        }

        LOG.info("validated ticket = "+ticket);
        
        // Get a dealer for this player
        // Note: if we were allocating dealers from a pool, this is the place
        // to implement that logic. For now we'll just spawn dealers without
        // restriction.
        Dealer dealer = new Dealer(this);
        // Spawn an actor in server
        RealPlayer player = new RealPlayer(dealer, courierAddress);
        accounts.put(player,ticket);
        
        synchronized(this) {
            nextPlayerId++;
            
            players.add(player);
        }
        
        String id = PLAYER_ACTOR + nextPlayerId;
        Address playerAddress = topology.spawnActor(id, player); 
        
        player.setMyAddress(playerAddress);
        
        // Inform player we're ready
        player.ready();        
    }

    /**
     * Gets the house properties
     * @return Properties
     */
    public Properties getProps() {
        return props;
    }
    
    /**
     * Validates a ticket.
     * @param ticket Ticket
     * @return True if the ticket is valid, false otherwise.
     */
    protected boolean valid(Ticket ticket) {
        List<Ticket> logins = server.getLogins();
        
        for(int i=0; i < logins.size(); i++) {
            if(logins.get(i).equals(ticket))
                return true;
        }
        
        return false;
    }
    /**
     * Updates the bankroll.
     * @param hid Hand
     * @param gain P&L
     * @param player the player
     */
    public void updateBankroll(IPlayer player,Hid hid,Double gain) {      
        if(player == null || !accounts.containsKey(player))
            return;
        
        Ticket ticket = accounts.get(player);
        
        Double bankroll = ticket.getBankroll() + gain * hid.getAmt() + hid.getSideAmt();
        
        ticket.setBankroll(bankroll);
    }
    
    /**
     * Gets the bankroll for a myAddress.
     * @param player the player
     * @return Dollar amount of bankroll
     */
    public Double getBankroll(IPlayer player) {
        if(player == null || !accounts.containsKey(player))
            return 0.0;
        
        return accounts.get(player).getBankroll();
    }
}
