/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package charlie.actor;

import charlie.message.Message;

/**
 *
 * @author Ron.Coleman
 */
public interface Listener {
    public void received(Message msg);
    
}
