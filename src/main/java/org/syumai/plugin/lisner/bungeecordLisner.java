package org.syumai.plugin.lisner;

import com.github.ucchyocean.lc3.LunaChatBungee;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelChatEvent;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.geysermc.floodgate.api.FloodgateApi;
import org.syumai.plugin.Discordconnector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class bungeecordLisner implements Listener {
    private final HashMap<String, UUID> uuidData = new HashMap<>();
    private Discordconnector plugin;
    private String floodgatePlayer;
    private String avatarUrl;
    private static final String XUID_API_URL = "https://api.geysermc.org/v2/xbox/xuid/";
    private static final String TEXTURE_API_URL = "http://textures.minecraft.net/texture/";
    private String Botname;
    private String ncmessage;
    private String minecraftID;
    private String cmessage;
    private String server;
    private String test;
    private Set<ProxiedPlayer> firstServerSwitch = new HashSet<>();

    public bungeecordLisner(Discordconnector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void getUsername(ChatEvent event) {
        if (event.isCommand()) {
            return;
        }
        else {
            Botname = String.valueOf(event.getSender());
            minecraftID = Botname;
            ncmessage = event.getMessage();
            avatarUrl = getSkinUrlFromUUID(minecraftID);
            ProxiedPlayer player = (ProxiedPlayer) event.getSender();
            server = player.getServer().getInfo().getName();
        }
    }

    @EventHandler
    public boolean onchat(LunaChatBungeeChannelChatEvent event) {
        // Fetch event details
        String message = event.getNgMaskedMessage();
        LunaChatBungee lunaChat = Discordconnector.getInstance().getLunaChat();
        String marker = lunaChat.getConfig().getNoneJapanizeMarker();
        if (message.contains("@everyone") || message.contains("@here")) {
            return true;  // キャンセルする
        }

        // Determine if the message needs to be japanized
        boolean japanese = true;
        if (!marker.isEmpty() && event.getPreReplaceMessage().startsWith(marker)) {
            japanese = false;
        }
        if (!lunaChat.getLunaChatAPI().isPlayerJapanize(event.getMember().getName())) {
            japanese = false;
        }
        if (japanese) {
            // Offload the japanization task to a separate thread
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                String cmessage = lunaChat.getLunaChatAPI().japanize(message, JapanizeType.GOOGLE_IME); // Japanize message asynchronously
                plugin.getLogger().info("PreReplaceMessage : " + event.getPreReplaceMessage());

                // Send Webhook data
                plugin.sendWebhookData(cmessage, event.getPreReplaceMessage(), avatarUrl, Botname,server);
            });
        } else {
            // Handle cases without japanization
            plugin.sendWebhookData(message, event.getPreReplaceMessage(), avatarUrl, Botname,server);
        }
        return japanese;
    }
    @EventHandler
    public void playerconnect(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String playerName = player.getName();
        minecraftID = playerName;
        UUID playerUUID = player.getUniqueId();
        uuidData.put(playerName, playerUUID);
        // プレイヤーのスキンURLをキャッシュまたは取得
        String avatarurl2;
        String embedtitle = "サーバー参加通知！";
        String message = "`" + playerName + "`"+"がサーバーに接続しました";
        try {
            avatarurl2 = getSkinUrlFromUUID(minecraftID);  // キャッシュまたはAPIからスキンURLを取得
        } catch (Exception e) {
            throw new RuntimeException(e);
        } // キャッシュまたはAPIからスキンURLを取得
        firstServerSwitch.add(player);
        plugin.embedchat(embedtitle, avatarurl2, message);
    }
    @EventHandler
    public void playerdisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String playerName = player.getName();
        minecraftID = playerName;
        // プレイヤーのスキンURLをキャッシュまたは取得
        String avatarurl2;
        String embedtitle = "サーバー切断通知";
        String message = "`" + playerName + "`"+"がサーバーから切断しました";
        try {
            avatarurl2 = getSkinUrlFromUUID(minecraftID);  // キャッシュまたはAPIからスキンURLを取得
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        firstServerSwitch.remove(player);
        plugin.embedchat(embedtitle, avatarurl2, message);
    }
    @EventHandler
    public void playerserverchange(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String playerName = player.getName();
        String server = player.getServer().getInfo().getName();
        // プレイヤーのスキンURLをキャッシュまたは取得
        String avatarurl2;
        minecraftID = playerName;
        String embedtitle = "サーバー移動通知!";
        String message ="`" + playerName + "`"+"が"+server+"に移動しました！";
        try {
            avatarurl2 = getSkinUrlFromUUID(minecraftID);  // キャッシュまたはAPIからスキンURLを取得
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (firstServerSwitch.contains(player)) {
            firstServerSwitch.remove(player);
            return;
        }
        plugin.embedchat(embedtitle, avatarurl2, message);
    }
    private String getSkinUrlFromUUID(String MinecraftID) {
        String uuid = String.valueOf(getUUID(minecraftID));
        if (FloodgateApi.getInstance().isFloodgatePlayer(UUID.fromString(uuid))) {
            floodgatePlayer = String.valueOf(FloodgateApi.getInstance().getPlayer(UUID.fromString(uuid)));
        }
        if (floodgatePlayer != null) {
            return "https://crafthead.net/helm/" + uuid + "/32.png";
        } else {
            return "https://crafthead.net/helm/" + uuid + "/32.png";
        }
    }
    public UUID getUUID(String minecraftID) {
        return uuidData.get(minecraftID);
    }
}
