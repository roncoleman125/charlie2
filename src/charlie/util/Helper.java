/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package charlie.util;

/**
 *
 * @author Ron.Coleman
 */
public class Helper {
    public static int getPropertyOrElse(String key,int dfault) {
        String value = System.getProperty(key);
        if(value != null)
            return Integer.parseInt(value);
        else
            return dfault;
    }
    
    public static String getPropertyOrElse(String key,String dfault) {
        String value = System.getProperty(key);
        if(value != null)
            return value;
        else
            return dfault;
    } 
    
    public static Boolean getPropertyOrElse(String key,Boolean dfault) {
        String value = System.getProperty(key);
        if(value != null)
            return Boolean.parseBoolean(value);
        else
            return dfault;
    }
}
