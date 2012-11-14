package me.stutiguias.webportal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import me.stutiguias.webportal.init.WebAuction;
import me.stutiguias.webportal.plugins.ProfileMcMMO;
import me.stutiguias.webportal.settings.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class MySQLDataQueries implements IDataQueries {

	private WebAuction plugin;
        private Integer found;
        private WALConnectionPool pool;
        
	public MySQLDataQueries(WebAuction plugin, String dbHost, String dbPort, String dbUser, String dbPass, String dbName) {
		this.plugin = plugin;
                try {
                        WebAuction.log.log(Level.WARNING, "{0} Starting pool....", plugin.logPrefix);
                        pool = new WALConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://"+ dbHost +":"+ dbPort +"/"+ dbName, dbUser, dbPass);
                }catch(Exception e) {
                        WebAuction.log.log(Level.WARNING, "{0} Exception getting mySQL WALConnection", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
                }
	}

	public WALConnection getConnection() {
		try {
			return pool.getConnection();
		} catch (Exception e) {
			WebAuction.log.log(Level.WARNING, "{0} Exception getting mySQL WALConnection", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		}
		return null;
	}

        @Override
        public Integer getFound() {
            return found;
        }
        
	public void closeResources(WALConnection conn, Statement st, ResultSet rs) {
		if (null != rs) {
			try {
				rs.close();
			} catch (SQLException e) {
			}
		}
		if (null != st) {
			try {
				st.close();
			} catch (SQLException e) {
			}
		}
		if (null != conn) {
                            conn.close();
		}
	}

	public boolean tableExists(String tableName) {
		boolean exists = false;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement("SHOW TABLES LIKE ?");
			st.setString(1, tableName);
			rs = st.executeQuery();
			while (rs.next()) {
				exists = true;
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to check if table exists: {1}", new Object[]{plugin.logPrefix, tableName});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return exists;
	}
        
        public int tableVersion() {
                int version = 0;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement("SELECT dbversion FROM WA_DbVersion");
			rs = st.executeQuery();
			while (rs.next()) {
				version = rs.getInt("dbversion");
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to check if table version ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return version;
        }

	public void executeRawSQL(String sql) {
		WALConnection conn = getConnection();
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			st.executeUpdate(sql);
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Exception executing raw SQL{1}", new Object[]{plugin.logPrefix, sql});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}

        @Override
	public void initTables() {
		if (!tableExists("WA_Players")) {
			WebAuction.log.log(Level.INFO, "{0} Creating table WA_Players", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_Players (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name VARCHAR(255), pass VARCHAR(255), money DOUBLE, itemsSold INT, itemsBought INT, earnt DOUBLE, spent DOUBLE, canBuy INT, canSell INT, isAdmin INT);");
		}
		if (!tableExists("WA_StorageCheck")) {
			WebAuction.log.log(Level.INFO, "{0} Creating table WA_StorageCheck", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_StorageCheck (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), time INT);");
		}
		if (!tableExists("WA_Auctions")) {
			WebAuction.log.log(Level.INFO, "{0} Creating table WA_Auctions", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_Auctions (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, player VARCHAR(255), quantity INT, price DOUBLE, created INT, allowBids BOOLEAN Default '0', currentBid DOUBLE, currentWinner VARCHAR(255), ench VARCHAR(45), tableid INT(1));");
		}
		if (!tableExists("WA_SellPrice")) {
			WebAuction.log.log(Level.INFO, "{0} Creating table WA_SellPrice", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_SellPrice (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, time INT, quantity INT, price DOUBLE, seller VARCHAR(255), buyer VARCHAR(255), ench VARCHAR(45));");
		}
		if (!tableExists("WA_MarketPrices")) {
			WebAuction.log.log(Level.INFO, "{0} Creating table WA_MarketPrices", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_MarketPrices (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, time INT, marketprice DOUBLE, ref INT);");
		}
		if (!tableExists("WA_SaleAlerts")) {
			WebAuction.log.log(Level.INFO, "{0} Creating table WA_SaleAlerts", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_SaleAlerts (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), seller VARCHAR(255), quantity INT, price DOUBLE, buyer VARCHAR(255), item VARCHAR(255), alerted BOOLEAN Default '0');");
		}
                if (!tableExists("WA_DbVersion")) {
                        WebAuction.log.log(Level.INFO, "{0} Creating table WA_DbVersion", plugin.logPrefix);
                        executeRawSQL("CREATE TABLE WA_DbVersion (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), dbversion INT);");
                        executeRawSQL("INSERT INTO WA_DbVersion (dbversion) VALUES (1)");
                        executeRawSQL("ALTER TABLE WA_Auctions ADD COLUMN `type` VARCHAR(45) NULL AFTER `tableid` , ADD COLUMN `itemname` VARCHAR(45) NULL  AFTER `type`, ADD COLUMN `searchtype` VARCHAR(45) NULL  AFTER `itemname` ;");
                }
                if (tableVersion() == 1) {
                        WebAuction.log.log(Level.INFO, "{0} Update DB version to 2", plugin.logPrefix);
                        executeRawSQL("ALTER TABLE WA_Players ADD COLUMN `lock` VARCHAR(1) Default 'N' AFTER `isAdmin` ");
                        executeRawSQL("UPDATE WA_DbVersion SET dbversion = 2 where id = 1");
                }
                
	}

        
        @Override
	public List<SaleAlert> getNewSaleAlertsForSeller(String player) {
		List<SaleAlert> saleAlerts = new ArrayList<SaleAlert>();

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT * FROM WA_SaleAlerts WHERE seller = ? AND alerted = ?");
			st.setString(1, player);
			st.setInt(2, 0);
			rs = st.executeQuery();
			while (rs.next()) {
				SaleAlert saleAlert = new SaleAlert();
				saleAlert.setId(rs.getInt("id"));
				saleAlert.setBuyer(rs.getString("buyer"));
				saleAlert.setItem(rs.getString("item"));
				saleAlert.setQuantity(rs.getInt("quantity"));
				saleAlert.setPriceEach(rs.getDouble("price"));
				saleAlerts.add(saleAlert);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get sale alerts for player {1}", new Object[]{plugin.logPrefix, player});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}

		return saleAlerts;
	}

        @Override
	public void markSaleAlertSeen(int id) {

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_SaleAlerts SET alerted = ? WHERE id = ?");
			st.setInt(1, 1);
			st.setInt(2, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to mark sale alert seen {1}", new Object[]{plugin.logPrefix, id});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}

        @Override
	public Auction getAuction(int id) {
		Auction auction = null;

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT name,quantity,damage,player,price,created,allowBids,currentBid,currentWinner,ench FROM WA_Auctions WHERE id = ?");
			st.setInt(1, id);
			rs = st.executeQuery();
			while (rs.next()) {
				auction = new Auction();
				auction.setId(id);
				ItemStack stack = new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage"));
                                stack = Chant(rs.getString("ench"),stack);
                                auction.setItemStack(stack);
				auction.setPlayerName(rs.getString("player"));
				auction.setPrice(rs.getDouble("price"));
				auction.setCreated(rs.getInt("created"));
				auction.setAllowBids(rs.getBoolean("allowBids"));
				auction.setCurrentBid(rs.getDouble("currentBid"));
				auction.setCurrentWinner(rs.getString("currentWinner"));
                                auction.setEnch(rs.getString("ench"));
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get auction {1}", new Object[]{plugin.logPrefix, id});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return auction;
	}

        
//        @Override
//	public int getTotalAuctionCount() {
//		int totalAuctionCount = 0;
//
//		WALConnection conn = getConnection();
//		PreparedStatement st = null;
//		ResultSet rs = null;
//
//		try {
//			st = conn.prepareStatement("SELECT COUNT(*) FROM WA_Auctions");
//			rs = st.executeQuery();
//			while (rs.next()) {
//				totalAuctionCount = rs.getInt(1);
//			}
//		} catch (SQLException e) {
//			WebAuction.log.warning(plugin.logPrefix + "Unable to get total auction count error : " + e.getMessage());
//		} finally {
//			closeResources(conn, st, rs);
//		}
//		return totalAuctionCount;
//	}

        
//        @Override
//	public Auction getAuctionForOffset(int offset) {
//		Auction auction = null;
//
//		WALConnection conn = getConnection();
//		PreparedStatement st = null;
//		ResultSet rs = null;
//
//		try {
//			st = conn.prepareStatement("SELECT * FROM WA_Auctions ORDER BY id DESC LIMIT ?, 1");
//			st.setInt(1, offset);
//			rs = st.executeQuery();
//			while (rs.next()) {
//				auction = new Auction();
//				auction.setId(offset);
//				auction.setItemStack(new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage")));
//				auction.setPlayerName(rs.getString("player"));
//				auction.setPrice(rs.getDouble("price"));
//				auction.setCreated(rs.getInt("created"));
//			}
//		} catch (SQLException e) {
//			WebAuction.log.warning(plugin.logPrefix + "Unable to get auction " + offset + " error : " + e.getMessage());
//		} finally {
//			closeResources(conn, st, rs);
//		}
//		return auction;
//	}

        @Override
        public List<Auction> getAuctions(int to,int from) {
		Auction auction;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
                List<Auction> la = new ArrayList<Auction>();
                
		try {
			st = conn.prepareStatement("SELECT SQL_CALC_FOUND_ROWS name,damage,player,quantity,price,id,created,ench FROM WA_Auctions where tableid = ? LIMIT ? , ?");
                        st.setInt(1, plugin.Auction);
                        st.setInt(2, to);
                        st.setInt(3, from);
			rs = st.executeQuery();
			while (rs.next()) {
				auction = new Auction();
				auction.setId(rs.getInt("id"));
                                ItemStack stack = new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage"));
                                stack = Chant(rs.getString("ench"), stack);
				auction.setItemStack(stack);
				auction.setPlayerName(rs.getString("player"));
				auction.setPrice(rs.getDouble("price"));
				auction.setCreated(rs.getInt("created"));
                                la.add(auction);
			}
                        st = conn.prepareStatement("SELECT FOUND_ROWS()");
			rs = st.executeQuery();
			while (rs.next()) {
		              found = rs.getInt(1);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get auction ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return la;
	}
        
        
        @Override
        public List<Auction> getSearchAuctions(int to,int from,String search,String searchtype) {
		Auction auction;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
                List<Auction> la = new ArrayList<Auction>();
                
		try {
			st = conn.prepareStatement("SELECT SQL_CALC_FOUND_ROWS name,damage,player,quantity,price,id,created,ench,type,itemname FROM WA_Auctions where tableid = ? and ( itemname like ? and searchtype = ? ) LIMIT ? , ?");
                        st.setInt(1, plugin.Auction);
                        st.setString(2, "%" + search + "%");
                        st.setString(3, searchtype);
                        st.setInt(4, to);
                        st.setInt(5, from);
			rs = st.executeQuery();
			while (rs.next()) {
				auction = new Auction();
				auction.setId(rs.getInt("id"));
                                ItemStack stack = new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage"));
                                stack = Chant(rs.getString("ench"), stack);
                                auction.setItemName(rs.getString("itemname"));
                                auction.setType(rs.getString("type"));
				auction.setItemStack(stack);
				auction.setPlayerName(rs.getString("player"));
				auction.setPrice(rs.getDouble("price"));
				auction.setCreated(rs.getInt("created"));
                                la.add(auction);
			}
                        st = conn.prepareStatement("SELECT FOUND_ROWS()");
			rs = st.executeQuery();
			while (rs.next()) {
		              found = rs.getInt(1);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get auction ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return la;
	}
       
        
        @Override
	public void updatePlayerPassword(String player, String newPass) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Players SET pass = ? WHERE name = ?");
			st.setString(1, newPass);
			st.setString(2, player);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update password for player: {1} error : {2}", new Object[]{plugin.logPrefix, player, e.getMessage()});
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
	public void UpdateItemAuctionQuantity(Integer numberleft, Integer id) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Auctions SET quantity = ? WHERE id = ?");
			st.setInt(1, numberleft);
			st.setInt(2, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update Auction: {1} error :{2}", new Object[]{plugin.logPrefix, id, e.getMessage()});
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
        public void DeleteAuction(Integer id) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("DELETE FROM WA_Auctions WHERE id = ?");
			st.setInt(1, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to delete Auction: {1}", new Object[]{plugin.logPrefix, id});
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
	public boolean hasMail(String player) {
		boolean exists = false;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT id FROM WA_Auctions WHERE player = ? and tableid = ?");
			st.setString(1, player);
                        st.setInt(2, plugin.Mail);
			rs = st.executeQuery();
			while (rs.next()) {
				exists = true;
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to check new mail for: {1}", new Object[]{plugin.logPrefix, player});
		} finally {
			closeResources(conn, st, rs);
		}
		return exists;
	}

        @Override
	public AuctionPlayer getPlayer(String player) {
		AuctionPlayer waPlayer = null;

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT * FROM WA_Players WHERE name = ?");
			st.setString(1, player);
			rs = st.executeQuery();
			while (rs.next()) {
				waPlayer = new AuctionPlayer();
				waPlayer.setId(rs.getInt("id"));
				waPlayer.setName(rs.getString("name"));
				waPlayer.setPass(rs.getString("pass"));
				waPlayer.setMoney(rs.getDouble("money"));
				waPlayer.setCanBuy(rs.getInt("canBuy"));
				waPlayer.setCanSell(rs.getInt("canSell"));
				waPlayer.setIsAdmin(rs.getInt("isAdmin"));
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0}Unable to get player {1}", new Object[]{plugin.logPrefix, player});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return waPlayer;
	}
        
        @Override
        public List<Auction> getAuctionsLimitbyPlayer(String player,int to,int from,int table) {
		Auction auction;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
                List<Auction> la = new ArrayList<Auction>();
                
		try {
			st = conn.prepareStatement("SELECT SQL_CALC_FOUND_ROWS name,damage,player,quantity,price,id,created,ench,type FROM WA_Auctions where player = ? and tableid = ? LIMIT ? , ?");
                        st.setString(1, player);
                        st.setInt(2, table);
                        st.setInt(3, to);
                        st.setInt(4, from);
			rs = st.executeQuery();
			while (rs.next()) {
				auction = new Auction();
				auction.setId(rs.getInt("id"));
                                ItemStack stack = new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage"));
                                stack = Chant(rs.getString("ench"), stack);
				auction.setItemStack(stack);
				auction.setPlayerName(rs.getString("player"));
				auction.setPrice(rs.getDouble("price"));
                                auction.setType(rs.getString("type"));
				auction.setCreated(rs.getInt("created"));
                                la.add(auction);
			}
                        st = conn.prepareStatement("SELECT FOUND_ROWS()");
			rs = st.executeQuery();
			while (rs.next()) {
		              found = rs.getInt(1);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get auction ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return la;
	}

        @Override
	public void updatePlayerPermissions(String player, int canBuy, int canSell, int isAdmin) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Players SET canBuy = ?, canSell = ?, isAdmin = ? WHERE name = ?");
			st.setInt(1, canBuy);
			st.setInt(2, canSell);
			st.setInt(3, isAdmin);
			st.setString(4, player);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update player permissions in DB error : {1}", new Object[]{plugin.logPrefix, e.getMessage()});
		} finally {
			closeResources(conn, st, rs);
		}
	}

        @Override
	public void createPlayer(String player, String pass, double money, int canBuy, int canSell, int isAdmin) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("INSERT INTO WA_Players (name, pass, money, canBuy, canSell, isAdmin) VALUES (?, ?, ?, ?, ?, ?)");
			st.setString(1, player);
			st.setString(2, pass);
			st.setDouble(3, money);
			st.setInt(4, canBuy);
			st.setInt(5, canSell);
			st.setInt(6, isAdmin);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update player permissions in DB", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
        public void LogSellPrice(Integer name,Short damage,Integer time,String buyer,String seller,Integer quantity,Double price,String ench) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("INSERT INTO WA_SellPrice (name, damage, time, buyer, seller, quantity, price, ench) VALUES (?,?,?,?,?,?,?,?)");
			st.setInt(1, name);
			st.setInt(2, damage);
			st.setInt(3, time);
			st.setString(4, buyer);
			st.setString(5, seller);
			st.setInt(6, quantity);
                        st.setDouble(7, price);
                        st.setString(8, ench);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update Sell Price", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
        public String getPassword(String player) {
                WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
                String pass = null;

		try {
                        if(plugin.authplugin.equalsIgnoreCase("WebPortal")) {
                            st = conn.prepareStatement("SELECT pass FROM WA_Players WHERE name = ?");
                        }else{
                            st = conn.prepareStatement("SELECT " + plugin.ColumnPassword + " FROM " + plugin.Table + " WHERE " + plugin.Username + " = ?");
                        }
			st.setString(1, player);
			rs = st.executeQuery();
                        while (rs.next()) {
                            if(plugin.authplugin.equalsIgnoreCase("WebPortal")) {
                                pass = rs.getString("pass");
                            }else{
                                pass = rs.getString(plugin.ColumnPassword);   
                            }         
                        }
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update player permissions in DB", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
                return pass;
        }

        @Override
	public void updatePlayerMoney(String player, double money) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Players SET money = ? WHERE name = ?");
			st.setDouble(1, money);
			st.setString(2, player);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update player money in DB", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}

        @Override
        public AuctionItem getItemById(int ID,int tableid) {
		AuctionItem auctionItem = null;

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			String sql = "SELECT id,name,damage,player,quantity,ench,itemname,price FROM WA_Auctions WHERE id = ? AND tableid = ?";
			st = conn.prepareStatement(sql);
                        st.setInt(1, ID);
                        st.setInt(2, tableid);
			rs = st.executeQuery();
			while (rs.next()) {
                                auctionItem = new AuctionItem();
				auctionItem.setId(rs.getInt("id"));
				auctionItem.setName(rs.getInt("name"));
                                auctionItem.setItemName(rs.getString("itemname"));
				auctionItem.setDamage(rs.getInt("damage"));
				auctionItem.setPlayerName(rs.getString("player"));
                                auctionItem.setPrice(rs.getString("price"));
				auctionItem.setQuantity(rs.getInt("quantity"));
                                auctionItem.setEnchantments(rs.getString("ench"));
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get items ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return auctionItem;
	}
        
        @Override
	public List<AuctionItem> getItemByName(String player, String itemName, boolean reverseOrder, int tableid) {
		List<AuctionItem> auctionItems = new ArrayList<AuctionItem>();

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			String sql = "SELECT id,name,damage,player,quantity,price,itemname,ench FROM WA_Auctions WHERE player = ? AND itemname = ? AND tableid = ?";
			if (reverseOrder) {
				sql += " ORDER BY id DESC";
			}
			st = conn.prepareStatement(sql);
			st.setString(1, player);
                        st.setString(2, itemName);
                        st.setInt(3, tableid);
			rs = st.executeQuery();
			while (rs.next()) {
				AuctionItem auctionItem = new AuctionItem();
				auctionItem.setId(rs.getInt("id"));
				auctionItem.setName(rs.getInt("name"));
				auctionItem.setDamage(rs.getInt("damage"));
				auctionItem.setPlayerName(rs.getString("player"));
				auctionItem.setQuantity(rs.getInt("quantity"));
                                auctionItem.setPrice(rs.getString("price"));
                                auctionItem.setItemName(rs.getString("itemname"));
                                auctionItem.setEnchantments(rs.getString("ench"));
				auctionItems.add(auctionItem);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get items ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return auctionItems;
	}
        
        @Override
	public List<AuctionItem> getItem(String player, int itemID, int damage, boolean reverseOrder, int tableid) {
		List<AuctionItem> auctionItems = new ArrayList<AuctionItem>();

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			String sql = "SELECT id,name,damage,player,quantity,ench FROM WA_Auctions WHERE player = ? AND name = ? AND damage = ? AND tableid = ?";
			if (reverseOrder) {
				sql += " ORDER BY id DESC";
			}
			st = conn.prepareStatement(sql);
			st.setString(1, player);
			st.setInt(2, itemID);
			st.setInt(3, damage);
                        st.setInt(4, tableid);
			rs = st.executeQuery();
			while (rs.next()) {
				AuctionItem auctionItem = new AuctionItem();
				auctionItem.setId(rs.getInt("id"));
				auctionItem.setName(rs.getInt("name"));
				auctionItem.setDamage(rs.getInt("damage"));
				auctionItem.setPlayerName(rs.getString("player"));
				auctionItem.setQuantity(rs.getInt("quantity"));
                                auctionItem.setEnchantments(rs.getString("ench"));
				auctionItems.add(auctionItem);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get items ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return auctionItems;
	}
        
        @Override
        public void setPriceAndTable(int id,Double price) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Auctions SET price = ? , tableid = " + plugin.Auction + " WHERE id = ?");
			st.setDouble(1, price);
                        st.setInt(2, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update item quantity in DB", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
	public void updateItemQuantity(int quantity, int id) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Auctions SET quantity = ? WHERE id = ?");
			st.setInt(1, quantity);
			st.setInt(2, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update item quantity in DB", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}

        @Override
        public void updateTable(int id,int tableid) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("UPDATE WA_Auctions SET tableid = ? WHERE id = ?");
			st.setInt(1, tableid);
                        st.setInt(2, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to update item quantity in DB", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
	public void createItem(int itemID, int itemDamage, String player, int quantity,Double price,String ench,int on,String type,String Itemname,String searchtype) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("INSERT INTO WA_Auctions (name, damage, player, quantity, price, ench, tableid, type, itemname, searchtype) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			st.setInt(1, itemID);
			st.setInt(2, itemDamage);
			st.setString(3, player);
			st.setInt(4, quantity);
                        st.setDouble(5, price);
                        st.setString(6, ench);
                        st.setInt(7, on);
                        st.setString(8, type);
                        st.setString(9, Itemname);
                        st.setString(10, searchtype);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to create item", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
	public void setAlert(String seller,Integer quantity,Double price,String buyer,String item) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("INSERT INTO WA_SaleAlerts (seller, quantity, price, buyer, item) VALUES (?,?,?,?,?)");
			st.setString(1, seller);
			st.setInt(2, quantity);
			st.setDouble(3, price);
                        st.setString(4, buyer);
                        st.setString(5, item);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to alert item", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
        public List<AuctionItem> getPlayerItems(String player) {
                List<AuctionItem> la = new ArrayList<AuctionItem>();
                AuctionItem ai;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT id,name,damage,player,quantity,ench FROM WA_Auctions WHERE player = ? and tableid = ?");
			st.setString(1, player);
                        st.setInt(2,plugin.Myitems);
			rs = st.executeQuery();
			while (rs.next()) {
				ai = new AuctionItem();
				ai.setId(rs.getInt("id"));
                                ai.setName(rs.getInt("name"));
                                ai.setDamage(rs.getInt("damage"));
                                ai.setQuantity(rs.getInt("quantity"));
                                ai.setPlayerName(rs.getString("player"));
                                ai.setEnchantments(rs.getString("ench"));
                                la.add(ai);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get mail for player {1}", new Object[]{plugin.logPrefix, player});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return la;
	}
        
        @Override
	public List<AuctionMail> getMail(String player) {
            
		List<AuctionMail> auctionMails = new ArrayList<AuctionMail>();

		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT id,name,quantity,damage,player,ench FROM WA_Auctions WHERE player = ? and tableid = ?");
			st.setString(1, player);
                        st.setInt(2, plugin.Mail);
			rs = st.executeQuery();
			while (rs.next()) {
				AuctionMail auctionMail = new AuctionMail();
				auctionMail.setId(rs.getInt("id"));
				ItemStack stack = new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage"));
                                stack = Chant(rs.getString("ench"),stack);
				auctionMail.setItemStack(stack);
				auctionMail.setPlayerName(rs.getString("player"));
				auctionMails.add(auctionMail);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get mail for player {1}", new Object[]{plugin.logPrefix, player});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return auctionMails;
	}

        @Override
	public void deleteMail(int id) {
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("DELETE FROM WA_Auctions WHERE id = ?");
			st.setInt(1, id);
			st.executeUpdate();
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to remove mail {1}", new Object[]{plugin.logPrefix, id});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
	}
        
        @Override
        public ProfileMcMMO getMcMMOProfileMySql(String tableprefix,String player){ 
                ProfileMcMMO pf = null;
            
            	WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT taming,mining,repair,unarmed,herbalism,excavation,archery,swords,axes,acrobatics,fishing FROM "+ tableprefix +"skills INNER JOIN "+ tableprefix +"users ON "+ tableprefix +"skills.user_id = "+ tableprefix +"users.id WHERE user = ?");
			st.setString(1, player);
			rs = st.executeQuery();
			while (rs.next()) {
				pf = new ProfileMcMMO();
				pf.setEXCAVATION(rs.getInt("excavation"));
                                pf.setTAMING(rs.getInt("taming"));
                                pf.setMINING(rs.getInt("mining"));
                                pf.setREPAIR(rs.getInt("repair"));
                                pf.setUNARMED(rs.getInt("unarmed"));
                                pf.setHERBALISM(rs.getInt("herbalism"));
                                pf.setARCHERY(rs.getInt("archery"));
                                pf.setSWORDS(rs.getInt("swords"));
                                pf.setAXES(rs.getInt("axes"));
                                pf.setACROBATICS(rs.getInt("acrobatics"));
                                pf.setFISHING(rs.getInt("fishing"));
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to get profile {1}", new Object[]{plugin.logPrefix, player});
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return pf;
        }
        
        @Override
        public ItemStack Chant(String ench,ItemStack stack) {
            if(!ench.equals(""))
            {
                String[] enchs = ench.split(":");
                for (String enchant:enchs) {
                    if(!enchant.equals("")) 
                    {
                        String[] number_level = enchant.split(",");
                        stack.addEnchantment(Enchantment.getById(Integer.parseInt(number_level[0])),Integer.parseInt(number_level[1]));
                    }
                }
            }
            return stack;
        }

    @Override
    public List<Transact> GetTransactOfPlayer(String player) {
                List<Transact> Transacts = new ArrayList<Transact>();
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT name,damage,time,quantity,price,seller,buyer,ench FROM WA_SellPrice where seller = ? or buyer = ?");
			st.setString(1, player);
                        st.setString(2, player);
			rs = st.executeQuery();
			while (rs.next()) {
                                Transact _Transact = new Transact();
                                _Transact.setBuyer(rs.getString("buyer"));
                                _Transact.setSeller(rs.getString("seller"));
                                ItemStack stack = new ItemStack(rs.getInt("name"), rs.getInt("quantity"), rs.getShort("damage"));
                                stack = Chant(rs.getString("ench"),stack);
                                _Transact.setItemStack(stack);
                                _Transact.setPrice(rs.getDouble("price"));
                                _Transact.setQuantity(rs.getInt("quantity"));
                                Transacts.add(_Transact);
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to transact ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return Transacts;
    }

    @Override
    public int GetMarketPriceofItem(int itemID, int itemDamage) {
                int MarketPrice = 0;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT SUM(price)/COUNT(id) as total FROM WA_SellPrice where name = ? and damage = ?");
			st.setInt(1, itemID);
                        st.setInt(2, itemDamage);
			rs = st.executeQuery();
			while (rs.next()) {
                                MarketPrice = rs.getInt("total");
			}
		} catch (SQLException e) {
			WebAuction.log.log(Level.WARNING, "{0} Unable to maket price ", plugin.logPrefix);
			WebAuction.log.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return MarketPrice;
    }

    @Override
    public String getLock(String player) {
        String Lock = null;
        WALConnection conn = getConnection();
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
                st = conn.prepareStatement("SELECT WA_Players.lock FROM WA_Players where name = ?");
                st.setString(1, player);
                rs = st.executeQuery();
                while (rs.next()) {
                        Lock = rs.getString("lock");
                }
        } catch (SQLException e) {
                WebAuction.log.log(Level.WARNING, "{0} Unable to maket price ", plugin.logPrefix);
                WebAuction.log.warning(e.getMessage());
        } finally {
                closeResources(conn, st, rs);
        }
        return Lock;
    }

    @Override
    public boolean setLock(String player, String lock) {
        WALConnection conn = getConnection();
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
                st = conn.prepareStatement("UPDATE WA_Players SET WA_Players.lock = ? where name = ? ");
                st.setString(1, lock);
                st.setString(2, player);
                st.executeUpdate();
        } catch (SQLException e) {
                WebAuction.log.log(Level.WARNING, "{0} Unable to create item", plugin.logPrefix);
                WebAuction.log.warning(e.getMessage());
        } finally {
                closeResources(conn, st, rs);
        }
        return true;
    }
        

}
