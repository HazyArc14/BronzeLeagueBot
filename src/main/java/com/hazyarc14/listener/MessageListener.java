package com.hazyarc14.listener;

import com.hazyarc14.audio.GuildMusicManager;
import com.hazyarc14.model.Command;
import com.hazyarc14.model.UserRank;
import com.hazyarc14.repository.CommandRepository;
import com.hazyarc14.repository.UserRanksRepository;
import com.hazyarc14.service.UserRankService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MessageListener extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    @Autowired
    UserRanksRepository userRanksRepository;

    @Autowired
    UserRankService userRankService;

    @Autowired
    CommandRepository commandRepository;

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private static final String reactionNoVote = "U+274C";
    private static final String reactionYesVote = "U+2705";

    private static final List<String> allowedFileExtensions = Arrays.asList("png", "jpg", "gif", "mp3");

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] commandList = message.getContentRaw().split(" ");

        Boolean isPrivate = event.isFromType(ChannelType.PRIVATE);
        VoiceChannel voiceChannel = null;
        String voiceChannelId = "";

        for (String command: commandList) {
            if (command.contains("$vc"))
                voiceChannelId = command.substring(command.lastIndexOf("$vc") + 3);
        }

        if (event.getChannel().getName().equals("bot-suggestions")) {

            createCommandVote(event, message, content);

        } else if (commandList[0].equalsIgnoreCase("!help")) {

            if (!isPrivate) {
                message.delete().queue();
            }

            sendHelpMessage(event, isPrivate);

        } else if (commandList[0].equalsIgnoreCase("!rank")) {

            if (!isPrivate) {
                message.delete().queue();
            }

            Long targetUserId = 0L;

            if (commandList.length > 2) {
                if (commandList[1].matches("^<@\\d*>")) {
                    targetUserId = Long.valueOf(commandList[1].substring(2, commandList[1].length() - 1));
                }
            } else {
                targetUserId = message.getAuthor().getIdLong();
            }

            sendRankMessage(event, isPrivate, targetUserId);

        } else if (commandList[0].equalsIgnoreCase("!rankAll")) {

            if (!isPrivate) {
                message.delete().queue();
            }

            sendRankAllMessage(event, isPrivate);

        } else if (commandList[0].startsWith(";") && commandList[0].endsWith(";")) {

            if (!isPrivate) {
                message.delete().queue();
            }

            String command = commandList[0].substring(1, content.length() - 1);
            sendCommand(event, isPrivate, command);

        } else if (commandList[0].startsWith("!")) {

            if (!isPrivate) {
                message.delete().queue();
            }

            if (!voiceChannelId.equalsIgnoreCase("")) {
                try {
                    voiceChannel = event.getGuild().getVoiceChannelById(voiceChannelId);
                } catch (Exception e) {
                    log.error("Could not get voice channel by id " + voiceChannelId + " :: ", e);
                }
            } else {
                voiceChannel = event.getMember().getVoiceState().getChannel();
            }

            String commandValue = commandList[0].substring(1);
            if (commandValue.equalsIgnoreCase("play")) {

                Integer trackPosition = 0;

                if (commandList[1].contains("?t="))
                    trackPosition = Integer.valueOf(commandList[1].substring(commandList[1].lastIndexOf("?t=") + 3));

                loadAndPlay(event.getGuild(), voiceChannel, commandList[1], trackPosition);

            } else if (commandValue.equalsIgnoreCase("skip")) {
                skipTrack(event.getGuild());
            } else {

                VoiceChannel finalVoiceChannel = voiceChannel;
                commandRepository.findById(commandValue).ifPresent(command -> {

                    if (command.getActive()) {

                        File commandFile = new File(command.getCommandName() + "." + command.getCommandFileExtension());
                        try {
                            FileUtils.writeByteArrayToFile(commandFile, command.getCommandFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        loadAndPlay(event.getGuild(), finalVoiceChannel, commandFile.getAbsolutePath(), 0);

                    }

                });

            }

        }

    }

    private void sendHelpMessage(MessageReceivedEvent event, Boolean isPrivate) {

        String basicCommands = "";
        String voiceCommands = "";
        String emoteCommands = "";

        basicCommands += "!help\n" +
                "!rank or !rank @<user> ex: !rank @HazyArc14\n" +
                "!rankAll\n";

        voiceCommands += "!play <YouTube Link>\n" +
                "!skip\n";

        List<Command> commandList = commandRepository.findAll(Sort.by(Sort.Direction.ASC, "commandName"));
        for (Command command : commandList) {
            if (command.getActive()) {
                if (command.getCommandFileExtension().equalsIgnoreCase("mp3")) {
                    voiceCommands += "!" + command.getCommandName() + "\n";
                } else {
                    emoteCommands += ";" + command.getCommandName() + ";\n";
                }
            }
        }

        String helpMessage = "```Basic Commands:\n" + basicCommands + "\nVoice Commands:\n" + voiceCommands + "\nEmote Commands:\n" + emoteCommands + "```";

        if (isPrivate)
            event.getPrivateChannel().sendMessage(helpMessage).queue();
        else
            event.getChannel().sendMessage(helpMessage).queue();

    }

    private void sendRankMessage(MessageReceivedEvent event, Boolean isPrivate, Long targetUserId) {

        userRanksRepository.findById(targetUserId).ifPresent(userRank -> {

            String rankMessage = userRank.getUserName() + "'s Current Rank is " + userRank.getRank();

            if (isPrivate)
                event.getPrivateChannel().sendMessage(rankMessage).queue();
            else
                event.getChannel().sendMessage(rankMessage).queue();

        });

    }

    private void sendRankAllMessage(MessageReceivedEvent event, Boolean isPrivate) {

        String rankAllMessage = "```Current User Ranks:\n";

        List<UserRank> userRankList = userRanksRepository.findAll(Sort.by(Sort.Direction.DESC, "rankingNbr"));
        for (UserRank userRank: userRankList) {

            rankAllMessage += userRank.getUserName() + " - " + userRank.getRank() + "\n";

        }
        rankAllMessage += "```";

        if (isPrivate)
            event.getPrivateChannel().sendMessage(rankAllMessage).queue();
        else
            event.getChannel().sendMessage(rankAllMessage).queue();

    }

    private void sendCommand(MessageReceivedEvent event, Boolean isPrivate, String commandName) {

        commandRepository.findById(commandName).ifPresent(command -> {
            if (command.getActive()) {
                if (isPrivate)
                    event.getPrivateChannel().sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).queue();
                else
                    event.getChannel().sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).queue();
            }
        });

    }

    private void createCommandVote(MessageReceivedEvent event, Message message, String content) {

        MessageChannel channel = event.getChannel();
        User author = event.getAuthor();

        List<String> contentList = Arrays.asList(content.split(" "));
        List<Message.Attachment> attachmentList = message.getAttachments();

        if (!content.startsWith("!new") || (contentList.size() < 2 || attachmentList.size() == 0)) {

            message.delete().queue();

            String helpMessage = "Incorrect command. Use the following:\n" +
                    "```!new commandName\n" +
                    "ex: !new widePeppoHappy```";
            channel.sendMessage(helpMessage).queue(sentMessage -> {
                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                    sentMessage.delete().queue();
                });
            });

        } else if (attachmentList.size() > 1) {

            message.delete().queue();

            channel.sendMessage("You can only upload one attachment at a time.").queue(sentMessage -> {
                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                    sentMessage.delete().queue();
                });
            });

        } else {

            Message.Attachment attachment = attachmentList.get(0);
            String fileExtension = attachment.getFileExtension();

            if (!allowedFileExtensions.contains(fileExtension)) {

                message.delete().queue();

                channel.sendMessage("Incorrect file extension. Use one of the following: " + allowedFileExtensions.toString()).queue(sentMessage -> {
                    CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                        sentMessage.delete().queue();
                    });
                });

            } else {

                Command commandSuggestion = new Command();
                commandSuggestion.setCommandName(contentList.get(1));
                commandSuggestion.setCommandFileExtension(fileExtension);
                commandSuggestion.setActive(false);

                commandRepository.findById(commandSuggestion.getCommandName()).ifPresentOrElse(command -> {

                    message.delete().queue();

                    if (command.getActive()) {

                        channel.sendMessage("The `" + command.getCommandName() + "` command already exists.").queue(sentMessage -> {
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                sentMessage.delete().queue();
                            });
                        });

                    } else {

                        channel.sendMessage("The `" + command.getCommandName() + "` command is still being voted on.").queue(sentMessage -> {
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                sentMessage.delete().queue();
                            });
                        });

                    }

                }, () -> {

                    attachment.retrieveInputStream().whenComplete((inputStream, throwable) -> {

                        if (throwable != null)
                            log.error("Error:", throwable);

                        try {
                            commandSuggestion.setCommandFile(inputStream.readAllBytes());
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (commandSuggestion.getCommandFile() != null) {

                            EmbedBuilder eb = new EmbedBuilder();

                            eb.setColor(Color.yellow);
                            eb.setDescription("Suggested Command: " + commandSuggestion.getCommandName());
                            eb.setAuthor(author.getName(), null, author.getAvatarUrl());

                            if (commandSuggestion.getCommandFileExtension().equalsIgnoreCase("mp3")) {
                                eb.setTitle("Add This New Voice Command?", null);
                            } else {
                                eb.setTitle("Add This New Emote?", null);
                                eb.setImage("attachment://" + commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension());
                            }

                            channel.sendFile(commandSuggestion.getCommandFile(), commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension()).embed(eb.build()).queue(sentMessage -> {
                                sentMessage.addReaction(reactionNoVote).queue();
                                sentMessage.addReaction(reactionYesVote).queue();
                                commandRepository.save(commandSuggestion);
                            });

                        } else {

                            log.info("File was empty");

                        }

                        message.delete().queue();

                    });

                });

            }

        }

    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        if (event.getUser().isBot()) return;

        if (!event.getChannel().getName().equals("bot-suggestions")) return;

        User reactionUser = event.getUser();
        String reactionAdded = event.getReactionEmote().getAsCodepoints();
        Long messageId = event.getReaction().getMessageIdLong();

        MessageChannel channel = event.getChannel();
        Message message = channel.retrieveMessageById(messageId).complete();
        List<MessageReaction> messageReactions = message.getReactions();

        if (!message.getEmbeds().isEmpty()) {

            MessageEmbed messageEmbed = message.getEmbeds().get(0);

            if (messageEmbed.getFooter() != null) return;

            String commandName = messageEmbed.getDescription().split(": ")[1];
            commandRepository.findById(commandName).ifPresent(command -> {

                if (reactionAdded.equalsIgnoreCase(reactionNoVote)) {

                    messageReactions.forEach(reaction -> {

                        if (reaction.getReactionEmote().getAsCodepoints().equalsIgnoreCase(reactionYesVote)) {

                            reaction.retrieveUsers().forEach(user -> {

                                if (user.getIdLong() == reactionUser.getIdLong()) {

                                    reaction.removeReaction(reactionUser).queue();

                                }

                            });

                        }

                    });

                } else if (reactionAdded.equalsIgnoreCase(reactionYesVote)) {

                    messageReactions.forEach(reaction -> {

                        if (reaction.getReactionEmote().getAsCodepoints().equalsIgnoreCase(reactionNoVote)) {

                            reaction.retrieveUsers().forEach(user -> {

                                if (user.getIdLong() == reactionUser.getIdLong()) {

                                    reaction.removeReaction(reactionUser).queue();

                                }

                            });

                        }

                    });

                }

                Integer noVotesCount = 0;
                Integer yesVotesCount = 0;

                for (MessageReaction reaction: messageReactions) {

                    if (reaction.getReactionEmote().getAsCodepoints().equalsIgnoreCase(reactionNoVote))
                        noVotesCount = reaction.getCount() - 1;

                    if (reaction.getReactionEmote().getAsCodepoints().equalsIgnoreCase(reactionYesVote))
                        yesVotesCount = reaction.getCount() - 1;

                }

                if (noVotesCount >= 2 || yesVotesCount >= 2) {

                    Boolean approved = false;
                    if (yesVotesCount > noVotesCount)
                        approved = true;

                    String responseMessage = "";
                    EmbedBuilder eb = new EmbedBuilder();

                    if (!approved) {
                        responseMessage = "Command Suggestion Not Approved";
                        eb.setColor(Color.red);
                        eb.setDescription(messageEmbed.getDescription());
                    } else if (approved) {
                        responseMessage = "Command Suggestion Approved";
                        eb.setColor(Color.green);
                        eb.setDescription("Command: " + command.getCommandName());
                    }

                    eb.setTitle("Voting Closed - " + responseMessage, null);

                    eb.setAuthor(messageEmbed.getAuthor().getName(), null, messageEmbed.getAuthor().getIconUrl());
                    eb.setFooter(noVotesCount + " No Votes / " + yesVotesCount + " Yes Votes");

                    if (!command.getCommandFileExtension().equalsIgnoreCase("mp3"))
                        eb.setImage("attachment://" + command.getCommandName() + "." + command.getCommandFileExtension());

                    event.getChannel().sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).embed(eb.build()).queue();

                    message.delete().queue();

                    if (approved) {
                        command.setActive(true);
                        commandRepository.save(command);
                    } else {
                        commandRepository.delete(command);
                    }

                }

            });

        }

    }

    public MessageListener() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager, guild);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public void loadAndPlay(Guild guild, VoiceChannel voiceChannel, String trackUrl, Integer trackPosition) {

        TextChannel defaultChannel = guild.getDefaultChannel();
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (trackPosition != 0) {
                    track.setPosition(1000 * trackPosition);
                }
                play(guild, musicManager, voiceChannel, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                play(guild, musicManager, voiceChannel, firstTrack);
            }

            @Override
            public void noMatches() {
                defaultChannel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                defaultChannel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });

    }

    private void play(Guild guild, GuildMusicManager musicManager, VoiceChannel voiceChannel, AudioTrack track) {

        connectVoiceChannel(guild.getAudioManager(), voiceChannel);
        musicManager.scheduler.queue(track);

    }

    public void skipTrack(Guild guild) {

        GuildMusicManager musicManager = getGuildAudioPlayer(guild);
        musicManager.scheduler.nextTrack();

    }

    public static void connectVoiceChannel(AudioManager audioManager, VoiceChannel voiceChannel) {

        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            audioManager.openAudioConnection(voiceChannel);
        }

    }

}