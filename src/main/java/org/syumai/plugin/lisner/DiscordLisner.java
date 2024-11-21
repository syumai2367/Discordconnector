package org.syumai.plugin.lisner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import org.syumai.plugin.Discordconnector;

import java.awt.Color;
import java.util.List;

public class DiscordLisner extends ListenerAdapter {
    private String roleColor;
    private Color discordColor;
    private final Discordconnector plugin;

    public DiscordLisner(Discordconnector plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // ボット自身のメッセージは無視する
        if (event.getAuthor().isBot()) {
            return;
        }

        // 指定のチャンネルのメッセージのみ処理
        if (event.getChannel().getId().equals(plugin.getDiscordChannelId())) {
            Message message = event.getMessage();
            String content = message.getContentDisplay();
            String authorName = message.getAuthor().getName();
            String highestRole = "No Role";
            Color discordColor = Color.WHITE;

            // 送信者の最高ロールを取得し、その色を取得
            Member member = event.getMember();
            ChatColor roleColor = ChatColor.WHITE;  // デフォルトの色は白
            if (member != null) {
                List<net.dv8tion.jda.api.entities.Role> roles = member.getRoles();
                if (!roles.isEmpty()) {
                    highestRole = roles.get(0).getName();
                    discordColor = roles.get(0).getColor();  // 最高ロールの色を取得
                }
            }

            // Minecraftサーバーにメッセージを送信
            plugin.broadcastDiscordMessage(authorName, content, ChatColor.of(discordColor), highestRole);
        }
    }
}