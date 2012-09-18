/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.stutiguias.webportal.dao;

import java.util.List;
import me.stutiguias.webportal.plugins.ProfileMcMMO;
import me.stutiguias.webportal.settings.*;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Daniel
 */
public interface IDataQueries {

        void initTables(); // Init Tables
        Integer getFound(); // Found On Last Search
        
        // Alert
        void setAlert(String seller,Integer quantity,Double price,String buyer,String item);
        List<SaleAlert> getNewSaleAlertsForSeller(String player);
        void markSaleAlertSeen(int id);
        
        // Auction
        Auction getAuction(int id); // Get One Auction
        //int getTotalAuctionCount(); // Get Total Auction
        //Auction getAuctionForOffset(int offset); // Get Auction Start in
        List<Auction> getAuctions(int to,int from); // 
        List<Auction> getSearchAuctions(int to,int from,String search,String type);
        List<Auction> getAuctionsLimitbyPlayer(String player,int to,int from,int table);
        void UpdateItemAuctionQuantity(Integer numberleft, Integer id);
        void DeleteAuction(Integer id);
        void setPriceAndTable(int id,Double price);
        
        //Player
	void updatePlayerPassword(String player, String newPass); 
        void updatePlayerPermissions(String player, int canBuy, int canSell, int isAdmin);
        void createPlayer(String player, String pass, double money, int canBuy, int canSell, int isAdmin);
        String getPassword(String player);
        void updatePlayerMoney(String player, double money);
	AuctionPlayer getPlayer(String player);	
        List<AuctionItem> getPlayerItems(String player);
        
        // Player Mail
        boolean hasMail(String player);
        void deleteMail(int id);
        List<AuctionMail> getMail(String player);
        
	// Admin
        void LogSellPrice(Integer name,Short damage,Integer time,String buyer,String seller,Integer quantity,Double price,String ench);
        void GetTransactOfPlayer(String player);

        //Items
        AuctionItem getItemById(int ID,int tableid);
        List<AuctionItem> getItem(String player, int itemID, int damage, boolean reverseOrder, int tableid);
        List<AuctionItem> getItemByName(String player, String itemName, boolean reverseOrder, int tableid);
        void updateItemQuantity(int quantity, int id);
        void updateTable(int id,int tableid);
	//void CreateAuction(int quantity, int id);
	void createItem(int itemID, int itemDamage, String player, int quantity,Double price,String ench,int on,String type,String Itemname,String searchtype);
        int GetMarketPriceofItem(int itemID, int itemDamage);
        
        ItemStack Chant(String ench,ItemStack stack);
        
        //Plugins
        ProfileMcMMO getMcMMOProfileMySql(String tableprefix,String player);
}
