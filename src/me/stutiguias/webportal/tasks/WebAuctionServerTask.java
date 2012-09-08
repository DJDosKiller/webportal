/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.stutiguias.webportal.tasks;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.stutiguias.webportal.init.WebAuction;
import me.stutiguias.webportal.settings.*;
import me.stutiguias.webportal.webserver.Html;
import me.stutiguias.webportal.webserver.Material;
import me.stutiguias.webportal.webserver.Response;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import me.stutiguias.request.FillAuction;
import me.stutiguias.request.FillMyAuctions;
import me.stutiguias.request.FillMyItems;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Stutiguias
 */
public class WebAuctionServerTask extends Thread {
    
    private WebAuction plugin;
    Socket WebServerSocket;
    String Lang;
    int Port;
    public AuthSystem AS;
    
    // will be delete soon
    Response Response;
    
    // Response type
    FillAuction _FillAuction;
    FillMyItems _FillMyItems;
    FillMyAuctions _FillMyAuctions;
    
    String levelname;
    String PluginDir;

    String ExternalUrl;

    public WebAuctionServerTask(WebAuction plugin, Socket s)
    {
        AS = new AuthSystem(plugin);
        PluginDir = "plugins/WebPortal/";
        this.plugin = plugin;
        Response = new Response(plugin,s);
        _FillAuction = new FillAuction(plugin, s);
        _FillMyItems = new FillMyItems(plugin, s);
        _FillMyAuctions = new FillMyAuctions(plugin, s);
        WebServerSocket = s;
    }

    public String readFileAsString(String filePath) throws IOException
    {
        StringBuilder fileData = new StringBuilder(0x10000);
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            char buf[] = new char[0x10000];
            int length;
            while((length = reader.read(buf)) > -1) 
                fileData.append(String.valueOf(buf, 0, length).replaceAll("_ExternalUrl_", ExternalUrl));
            reader.close();
        }
        catch(Exception e)
        {
           WebAuction.log.info((new StringBuilder()).append("ERROR in readFileAsString(): ").append(e.getMessage()).toString());
        }
        return fileData.toString();
    }
    

    public void readLine(String path, ArrayList save)
    {
        try
        {
            save.clear();
            File banlist = new File(path);
            if(banlist.exists())
            {
                BufferedReader in = new BufferedReader(new FileReader(banlist));
                String data = null;
                do
                {
                    if((data = in.readLine()) == null)
                        break;
                    if(data.length() > 0)
                        save.add(data);
                } while(true);
                in.close();
            }
        }
        catch(IOException e)
        {
            WebAuction.log.info((new StringBuilder()).append("ERROR in readLine(): ").append(e.getMessage()).toString());
        }
    }

    public static void copyFolder(File src, File dest)  throws IOException
    {
        FileChannel source;
        FileChannel destination;
        if(src.isDirectory())
        {
            if(!dest.exists())
                dest.mkdir();
            if(!src.exists())
            {
                WebAuction.log.info("Directory does not exist.");
                return;
            }
            String files[] = src.list();
            String arr$[] = files;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                String file = arr$[i$];
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyFolder(srcFile, destFile);
            }

        }
        if(!dest.exists())
            dest.createNewFile();
        source = null;
        destination = null;
        source = (new FileInputStream(src)).getChannel();
        destination = (new FileOutputStream(dest)).getChannel();
        destination.transferFrom(source, 0L, source.size());
        if(source != null)
            source.close();
        if(destination != null)
            destination.close();

        if(source != null)
            source.close();
        if(destination != null)
            destination.close();
    }

    public static boolean deleteDirectory(File path)
    {
        if(path.exists())
        {
            File files[] = path.listFiles();
            for(int i = 0; i < files.length; i++)
                if(files[i].isDirectory())
                    deleteDirectory(files[i]);
                else
                    files[i].delete();

        }
        return path.delete();
    }

    @Override
    public void run()
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
            try
            {
                String l,json, url = "", param = "", htmlDir = "./plugins/WebPortal/html";
                boolean flag = true;
                while((l = in.readLine()) != null && flag)
                {
                    if(l.startsWith("GET"))
                    {
                        flag = false;
                        String g = l.split(" ")[1];
                        Pattern regex = Pattern.compile("([^\\?]*)([^#]*)");
                        Matcher result = regex.matcher(g);
                        if(result.find())
                        {
                            url = result.group(1);
                            param = result.group(2);
                        }
                        if(l.contains("..") || l.contains("./"))
                        {
                            Response.readFileAsBinary(htmlDir+"/login.html","text/html");
                        }

                        String HostAddress = WebServerSocket.getInetAddress().getHostAddress();

                        if(url.startsWith("/web/login"))
                        {
                            String username = getParam("Username", param);
                            String pass = getParam("Password", param);
                            if(AS.Auth(username, pass))
                            {
                                AuthPlayer AuthPlayer = new AuthPlayer();
                                AuthPlayer.AuctionPlayer = plugin.dataQueries.getPlayer(username);
                                AuthPlayer.AuctionPlayer.setIp(HostAddress);
                                WebAuction.AuthPlayer.put(HostAddress,AuthPlayer);
                                json = "ok";
                            }else{
                                json = "no";
                            }
                            Response.print(json,"text/plain");
                        }else 

                        if(!WebAuction.AuthPlayer.containsKey(HostAddress))
                        {
                            if(url.startsWith("/css"))
                            {
                                Response.readFileAsBinary(htmlDir+url,"text/css");
                            }else if(url.startsWith("/image")) {
                                Response.readFileAsBinary(htmlDir+url,"image/jpg");
                            }else if(url.startsWith("/js")) {
                                Response.readFileAsBinary(htmlDir+url,"text/javascript");
                            }else {
                                Response.readFileAsBinary(htmlDir+"/login.html","text/html");
                            }
                        }else if(WebAuction.AuthPlayer.containsKey(HostAddress))
                        {
                            if(url.startsWith("/css"))
                            {
                                if(url.contains("image"))
                                {
                                    Response.readFileAsBinary(htmlDir+url,"image/png");
                                }else{
                                    Response.readFileAsBinary(htmlDir+url,"text/css");
                                }
                            }else if(url.startsWith("/image")) {
                                Response.readFileAsBinary(htmlDir+url,"image/png");
                            }else if(url.startsWith("/js")) {
                                Response.readFileAsBinary(htmlDir+url,"text/javascript");
                            }else if(url.startsWith("/server/username/info"))
                            {
                                AuthPlayer authPlayer = WebAuction.AuthPlayer.get(HostAddress);
                                String Name = authPlayer.AuctionPlayer.getName();
                                String Admin = (authPlayer.AuctionPlayer.getIsAdmin() == 1) ? ", Admin":",";
                                json = Name + Admin + ",";
                                json += "$ " + plugin.economy.getBalance(Name) + ",";
                                json += plugin.dataQueries.getMail(Name).size();
                                Response.print(json,"text/plain");
                            }else if(url.startsWith("/logout")) {
                                WebAuction.AuthPlayer.remove(HostAddress);
                                Response.readFileAsBinary(htmlDir + "/login.html","text/html");
                            }else if(url.startsWith("/fill/auction")) {
                                _FillAuction.fillAuction(HostAddress,url,param);
                            }else if(url.startsWith("/buy/item")) {
                                Buy(HostAddress,url,param);
                            }else if(url.startsWith("/fill/myitens")) {
                                _FillMyItems.getMyItems(HostAddress, url, param);
                            }else if(url.startsWith("/web/postauction") && !getLockState(HostAddress)) {
                                CreateAuction(HostAddress, url, param);
                            }else if(url.startsWith("/web/mail") && !getLockState(HostAddress)) {
                                Mail(HostAddress, url, param);
                            }else if(url.startsWith("/fill/myauctions")) {
                                _FillMyAuctions.getMyAuctions(HostAddress, url, param);
                            }else if(url.startsWith("/cancel/auction")) {
                                Cancel(HostAddress, url, param);
                            }else if(url.startsWith("/box/1")) {
                                BOX1(HostAddress);
                            }else if(url.startsWith("/box/2")) {
                                BOX2(HostAddress);
                            }else if(url.equalsIgnoreCase("/")) {
                                Response.readFileAsBinary(htmlDir + "/login.html","text/html");
                            }else{
                                Response.readFileAsBinary(htmlDir + url,"text/html");
                            }
                        }else{
                                Response.readFileAsBinary(htmlDir+"/login.html","text/html");
                        }
                    }
                }
            }catch(IOException e) {
                WebAuction.log.log(Level.WARNING, plugin.logPrefix + "ERROR in IO ");
                e.printStackTrace();
            }
            catch(Exception e)
            {
                WebAuction.log.log(Level.WARNING, plugin.logPrefix + "ERROR in ServerParser ");
                e.printStackTrace();
            }
        }
        catch(IOException e)
        {
                WebAuction.log.log(Level.WARNING, plugin.logPrefix + "ERROR in IO ");
                e.printStackTrace();
        }finally {
            plugin.connections--;
        }
    }
    
    public void BOX1(String HostAddress) {
        StringBuilder sb = new StringBuilder();
        AuthPlayer authPlayer = WebAuction.AuthPlayer.get(HostAddress);
        String Name = authPlayer.AuctionPlayer.getName();
        if((Boolean)plugin.mcmmoconfig.get("UseMcMMO") && !(Boolean)plugin.mcmmoconfig.get("McMMOMYSql")) {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(Name);
            sb.append(plugin.mcmmo.getBox(player));
        }else if((Boolean)plugin.mcmmoconfig.get("UseMcMMO") && (Boolean)plugin.mcmmoconfig.get("McMMOMYSql")) {
            sb.append(plugin.mcmmo.getBox(Name));
        }else{
            sb.append("");
        }
        Response.print(sb.toString(), "text/plain");
    }
    
    public void BOX2(String HostAddress) {
        StringBuilder sb = new StringBuilder();
        AuthPlayer authPlayer = WebAuction.AuthPlayer.get(HostAddress);
        String Name = authPlayer.AuctionPlayer.getName();
        if(plugin.UseEssentialsBox) {
           sb.append(plugin.essentials.getBox(Name));
        }else{
            sb.append("");
        }
        Response.print(sb.toString(),"text/plain");
    }
    
    public Boolean getLockState(String HostAddress) {
        if(WebAuction.LockTransact.get(WebAuction.AuthPlayer.get(HostAddress).AuctionPlayer.getName()) != null) {
            return WebAuction.LockTransact.get(WebAuction.AuthPlayer.get(HostAddress).AuctionPlayer.getName());
        }else{
            return false;
        }
    }
    
    public String getParam(String param, String URL)
    {
            Pattern regex = Pattern.compile("[\\?&]"+param+"=([^&#]*)");
            Matcher result = regex.matcher(URL);
            if(result.find()){
                    try{
                            String resdec = URLDecoder.decode(result.group(1),"UTF-8");
                            return resdec;
                    }catch (UnsupportedEncodingException e){
                            WebAuction.log.info(plugin.logPrefix+"ERROR in getParam(): " + e.getMessage());
                            return "";
                    }
            }else
                    return "";
    }

    public void CreateAuction(String ip,String url,String param) {
        int qtd = Integer.parseInt(getParam("Quantity", param));
        Double price = Double.parseDouble(getParam("Price", param));
        int id = Integer.parseInt(getParam("ID", param));
        AuctionItem au = plugin.dataQueries.getItemsById(id,plugin.Myitems);
        if(au.getQuantity() == qtd) {
            plugin.dataQueries.updateforCreateAuction(id,price);
            Response.print("You have sucess create Auction","text/plain");
        }else{
            if(au.getQuantity() > qtd)
            {
              plugin.dataQueries.UpdateItemAuctionQuantity(au.getQuantity() - qtd, id);
              Short dmg = Short.valueOf(String.valueOf(au.getDamage()));
              ItemStack stack = new ItemStack(au.getName(),au.getQuantity(),dmg);  
              String type =  stack.getType().toString();
              String ItemName = Material.getItemName(au.getName(),dmg);
              plugin.dataQueries.createItem(au.getName(),au.getDamage(),au.getPlayerName(),qtd,price,au.getEnchantments(),plugin.Auction,type,ItemName);
              Response.print("You have successfully created an Auction","text/plain");
            }else{
              Response.print("You not permit to sell more then you have","text/plain");
            }
        }
    }
    
    public void Mail(String ip,String url,String param) {
        int id = Integer.parseInt(getParam("ID", param));
        plugin.dataQueries.updateTable(id, plugin.Mail);
        Response.print("Mailt send","text/plain");
    }
    
    public void Cancel(String ip,String url,String param) {
        int id = Integer.parseInt(getParam("ID", param));
        plugin.dataQueries.updateTable(id, plugin.Myitems);
        Response.print("Cancel Done.","text/plain");
    }
    
    public void Buy(String ip,String url,String param) {
       try { 
           int qtd = Integer.parseInt(getParam("Quantity", param));
           int id = Integer.parseInt(getParam("ID", param));
           AuctionPlayer ap = WebAuction.AuthPlayer.get(ip).AuctionPlayer;
           Auction au = plugin.dataQueries.getAuction(id);
           
           boolean found = false;
           int StackId = 0;
           int Stackqtd = 0;
           String item_name = Material.getItemName(au.getItemStack().getTypeId(),(short)(au.getItemStack().getDurability()));
           if(qtd <= 0)
           {
              Response.print("Quantity greater then 0","text/plain");
           } else if(qtd > au.getItemStack().getAmount())
           {
              Response.print("You are attempting to purchase more than the maximum available","text/plain");
           } else if(!plugin.economy.has(ap.getName(),au.getPrice() * qtd))
           {
              Response.print("You do not have enough money.","text/plain");
           } else if(ap.getName().equals(au.getPlayerName())) {
              Response.print("You cannnot buy your own items.","text/plain");
           } else {
              plugin.economy.withdrawPlayer(ap.getName(), au.getPrice() * qtd);
              plugin.economy.depositPlayer(au.getPlayerName(), au.getPrice() * qtd);
              plugin.dataQueries.setAlert(au.getPlayerName(), au.getItemStack().getAmount(), au.getPrice(), ap.getName(), item_name);
              List<AuctionItem> items = plugin.dataQueries.getPlayerItems(ap.getName());
               for (AuctionItem item:items) {
                   
                   String playeritemname =  Material.getItemName(item.getName(),(short)(item.getDamage()));
                   if(item_name.equals(playeritemname) && item.getDamage() == au.getItemStack().getDurability())
                   {
                      if(au.getEnch().equals(item.getEnchantments()))
                      {
                          found = true;
                          StackId = item.getId();
                          Stackqtd = item.getQuantity();
                      }
                   }
               }
               if(found) {
                   plugin.dataQueries.updateItemQuantity(Stackqtd + qtd, StackId);
               }else{
                   String Type = au.getItemStack().getType().toString();
                   String ItemName = Material.getItemName(au.getItemStack().getTypeId(), au.getItemStack().getDurability());
                   plugin.dataQueries.createItem(au.getItemStack().getTypeId(), au.getItemStack().getDurability() , ap.getName(), qtd, 0.0, au.getEnch(), plugin.Myitems,Type,ItemName);
               }
               
               if(au.getItemStack().getAmount() > 0) {
                   if((au.getItemStack().getAmount() - qtd) > 0)
                   {
                      plugin.dataQueries.UpdateItemAuctionQuantity(au.getItemStack().getAmount() - qtd, au.getId());
                   }else{
                      plugin.dataQueries.DeleteAuction(au.getId());
                   }
               }
   
               int time = (int) ((System.currentTimeMillis() / 1000));
               plugin.dataQueries.LogSellPrice(au.getItemStack().getTypeId(),au.getItemStack().getDurability(),time, ap.getName(), au.getPlayerName(), qtd, au.getPrice(), au.getEnch());
               String res = "You purchased "+ qtd +" " + item_name + " from "+ au.getPlayerName() +" for " + au.getPrice();
               Response.print(res,"text/plain");
           }
       }catch(Exception ex){
           WebAuction.log.warning(ex.getMessage());
       }
        
    }

}
