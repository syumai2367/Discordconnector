package org.syumai.plugin;

import com.github.ucchyocean.lc3.LunaChatBungee;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.syumai.plugin.lisner.DiscordLisner;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.syumai.plugin.lisner.bungeecordLisner;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Discordconnector extends Plugin implements Listener {


    static Config config;
    private LunaChatBungee lunaChat;
    private String servername;
    private static Discordconnector instance;
    private JDA jda;
    private final Map<String, CachedSkin> skinCache = new HashMap<>();
    private static final long CACHE_EXPIRY_TIME = TimeUnit.MINUTES.toMillis(10); // キャッシュの有効期限を10分に設定

    @Override
    public void onEnable() {
        instance = this;
        // Configクラスを初期化し、設定を読み込む
        config = new Config(this);
        String token = config.getBotToken();
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(25565)) { // Paperサーバーが接続するポート番号
                getLogger().info("Waiting for connection from Paper Plugin...");

                while (true) {
                    try (Socket socket = serverSocket.accept(); // Paperサーバーからの接続を受け入れる
                         DataInputStream in = new DataInputStream(socket.getInputStream())) {

                        String serverport = in.readUTF();
                        String servereb = in.readUTF();// 受信したメッセージを読み込む
                        if (servereb.equals("enable")) {
                            enablemessage(serverport);
                        } else if (servereb.equals("disable")) {
                            disablemessage(serverport);
                        }

                    } catch (IOException e) {
                        getLogger().severe("Error while receiving message: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                getLogger().severe("Failed to start ServerSocket: " + e.getMessage());
            }
        }).start();
        // フォールバックロガーを無効化
        JDALogger.setFallbackLoggerEnabled(false);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("MyCustomChannel");
        try {
            // JDAインスタンスを生成
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build();
            jda.awaitReady();  // Botが準備完了するまで待機

            getLogger().info("Discord Botが正常に起動しました！");
        } catch (InterruptedException e) {
            getLogger().severe("JDAの初期化中に中断されました: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            getLogger().severe("予期しないエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
        getLogger().info("Use token : " + config.getBotToken());

        // コマンドリストを作成
        //List<Object> commands = new ArrayList<>();
        //commands.add(new etestcommand(this, jda));  // etestcommandにJDAインスタンスを渡す
        //commands.add(new testcommand(this));   // testcommandをリストに追加

        getProxy().getPluginManager().registerListener(this, new bungeecordLisner(this));
        jda.addEventListener(new DiscordLisner(this));
        // ループを使って一括でコマンドを登録
//        for (Object command : commands) {
//            getProxy().getPluginManager().registerCommand(this, (net.md_5.bungee.api.plugin.Command) command);
//        }
        getLogger().info("複数のコマンドが登録されました。");
        Plugin temp = getProxy().getPluginManager().getPlugin("LunaChat");
        if (temp instanceof LunaChatBungee) {
            lunaChat = (LunaChatBungee) temp;
        }
        String channelId = config.getChannelId();
        // Embedメッセージを作成
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(":white_check_mark: サーバーが起動しました");
        embed.setDescription("Proxyサーバーが起動しました");
        embed.setColor(Color.CYAN);
        getLogger().info("config.getchannelId:" + config.getChannelId());
        // Discordの特定のチャンネルに送信
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
    }

    public void onDisable() {
        String channelId = config.getChannelId();
        // Embedメッセージを作成
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(":octagonal_sign: サーバーが停止しました");
        embed.setDescription("Proxyサーバーが停止しました");
        embed.setColor(Color.CYAN);
        getLogger().info("config.getchannelId:" + config.getChannelId());
        // Discordの特定のチャンネルに送信
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
        if (jda != null) {
            jda.shutdown();
        }
    }

    public static Discordconnector getInstance() {
        return instance;
    }

    public LunaChatBungee getLunaChat() {
        return lunaChat;
    }

    // Webhookメッセージを送信するメソッド
    public void sendWebhookMessage(String cmessage, String ncmessage, String avatarUrl, String Botname,String server) {
        String webhookUrl = config.getWebhookUrl();

        // JSONオブジェクトを作成
        JsonObject jsonPayload = new JsonObject();
        if (Objects.equals(ncmessage, cmessage)) {
            jsonPayload.addProperty("content", ncmessage);
        } else {
            jsonPayload.addProperty("content", ncmessage + "(" + cmessage + ")");
        }
        jsonPayload.addProperty("username", Botname + "@" + server);
        jsonPayload.addProperty("avatar_url", avatarUrl);

        // 送信前にJSONデータを確認
        System.out.println("Sending JSON payload: " + jsonPayload.toString());

        try {
            int responseCode = getResponseCode(webhookUrl, jsonPayload.toString());
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("Webhook sent successfully!");
            } else {
                System.out.println("Failed to send webhook: " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Error sending webhook: " + e.getMessage());
        }
    }


    private int getResponseCode(String webhookUrl, String jsonPayload) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            return connection.getResponseCode();
        } finally {
            if (connection != null) {
                connection.disconnect(); // 接続をクリーンアップ
            }
        }
    }

    // コマンドクラスから呼び出すインスタンスメソッドに変更
    public void sendWebhookData(String cmessage, String ncmessage, String avatarUrl, String Botname ,String server) {
        sendWebhookMessage(cmessage, ncmessage, avatarUrl, Botname,server);
    }

    public void enablemessage(String serverport) {
        EmbedBuilder embed = new EmbedBuilder();
        String channelId = config.getChannelId();
        String port = serverport;
        getLogger().info("port : " + port);
        servername = config.getservername(port);
        embed.setTitle(":white_check_mark:" + servername + "が起動しました");
        embed.setDescription("起動直後なので重いかもしれません");
        embed.setColor(Color.CYAN);
        getLogger().info("config.getchannelId:" + config.getChannelId());
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
    }
    public void disablemessage(String serverport) {
        EmbedBuilder embed = new EmbedBuilder();
        String channelId = config.getChannelId();
        String port = serverport;
        getLogger().info("port : " + port);
        servername = config.getservername(port);
        embed.setTitle(":white_check_mark:" + servername + "が終了しました");
        embed.setDescription("Server Closed");
        embed.setColor(Color.CYAN);
        getLogger().info("config.getchannelId:" + config.getChannelId());
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
    }

    public void embedchat(String embedtitle, String avatarurl2, String message) {
        String channelId = config.getChannelId();

        // Embedメッセージを作成
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(embedtitle);
        embed.setDescription(message);
        embed.setColor(Color.BLUE);
        embed.setThumbnail(avatarurl2);  // プレイヤーの顔画像をサムネイルとして表示
        getLogger().info("config.getchannelId:" + config.getChannelId());
        // Discordの特定のチャンネルに送信
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
    }
    public void advanceembed(String embedtitle, String avatarurl2, String message,String advancedicon) {
        String channelId = config.getChannelId();

        // Embedメッセージを作成
        EmbedBuilder embed = new EmbedBuilder();
        embed.setFooter(embedtitle,avatarurl2);
        embed.setDescription(message);
        embed.setColor(Color.MAGENTA);
        getLogger().info("config.getchannelId:" + config.getChannelId());
        // Discordの特定のチャンネルに送信
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
    }
    public void ntembedchat(String avatarurl2, String message) {
        String channelId = config.getChannelId();
        // Embedメッセージを作成
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(message);
        embed.setThumbnail(avatarurl2);
        embed.setColor(Color.BLUE);
        // Discordの特定のチャンネルに送信
        jda.getTextChannelById(channelId)
                .sendMessageEmbeds(embed.build())
                .queue();
    }

    public void broadcastDiscordMessage(String username, String message, ChatColor roleColor, String highestRole) {
        // メッセージフォーマットを太字に設定
        TextComponent openBracket = new TextComponent("[");
        openBracket.setBold(true);
        openBracket.setColor(ChatColor.AQUA);

        TextComponent discordText = new TextComponent("discord | ");
        discordText.setBold(true);
        TextComponent roleText = new TextComponent(highestRole);
        roleText.setBold(true);
        roleText.setColor(roleColor);
        TextComponent closeBracket = new TextComponent("] ");
        closeBracket.setBold(true);
        closeBracket.setColor(ChatColor.AQUA);

        // ユーザー名部分のTextComponentを作成し、ロールの色を適用
        TextComponent userNameText = new TextComponent(username);
        userNameText.setBold(true);
        TextComponent separator = new TextComponent(" >> ");
        separator.setBold(true);
        separator.setColor(ChatColor.YELLOW);

        // メッセージ部分のTextComponent
        TextComponent messageText = new TextComponent(message);
        messageText.setBold(true);

        // 最終メッセージを構築
        TextComponent finalMessage = new TextComponent();
        finalMessage.addExtra(openBracket);
        finalMessage.addExtra(discordText);
        finalMessage.addExtra(roleText);
        finalMessage.addExtra(closeBracket);
        finalMessage.addExtra(userNameText);
        finalMessage.addExtra(separator);
        finalMessage.addExtra(messageText);

        // 全プレイヤーにメッセージを送信
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            player.sendMessage(finalMessage);
        }
    }

    private static class CachedSkin {
        String avatarUrl;
        long cacheTime;

        CachedSkin(String avatarUrl, long cacheTime) {
            this.avatarUrl = avatarUrl;
            this.cacheTime = cacheTime;
        }
    }

    public String getDiscordChannelId() {
        return config.getChannelId();  // ConfigからchannelIDを取得
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("BungeeCord")) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
        try {
            String messageform = in.readUTF();
            if (messageform.equals("ntembed")) {
                String avatarurl2 = in.readUTF();
                String message = in.readUTF();
                ntembedchat(avatarurl2, message);
            }
            if (messageform.equals("advance")) {
                String embedtitle = in.readUTF();
                String avatarurl2 = in.readUTF();
                String message = in.readUTF();
                String advancedicon = in.readUTF();
                advanceembed(embedtitle, avatarurl2, message, advancedicon);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
