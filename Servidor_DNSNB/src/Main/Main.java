/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import Entidades.servidorDNS;

/**
 *
 * @author ncp43
 */
public class Main {
private static servidorDNS serverDNS = new servidorDNS();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        serverDNS.iniciarServidorDns(53);
    }
    
}
