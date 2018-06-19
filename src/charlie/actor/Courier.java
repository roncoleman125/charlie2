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

import charlie.card.Card;
import charlie.card.HoleCard;
import charlie.message.view.to.Ready;
import charlie.plugin.IUi;
import charlie.card.Hid;
import charlie.dealer.Seat;
import charlie.message.view.from.Bet;
import charlie.message.view.from.DoubleDown;
import charlie.message.view.from.Hit;
import charlie.message.view.from.SplitFromView;
import charlie.message.view.from.Stay;
import charlie.message.view.to.Blackjack;
import charlie.message.view.to.Bust;
import charlie.message.view.to.Charlie;
import charlie.message.view.to.Deal;
import charlie.message.view.to.GameOver;
import charlie.message.view.to.Loose;
import charlie.message.view.to.Outcome;
import charlie.message.view.to.Play;
import charlie.message.view.to.Push;
import charlie.message.view.to.GameStart;
import charlie.message.view.to.Shuffle;
import charlie.message.view.to.SplitToView;
import charlie.message.view.to.Win;
import charlie.util.Constant;
import com.googlecode.actorom.Actor;
import com.googlecode.actorom.Address;
import com.googlecode.actorom.annotation.OnMessage;
import com.googlecode.actorom.remote.ClientTopology;
import java.util.concurrent.TimeUnit;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.apache.log4j.Logger;

/**
 * This class implements the courier actor between the client and server.
 * @author Ron Coleman
 */
public class Courier {  
    private final Logger LOG = Logger.getLogger(Courier.class);

    private final IUi ui;
    protected Actor player;
    protected ClientTopology topology;
    protected Address myAddress;
    protected HoleCard holeCard;
    
    /**
     * Constructor
     * @param ui User interface
     */
    public Courier(IUi ui) {
        this.ui = ui;
        
        ui.setCourier(this);
    }
    
    /**
     * Sends the stay request to dealer surrogate on server.
     * @param hid Hand id
     */
    public void stay(Hid hid) {
        player.send(new Stay(hid));
    }
    
    /**
     * Sends the hit request to dealer surrogate actor on server.
     * @param hid Hand id
     */
    public void hit(Hid hid) {
        player.send(new Hit(hid));
    }
    
    /**
     * Sends the double-down request to dealer surrogate actor on server.
     * @param hid Hand id
     */
    public void dubble(Hid hid) {
        player.send(new DoubleDown(hid));
    }    
    
    /**
     * Sends the bet request and creates a new hand id.
     * @param amt Main bet amount
     * @param sideAmt Side bet amount
     * @return Hand id
     */
    public Hid bet(Integer amt, Integer sideAmt) {
        Hid hid = new Hid(Seat.YOU,amt,sideAmt);
        
        player.send(new Bet(hid));
        
        return hid;
    }
    
    /**
     * Sends the split request to the dealer.
     * The HID is created in the game frame after player pressed "split"
     * @param hid The hand ID we are going to split (original hand)
     */
    public void split(Hid hid){
        player.send(new SplitFromView(hid));
    }
    
    /**
     * Receives a split notification from the dealer with the new HID
     * @param split
     */
    @OnMessage(type = SplitToView.class)
    public void onReceive(SplitToView split){
        LOG.info("received split outcome from dealer");
        ui.split(split.getNewHid(), split.getOrigHid());
    }
    
    /**
     * Receives a game outcome from dealer surrogate actor on server.
     * @param outcome Game outcome
     */
    @OnMessage(type = Outcome.class)
    public void onReceive(Outcome outcome) {
        LOG.info("received outcome = "+outcome);
        
        Hid hid = outcome.getHid();
        
        if(outcome instanceof Blackjack)
            ui.blackjack(hid);
        else if(outcome instanceof Charlie)
            ui.charlie(hid);
        else if(outcome instanceof Win)
            ui.win(hid);
        else if(outcome instanceof Push)
            ui.push(hid);
        else if(outcome instanceof Loose)
            ui.lose(hid);
        else if(outcome instanceof Bust)
            ui.bust(hid);
        else
            LOG.error("outcome unknown");
    }
    
    /**
     * Receives a connected message sent by the house actor.
     * @param msg Ready message
     */
    @OnMessage(type = Ready.class)
    public void onReceive(Ready msg) {
        Address addr = msg.getSource();
        LOG.info("received "+msg+" from "+addr);
        
        this.topology =
                new ClientTopology(addr.getHost(), addr.getPort(), 5, TimeUnit.SECONDS, 3, TimeUnit.SECONDS);
        
        this.player = topology.getActor(msg.getSource());
        
        if(!player.isActive())
            return;

        synchronized(ui) {
            ui.notify();
        }
    }
    
    /**
     * Receives game starting message from dealer surrogate actor on server.
     * @param starting Game start which contains hand ids and shoe size
     */
    @OnMessage(type = GameStart.class)
    public void onReceive(GameStart starting) { 
        LOG.info("receive starting shoe size = "+starting.shoeSize());
        
        for(Hid hid: starting.getHids())
            LOG.info("starting hand: "+hid);
        
        ui.starting(starting.getHids(),starting.shoeSize());
    }

    /**
     * Receives a card deal from the dealer surrogate actor on the server.
     * @param deal Deal containing card
     */
    @OnMessage(type = Deal.class)
    public void onReceive(Deal deal) {      
        Hid hid = deal.getHid();
        
        Card card = deal.getCard();
        
        if(card instanceof HoleCard)
            holeCard = (HoleCard)card;
        
        int[] values = deal.getHandValues();
        
        LOG.info("received card = "+card+" values = "+values[Constant.HAND_LITERAL_VALUE]+"/"+values[Constant.HAND_SOFT_VALUE]+" hid = "+hid);
        
        ui.deal(hid, card, values);
    }
    
    /**
     * Receives the play turn.
     * @param turn Turn
     */
    @OnMessage(type = Play.class)
    public void onReceive(Play turn) {
        LOG.info("got trun = "+turn.getHid());
        
        ui.turn(turn.getHid());

    }
    
    /**
     * Receives the game over signal from the dealer surrogate on the server.
     * @param ending Game over
     */
    @OnMessage(type = GameOver.class)
    public void onReceive(GameOver ending) {
        LOG.info("received ending shoe size = "+ending.getShoeSize());
        ui.ending(ending.getShoeSize());
    }
    
    /**
     * Receives the shuffling signal from the dealer surrogate on the server.
     * @param shuffle Shuffle
     */
    @OnMessage(type = Shuffle.class)
    public void onReceive(Shuffle shuffle) {
        LOG.info("received shuffle");
        ui.shuffling();
    }
    
    /**
     * Receives a string message typically for testing purposes.
     * @param s String
     */
    @OnMessage(type = String.class)
    public void onReceive(String s) {
        System.out.println(s);
    }
    
    /**
     * Sets my address.
     * @param mine My address
     */
    public void setMyAddress(Address mine) {
        this.myAddress = mine;
    }
}
