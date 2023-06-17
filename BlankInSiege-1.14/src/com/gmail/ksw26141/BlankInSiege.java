package com.gmail.ksw26141;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractArrow.PickupRule;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

@SuppressWarnings("deprecation")
public class BlankInSiege extends JavaPlugin implements Listener{
	String PluginTitle=ChatColor.RED+"["+ChatColor.WHITE+"BlankInSiege"+ChatColor.RED+"] "+ChatColor.GRAY;
	String GreenTitle=ChatColor.GRAY+"["+ChatColor.GREEN+"+"+ChatColor.GRAY+"] "+ChatColor.GRAY;
	String RedTitle=ChatColor.GRAY+"["+ChatColor.RED+"+"+ChatColor.GRAY+"] "+ChatColor.GRAY;
	HashMap<String,Boolean> blitCoolList=new HashMap<String,Boolean>();
	HashMap<String,Arrow> glowTrackingArrow=new HashMap<String, Arrow>();
	HashMap<String,Boolean> glowTrackingCool=new HashMap<String, Boolean>();
    BukkitScheduler scheduler = getServer().getScheduler();
    NamespacedKey nbtName=new NamespacedKey(this, "Siege");
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info(PluginTitle+"작동 시작");
	}
	
	@Override
	public void onDisable() {
		saveConfig();
		getLogger().info(PluginTitle+"작동 중지");
	}
	
	public void sendMessage(CommandSender sender,String Message) {
		sender.sendMessage(Message);
		getLogger().info(Message);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] player){
		if(cmd.getName().equalsIgnoreCase("blanksiege")) {//플러그인 재설정 명령어
			if(player.length==1) {
				Player user=getServer().getPlayer(sender.getName());
				ItemStack handItem=user.getInventory().getItemInMainHand();
				ItemMeta itemMeta=handItem.getItemMeta();
				if(itemMeta==null) {
					sender.sendMessage(RedTitle+"손에 아이템을 들어주세요.");
					return true;
				}
				String nbtData=player[0];
				sender.sendMessage(PluginTitle+"전용 NBT태그로 "+nbtData+" 등록완료");
				itemMeta.getCustomTagContainer().setCustomTag(nbtName, ItemTagType.STRING,nbtData);
				handItem.setItemMeta(itemMeta);
				return true;
			}
			return false;
		}
		else if(cmd.getName().equalsIgnoreCase("blitzcharge")) {
			String uuid="";
			Player user=null;
			int chargeCount=0;
			try {
				if(player.length==1) {
					uuid=getServer().getPlayer(sender.getName()).getUniqueId().toString();
					user=getServer().getPlayer(sender.getName());
					chargeCount=Integer.parseInt(player[0]);
				}
				else if(player.length==2) {
					uuid=getServer().getPlayer(player[0]).getUniqueId().toString();
					user=getServer().getPlayer(player[0]);
					chargeCount=Integer.parseInt(player[1]);
				}
				else {
					return false;
				}
			}
			catch (Exception e) {
				return false;
			}
			if(!uuid.equals("")) {
				getConfig().set(uuid+".blitz", getConfig().getInt(uuid+".blitz",0)+chargeCount);
				user.sendRawMessage(GreenTitle+"섬광방패 남은 사용횟수 : "+getConfig().getInt(uuid+".blitz"));
			}
			return true;
		}
		return false;//모든 명령어 처리에서 false 발생시 최종적으로 false 반환
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action=event.getAction();
		Player user=event.getPlayer();
		Location loc=user.getLocation();
		World world=user.getWorld();
		ItemStack handItem=user.getItemInHand();
		String tag="";
		try {
			tag=handItem.getItemMeta().getCustomTagContainer().getCustomTag(nbtName, ItemTagType.STRING);
			if(tag.isEmpty()) {
				return;
			}
		}
		catch(Exception e){
			return;
		}
		if(action.equals(Action.LEFT_CLICK_AIR)||action.equals(Action.LEFT_CLICK_BLOCK)) {
			if(tag.equals("blitz")&&blitCoolList.getOrDefault(user.getName(),true)) {
				int chargeCount=getConfig().getInt(user.getUniqueId()+".blitz",0);
				if(chargeCount<1) {
					user.sendRawMessage(RedTitle+"섬광방패의 배터리가 비었습니다.");
					return;
				}
				loc.add(0, 1.2, 0);
				Vector vector=loc.getDirection();
				world.playSound(loc,Sound.BLOCK_GLASS_BREAK,1,2);
				world.playSound(loc,Sound.BLOCK_GLASS_BREAK,1,0.5f);
				world.playSound(loc,Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 1.6f);
				world.playSound(loc,Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2, 1);
				world.playSound(loc,Sound.ITEM_TRIDENT_RIPTIDE_3, 0.7f, 1.5f);
				world.playSound(loc,Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 0.8f, 1.5f);
				world.spawnParticle(Particle.FLASH,loc.add(vector.multiply(1)),5);
				Location circle=loc.add(vector.multiply(2.1));
				PotionEffect blind=new PotionEffect(PotionEffectType.BLINDNESS,80,0);
				PotionEffect weak=new PotionEffect(PotionEffectType.WEAKNESS,60,0);
				PotionEffect glow=new PotionEffect(PotionEffectType.GLOWING, 20, 0);
				for(LivingEntity enemy:circle.getNearbyLivingEntities(2.9)) {
					if(enemy.getName().equals(user.getName())) {
						continue;
					}
					scheduler.scheduleSyncDelayedTask(this, new Runnable() {
			            @Override
			            public void run() {
							enemy.addPotionEffect(blind);
							enemy.addPotionEffect(weak);
							enemy.addPotionEffect(glow);
			            }
					},0);
				}
				for(Player enemy:circle.getNearbyPlayers(2.9)) {
					if(enemy.getName().equals(user.getName())) {
						continue;
					}
					enemy.playSound(enemy.getLocation(), Sound.BLOCK_BELL_RESONATE, 2, 2);
				}
				blitCoolList.put(user.getName(), false);
				getConfig().set(user.getUniqueId()+".blitz", --chargeCount);
            	user.sendRawMessage(GreenTitle+"섬광방패 남은 사용횟수 : "+chargeCount);
				scheduler.scheduleSyncDelayedTask(this, new Runnable() {
		            @Override
		            public void run() {
						world.playSound(user.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2);
		            }
				},100);
				scheduler.scheduleSyncDelayedTask(this, new Runnable() {
		            @Override
		            public void run() {
		            	world.playSound(user.getLocation(), Sound.ITEM_BOOK_PUT, 1,2);
		            	user.sendActionBar(ChatColor.YELLOW+""+ChatColor.BOLD+"섬광방패 충전완료.");
						blitCoolList.put(user.getName(), true);
		            }
				},120);
			}
			else if(tag.equals("glowtracking")&&glowTrackingCool.getOrDefault(user.getName(),true)) {
				glowTrackingCool.put(user.getName(), false);
				world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 2, 0.5f);
				world.playSound(loc, Sound.ENTITY_ARMOR_STAND_BREAK,1,2);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL,1,2);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_HURT,1,2);
				world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_OPEN,1, 0.5f);
				world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT,1, 0.5f);
				loc.add(0, 1.6, 0);
				Vector vector=loc.getDirection().multiply(0.5);
				loc.add(vector);
				Arrow arrow=world.spawnArrow(loc,vector,5,0);
				arrow.setDamage(0.1);
				arrow.setShooter(user);
				arrow.setSilent(true);
				arrow.setPickupRule(PickupRule.DISALLOWED);
				glowTrackingArrow.put(user.getName(),arrow);
				scheduler.scheduleSyncDelayedTask(this, new Runnable() {
		            @Override
		            public void run() {
		            	//playSound
		            }
				},380);
				scheduler.scheduleSyncDelayedTask(this, new Runnable() {
		            @Override
		            public void run() {
		            	user.sendActionBar(ChatColor.YELLOW+""+ChatColor.BOLD+"발광추적기 재장전 완료.");
						glowTrackingCool.put(user.getName(), true);
		            }
				},400);
			}
		}
		else if(action.equals(Action.RIGHT_CLICK_AIR)||action.equals(Action.RIGHT_CLICK_BLOCK)) {
			if(tag.equals("glowtracking")) {
				if(!glowTrackingArrow.containsKey(user.getName())){
					user.sendRawMessage(RedTitle+"작동시킬 발광추적기 탄자가 없습니다.");
					return;
				}
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS,1,1);
				Arrow arrow=glowTrackingArrow.get(user.getName());
				world.playSound(arrow.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS,0.5f,1);
				glowTrackingWork(world, arrow, 0,user);
				glowTrackingArrow.remove(user.getName());
			}
		}
	}
	public void glowTrackingWork(World world, Arrow arrow,int count,Player user) {
		if(count>9) {
			arrow.remove();
			world.playSound(arrow.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.6f);
			return;
		}
		PotionEffect glow=new PotionEffect(PotionEffectType.GLOWING, 10, 0);
		for(int a=0;a<20;a+=4) {
			scheduler.scheduleSyncDelayedTask(this, new Runnable() {
	            @Override
	            public void run() {
	    			world.playSound(arrow.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.3f, 1.6f);
	            }
			},a);
		}
		for(LivingEntity enemy:arrow.getLocation().getNearbyLivingEntities(10)) {
			if(enemy.getName().equals(user.getName())) {
				continue;
			}
			scheduler.scheduleSyncDelayedTask(this, new Runnable() {
	            @Override
	            public void run() {
					enemy.addPotionEffect(glow);
	            }
			},0);
		}
		scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
				glowTrackingWork(world, arrow, count+1, user);
            }
		},20);
	}
}