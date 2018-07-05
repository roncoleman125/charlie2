//package charlie.test;
//
//import charlie.actor.House;
//import charlie.actor.RealPlayer;
//import charlie.card.Card;
//import charlie.card.Card.Suit;
//import charlie.card.Hand;
//import charlie.dealer.Dealer;
//import charlie.message.Message;
//import charlie.message.view.to.Ready;
//import charlie.plugin.IPlayer;
//import charlie.server.GameServer;
//import java.util.Properties;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
///**
// *
// * @author blossom
// */
//public class SplitTest {
//
//    @Test
//    public void test(){
//        
//        Hand hand = new Hand();
//        
//        hand.getHid().setAmt(5.0);
////        
//        System.out.println(hand.getHid().getAmt());
////        
//        hand.hit(new Card(Card.ACE, Suit.CLUBS));
//        hand.hit(new Card(Card.ACE, Suit.SPADES));
////        
//        System.out.println(hand.getCard(0));
//        System.out.println(hand.getCard(1));
////        
////        System.out.println(hand.size());
////        
////        Hand split = hand.split();
////        
////        System.out.println(hand.getHid().getAmt());
////        System.out.println(split.getHid().getAmt());
////        
////        System.out.println(hand.size());
////        System.out.println(split.size());
////        
////        System.out.println(hand.getCard(0));
////        System.out.println(split.getCard(0));
////        
////        System.out.println(hand.getCard(1));
////        System.out.println(split.getCard(1));
//        
//        Properties props = System.getProperties();
//        props.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
//        props.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
//        props.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
//        props.setProperty("org.slf4j.simpleLogger.dateTimeFormat","HH:mm:ss");
//        
//        House house = new House(new GameServer(),props);
//        
//        Dealer dealer = new Dealer(house);
//        
//        Topology serverTopology = new ServerTopology("127.0.0.1", 1234);
//        
//        Address houseAddr = serverTopology.spawnActor("HOUSE", house);
//       
//        IPlayer player = new RealPlayer(dealer, houseAddr);
//        
//        //dealer.split(player, hand.getHid());
//        
//        System.out.println(hand.getCard(0));
//        System.out.println(hand.getCard(1));
//        
//        assert(true);
//        
//    }
//
//}
