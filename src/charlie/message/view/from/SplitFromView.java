package charlie.message.view.from;

import charlie.card.Hid;

/**
 * Player has requested a split. This class will pass that request
 * from the ATable to the Dealer.
 * 
 * @author Dan Blossom
 */
public class SplitFromView extends Request {

    public SplitFromView(Hid hid){
        super(hid);
    }
}