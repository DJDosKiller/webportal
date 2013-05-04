/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.stutiguias.webportal.request;

import java.net.Socket;
import java.util.List;
import me.stutiguias.webportal.init.WebPortal;
import me.stutiguias.webportal.settings.Auction;
import me.stutiguias.webportal.settings.AuctionPlayer;
import me.stutiguias.webportal.settings.TradeSystem;
import me.stutiguias.webportal.webserver.Response;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Daniel
 */
public class FillOperations extends Response {
        
    private WebPortal plugin;
    TradeSystem tr;
    
    public FillOperations(WebPortal plugin,Socket s) {
        super(plugin, s);
        this.plugin = plugin;
    }

    public void CreateAuction(String ip,String url,String param) {
        int qtd;
        Double price;
        int id;
        try {
            qtd = Integer.parseInt(getParam("Quantity", param));
            price = Double.parseDouble(getParam("Price", param));
            id = Integer.parseInt(getParam("ID", param));
        }catch(NumberFormatException ex) {
            print("Invalid Number","text/plain");
            return;
        }
        Auction auction = plugin.dataQueries.getItemById(id,plugin.Myitems);
        if(auction.getQuantity() == qtd) {
            plugin.dataQueries.setPriceAndTable(id,price);
            print("You have sucess create Auction","text/plain");
        }else{
            if(auction.getQuantity() > qtd)
            {
              plugin.dataQueries.UpdateItemAuctionQuantity(auction.getQuantity() - qtd, id);
              Short dmg = Short.valueOf(String.valueOf(auction.getDamage()));
              ItemStack stack = new ItemStack(auction.getName(),auction.getQuantity(),dmg);  
              String type =  stack.getType().toString();
              String[] itemConfig = getItemNameAndImg(stack);
              String ItemName = itemConfig[0];
              String searchtype = itemConfig[2];
              plugin.dataQueries.createItem(auction.getName(),auction.getDamage(),auction.getPlayerName(),qtd,price,auction.getEnchantments(),plugin.Auction,type,ItemName,searchtype);
              print("You have successfully created an Auction","text/plain");
            }else{
              print("You not permit to sell more then you have","text/plain");
            }
        }
    }
    
    public void Mail(String ip,String url,String param) {
        int id = Integer.parseInt(getParam("ID", param));
        int quantity = Integer.parseInt(getParam("Quantity", param));
        Auction _Auction = plugin.dataQueries.getAuction(id);
        if(_Auction.getItemStack().getAmount() == quantity) {
            plugin.dataQueries.updateTable(id, plugin.Mail);
        }else if(_Auction.getItemStack().getAmount() < quantity) {
            print("Not enought items","text/plain");
            return;
        }else if(_Auction.getItemStack().getAmount() > quantity) {
            plugin.dataQueries.updateItemQuantity(_Auction.getItemStack().getAmount() - quantity, id);
            String[] ItemConfig = getItemNameAndImg(_Auction.getItemStack());
            String itemName = ItemConfig[0];
            String SearchType = ItemConfig[2];
            plugin.dataQueries.createItem(_Auction.getItemStack().getTypeId(),_Auction.getItemStack().getDurability(),_Auction.getPlayerName(),quantity, _Auction.getPrice(),_Auction.getEnchantments(),plugin.Mail,_Auction.getType(), itemName , SearchType );
        }
        print("Mailt send","text/plain");
    }
    
    public void Cancel(String ip,String url,String param) {
        int id = Integer.parseInt(getParam("ID", param));
        
        Auction auction = plugin.dataQueries.getAuction(id);
        
        String player = auction.getPlayerName();
        Integer cancelItemId = auction.getItemStack().getTypeId();
        Short cancelItemDamage = auction.getItemStack().getDurability();
        
        List<Auction> auctions = plugin.dataQueries.getItem(player,cancelItemId,cancelItemDamage, true, plugin.Myitems);
        
        if(!auctions.isEmpty()) {
            
            Integer newAmount = auction.getItemStack().getAmount() + auctions.get(0).getItemStack().getAmount();
            Integer itemId = auctions.get(0).getId();
            plugin.dataQueries.updateItemQuantity(newAmount,itemId);
            plugin.dataQueries.DeleteAuction(id);
            
            
        }else{
            plugin.dataQueries.updateTable(id, plugin.Myitems);
        }
        print("Cancel Done.","text/plain");
    }
    
    public void Buy(String ip,String url,String param) {
       try { 
           int qtd = Integer.parseInt(getParam("Quantity", param));
           int id = Integer.parseInt(getParam("ID", param));
           
           AuctionPlayer ap = WebPortal.AuthPlayers.get(ip).AuctionPlayer;
           Auction au = plugin.dataQueries.getAuction(id);
           String item_name = getItemNameAndImg(au.getItemStack())[0];
           if(qtd <= 0)
           {
              print("Quantity greater then 0","text/plain");
           } else if(qtd > au.getItemStack().getAmount())
           {
              print("You are attempting to purchase more than the maximum available","text/plain");
           } else if(!plugin.economy.has(ap.getName(),au.getPrice() * qtd))
           {
              print("You do not have enough money.","text/plain");
           } else if(ap.getName().equals(au.getPlayerName())) {
              print("You cannnot buy your own items.","text/plain");
           } else {
               tr = new TradeSystem(plugin);
               print(tr.Buy(ap.getName(),au, qtd, item_name, false),"text/plain");
           }
       }catch(Exception ex){
           WebPortal.logger.warning(ex.getMessage());
       }
        
    }
}
