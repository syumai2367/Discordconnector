package org.syumai.plugin;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
public class Config {

    private final Discordconnector plugin;
    private String webhookUrl;
    private String botToken;
    private String channelId;
    private String servername;
    private String localip;
    public Config(Discordconnector plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // config.ymlをロードし、設定を読み込む
    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("config.ymlをデフォルトで作成しました。");
                } else {
                    plugin.getLogger().warning("resourcesからconfig.ymlを取得できませんでした。");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            webhookUrl = config.getString("discord.webhook-url");
            botToken = config.getString("discord.bot-token");
            localip = config.getString("localip");
            channelId = config.getString("discord.channel-id");

            // 設定値が取得できたかチェック
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                plugin.getLogger().warning("DiscordのWebhook URLが設定されていません！");
            }
            if (botToken == null || botToken.isEmpty()) {
                plugin.getLogger().warning("DiscordのBot Tokenが設定されていません！");
            }
            if (channelId == null || channelId.isEmpty()) {
                plugin.getLogger().warning("DiscordのChannel IDが設定されていません！");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void findportserver(String port) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            servername = config.getString("minecraft.servers." + port);
            plugin.getLogger().info("minecraft.servers." + port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // Getterメソッドで各設定を取得する
    public String getWebhookUrl() {
        return webhookUrl;
    }
    public String getBotToken() {
        return botToken;
    }
    public String getservername(String port) {
        findportserver(port);
        plugin.getLogger().info("getserevername : " + servername);
        return servername;
    }
    public String getChannelId() {
        return channelId;
    }
    public String getLocalip() {
        return localip;
    }
}
