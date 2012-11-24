/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.stutiguias.webportal.request;

import java.net.Socket;
import me.stutiguias.webportal.init.WebPortal;
import me.stutiguias.webportal.settings.AuctionPlayer;
import me.stutiguias.webportal.settings.AuthPlayer;
import me.stutiguias.webportal.settings.AuthSystem;
import me.stutiguias.webportal.webserver.Response;

/**
 *
 * @author Daniel
 */
public class Login extends Response {
    
    public WebPortal plugin;
    public AuthSystem AS;
    public Socket _Socket;
    
    public Login(WebPortal plugin,Socket socket){
        super(plugin, socket);
        this.plugin = plugin;
        AS = new AuthSystem(plugin);
        this._Socket = socket;
    }
    
    public void TryToLogin(String param) {
        String username = getParam("Username", param);
        String pass = getParam("Password", param);
        String json;
        if(AS.Auth(username, pass))
        {
            AuthPlayer _AuthPlayer = new AuthPlayer();
            AuctionPlayer _AuctionPlayer = plugin.dataQueries.getPlayer(username);
            if(_AuctionPlayer == null) {
                plugin.dataQueries.createPlayer(username,pass,0.0, 1, 1, 0);
                _AuctionPlayer = plugin.dataQueries.getPlayer(username);
            }
            _AuthPlayer.AuctionPlayer = _AuctionPlayer;
            _AuthPlayer.AuctionPlayer.setIp(_Socket.getInetAddress().getHostAddress());
            WebPortal.AuthPlayers.put(_Socket.getInetAddress().getHostAddress(),_AuthPlayer);
            json = "ok";
        }else{
            json = "no";
        }
        print(json,"text/plain");
    }
}
