package com.hazyarc14.listener;

import com.hazyarc14.audio.GuildMusicManager;
import com.hazyarc14.model.Command;
import com.hazyarc14.model.SeasonArchive;
import com.hazyarc14.model.SeasonRole;
import com.hazyarc14.model.UserInfo;
import com.hazyarc14.repository.*;
import com.hazyarc14.service.SteamAPIService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class MessageListener extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    UserRankService userRankService;

    @Autowired
    CommandRepository commandRepository;

    @Autowired
    SeasonInfoRepository seasonInfoRepository;

    @Autowired
    SeasonRolesRepository seasonRolesRepository;

    @Autowired
    SeasonArchiveRepository seasonArchiveRepository;

    @Autowired
    SteamAPIService steamAPIService;

    @Value("${bronze.league.bot.dev.mode}")
    private Boolean devMode;

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

        if ((event.getChannel().getName().equals("bot-testing") && devMode) ||
                (!event.getChannel().getName().equals("bot-testing") && !devMode)) {

            if (event.getChannel().getName().equals("bot-suggestions")) {

                Boolean override = false;
                for (String command: commandList) {
                    if (command.contains("~override") && event.getAuthor().getIdLong() == 148630426548699136L)
                        override = true;
                }

                commandVote(event, message, content, override);

            } else if (commandList[0].equalsIgnoreCase("!help")) {

                if (!isPrivate) {
                    message.delete().queue();
                    if (!event.getChannel().getName().equals("bot-commands")) return;
                }

                sendHelpMessage(event, isPrivate);

            } else if (commandList[0].equalsIgnoreCase("!startSeason2")) {

                if (isPrivate) {
                    event.getPrivateChannel().sendMessage("Not able to use this command in Direct Messages").queue();
                } else {
                    message.delete().queue();
                    startSeason2(event);
                }

            } else if (commandList[0].equalsIgnoreCase("!roleRebalance")) {

                if (isPrivate) {
                    event.getPrivateChannel().sendMessage("Not able to use this command in Direct Messages").queue();
                } else {
                    message.delete().queue();
                    userRankService.updateAllUserRoles(event.getGuild());
                }

            } else if (commandList[0].equalsIgnoreCase("!rank")) {

                if (!isPrivate) {
                    message.delete().queue();
                }

                Long targetUserId = 0L;

                if (commandList.length > 1) {
                    if (commandList[1].matches("^<@!\\d*>")) {
                        targetUserId = Long.valueOf(commandList[1].substring(3, commandList[1].length() - 1));
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

            } else if (commandList[0].equalsIgnoreCase("!roleInfo")) {

                if (!isPrivate) {
                    message.delete().queue();
                }

                sendRoleInfoMessage(event, isPrivate);

            } else if (commandList[0].equalsIgnoreCase("!steam")) {

                if (isPrivate) {
                    event.getPrivateChannel().sendMessage("Not able to use this command in Direct Messages").queue();
                } else {

                    message.delete().queue();

                    if (commandList.length > 1) {

                        if (commandList[1].matches("^\\d*")) {

                            Long steamId = Long.valueOf(commandList[1]);
                            userInfoRepository.findById(event.getAuthor().getIdLong()).ifPresent(userInfo -> {
                                UserInfo updatedUserInfo = userInfo;
                                updatedUserInfo.setSteamId(steamId);
                                userInfoRepository.save(updatedUserInfo);
                            });

                        } else {

                            String errorMessage = "Incorrect SteamID format. Use the following:\n" +
                                    "```!steam 123456789```";
                            event.getChannel().sendMessage(errorMessage).queue(sentMessage -> {
                                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                                    sentMessage.delete().queue();
                                });
                            });

                        }

                    } else {

                        String errorMessage = "Incorrect SteamID format. Use the following:\n```!steam 123456789```";
                        event.getChannel().sendMessage(errorMessage).queue(sentMessage -> {
                            CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                                sentMessage.delete().queue();
                            });
                        });

                    }

                }

            } else if (commandList[0].equalsIgnoreCase("!commonGames")) {

                if (isPrivate) {
                    event.getPrivateChannel().sendMessage("Not able to use this command in Direct Messages").queue();
                } else {

                    message.delete().queue();

                    Long eventUserId = event.getAuthor().getIdLong();

                    if (event.getGuild().getMemberById(eventUserId).getVoiceState().inVoiceChannel()) {

                        VoiceChannel connectedVoiceChannel = event.getGuild().getMemberById(eventUserId).getVoiceState().getChannel();
                        List<Member> connectedVoiceChannelMembers = connectedVoiceChannel.getMembers();

                        if (connectedVoiceChannelMembers.size() > 1) {

                            steamAPIService.findCommonSteamGames(event, connectedVoiceChannelMembers);

                        } else {

                            event.getChannel().sendMessage("Not enough users in the voice channel to use that command.").queue(sentMessage -> {
                                CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                    sentMessage.delete().queue();
                                });
                            });

                        }

                    } else {

                        event.getChannel().sendMessage("You have to be in a voice channel to use that command.").queue(sentMessage -> {
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                sentMessage.delete().queue();
                            });
                        });

                    }

                }

            } else if (commandList[0].startsWith(";") && commandList[0].endsWith(";")) {

                if (!isPrivate) {
                    message.delete().queue();
                }

                String command = commandList[0].substring(1, content.length() - 1);
                sendCommand(event, isPrivate, command);

            } else if (commandList[0].startsWith("!")) {

                if (isPrivate) {
                    event.getPrivateChannel().sendMessage("Not able to use this command in Direct Messages").queue();
                } else {

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

                        if (commandList[1].contains("&t="))
                            trackPosition = Integer.valueOf(commandList[1].substring(commandList[1].lastIndexOf("&t=") + 3));

                        loadAndPlay(event.getGuild(), voiceChannel, commandList[1], trackPosition);

                    } else if (commandValue.equalsIgnoreCase("skip")) {
                        skipTrack(event.getGuild());
                    } else {

                        VoiceChannel finalVoiceChannel = voiceChannel;
                        commandRepository.findById(commandValue).ifPresent(command -> {

                            if (command.getActive() && command.getCommandFileExtension().equalsIgnoreCase("mp3")) {

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

                    message.delete().queue();

                }

            }

        }

    }

    private void sendHelpMessage(MessageReceivedEvent event, Boolean isPrivate) {

        String basicCommands = "";
        String voiceCommands = "";
        String emoteCommands = "";

        basicCommands += "!help\n" +
                "!rank or !rank @<user> ex: !rank @HazyArc14\n" +
                "!rankAll\n" +
                "!roleInfo\n" +
                "!steam steamID ex: !steam 123456789\n" +
                "!commonGames\n";

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

        Guild guild = event.getGuild();

        userInfoRepository.findById(targetUserId).ifPresent(userInfo -> {

            Member targetMember = event.getGuild().getMemberById(targetUserId);
            UserInfo updatedUserInfo = userInfo;

            SeasonRole currentUserRank = userRankService.calculateRoleByRank(updatedUserInfo.getRank());
            SeasonRole nextUserRank = userRankService.nextSeasonRole(currentUserRank);
            List<Role> roles = event.getGuild().getRolesByName(currentUserRank.getRoleName(), false);

            Double pointsToNextRank = nextUserRank.getRoleValue() - updatedUserInfo.getRank();
            String ebDescription;
            if (!nextUserRank.getRoleName().equalsIgnoreCase("MAX")) {
                ebDescription = String.format("%.2f", pointsToNextRank) + " points until next rank (" + String.format("%.2f", updatedUserInfo.getRank()) + "/" + (int) nextUserRank.getRoleValue() + ")";
            } else {
                ebDescription = "You have the highest rank in the land!";
            }

            if (!roles.isEmpty()) {

                var currentSeason = seasonInfoRepository.findById("current_season").stream().findFirst().get().getValue();
                String seasonEmbedTitle = null;
                if (currentSeason.equalsIgnoreCase("season_1")) {
                    seasonEmbedTitle = "Season 1";
                } else if (currentSeason.equalsIgnoreCase("season_2")) {
                    seasonEmbedTitle = "Season 2";
                }

                Color rankColor = roles.get(0).getColor();

                EmbedBuilder eb = new EmbedBuilder();

                eb.setColor(rankColor);
                eb.setTitle(seasonEmbedTitle + " Rank: " + currentUserRank.getRoleName());
                eb.setDescription(ebDescription);
                eb.setAuthor(userInfo.getUserName(), null, targetMember.getUser().getAvatarUrl());

                if (isPrivate) {
                    event.getPrivateChannel().sendMessage("Current Role & Rank").embed(eb.build()).queue();
                } else {
                    event.getChannel().sendMessage("Current Role & Rank").embed(eb.build()).queue(sentMessage -> {
                        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                            sentMessage.delete().queue();
                        });
                    });
                }

            }

        });

    }

    private void sendRankAllMessage(MessageReceivedEvent event, Boolean isPrivate) {

        var currentSeason = seasonInfoRepository.findById("current_season").stream().findFirst().get().getValue();
        var seasonRoles = seasonRolesRepository.findAllBySeason(currentSeason);

        SeasonRole bronze = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Bronze")).findFirst().get();
        SeasonRole silver = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Silver")).findFirst().get();
        SeasonRole gold = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Gold")).findFirst().get();
        SeasonRole platinum = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Platinum")).findFirst().get();
        SeasonRole diamond = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Diamond")).findFirst().get();
        SeasonRole master = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Master")).findFirst().get();
        SeasonRole grandMaster = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("GrandMaster")).findFirst().get();

        String grandMasterTitle = grandMaster.getRoleName() + " (" + (int) grandMaster.getRoleValue() + ")";
        String masterTitle = master.getRoleName() + " (" + (int) master.getRoleValue() + " - " + (int)(grandMaster.getRoleValue() - 1) + ")";
        String diamondTitle = diamond.getRoleName() + " (" + (int) diamond.getRoleValue() + " - " + (int)(master.getRoleValue() - 1) + ")";
        String platinumTitle = platinum.getRoleName() + " (" + (int) platinum.getRoleValue() + " - " + (int)(diamond.getRoleValue() - 1) + ")";
        String goldTitle = gold.getRoleName() + " (" + (int) gold.getRoleValue() + " - " + (int)(platinum.getRoleValue() - 1) + ")";
        String silverTitle = silver.getRoleName() + " (" + (int) silver.getRoleValue() + " - " + (int)(gold.getRoleValue() - 1) + ")";
        String bronzeTitle = bronze.getRoleName() + " (" + (int) bronze.getRoleValue() + " - " + (int)(silver.getRoleValue() - 1) + ")";

        String grandMasterUsers = "";
        String masterUsers = "";
        String diamondUsers = "";
        String platinumUsers = "";
        String goldUsers = "";
        String silverUsers = "";
        String bronzeUsers = "";

        List<UserInfo> userInfoList = userInfoRepository.findAll(Sort.by(Sort.Direction.DESC, "rank"));
        for (UserInfo userInfo : userInfoList) {

            Double userRank = userInfo.getRank();
            if (userRank != 0.0) {

                if (userRank >= grandMaster.getRoleValue())
                    grandMasterUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";
                else if (userRank >= master.getRoleValue())
                    masterUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";
                else if (userRank >= diamond.getRoleValue())
                    diamondUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";
                else if (userRank >= platinum.getRoleValue())
                    platinumUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";
                else if (userRank >= gold.getRoleValue())
                    goldUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";
                else if (userRank >= silver.getRoleValue())
                    silverUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";
                else if (userRank >= bronze.getRoleValue())
                    bronzeUsers += userInfo.getUserName() + " - " + String.format("%.2f", userRank) + "\n";

            }

        }

        grandMasterUsers = (grandMasterUsers.isBlank() || grandMasterUsers.isEmpty()) ? "" : "```" + grandMasterUsers + "```";
        masterUsers = (masterUsers.isBlank() || masterUsers.isEmpty()) ? "" : "```" + masterUsers + "```";
        diamondUsers = (diamondUsers.isBlank() || diamondUsers.isEmpty()) ? "" : "```" + diamondUsers + "```";
        platinumUsers = (platinumUsers.isBlank() || platinumUsers.isEmpty()) ? "" : "```" + platinumUsers + "```";
        goldUsers = (goldUsers.isBlank() || goldUsers.isEmpty()) ? "" : "```" + goldUsers + "```";
        silverUsers = (silverUsers.isBlank() || silverUsers.isEmpty()) ? "" : "```" + silverUsers + "```";
        bronzeUsers = (bronzeUsers.isBlank() || bronzeUsers.isEmpty()) ? "" : "```" + bronzeUsers + "```";

        String seasonEmbedTitle = null;
        if (currentSeason.equalsIgnoreCase("season_1")) {
            seasonEmbedTitle = "Season 1";
        } else if (currentSeason.equalsIgnoreCase("season_2")) {
            seasonEmbedTitle = "Season 2";
        }

        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.GREEN);
        eb.setTitle(seasonEmbedTitle + " Ranks");
        eb.addField(grandMasterTitle, grandMasterUsers, false);
        eb.addField(masterTitle, masterUsers, false);
        eb.addField(diamondTitle, diamondUsers, false);
        eb.addField(platinumTitle, platinumUsers, false);
        eb.addField(goldTitle, goldUsers, false);
        eb.addField(silverTitle, silverUsers, false);
        eb.addField(bronzeTitle, bronzeUsers, false);

        if (isPrivate)
            event.getPrivateChannel().sendMessage("⠀").embed(eb.build()).queue();
        else
            event.getChannel().sendMessage("⠀").embed(eb.build()).queue();

    }

    private void sendRoleInfoMessage(MessageReceivedEvent event, Boolean isPrivate) {

        String roleInfoMessage = null;

        var currentSeason = seasonInfoRepository.findById("current_season").stream().findFirst().get().getValue();
        if (currentSeason.equalsIgnoreCase("season_1")) {

            roleInfoMessage = "```\n" +
                    "Season 1 Info:\n\n" +
                    "What is all this role business?\n" +
                    " - This server has 7 roles and they are Bronze, Silver, Gold, Platinum, Diamond, Master, & GrandMaster\n" +
                    "\n" +
                    "How do I get these roles?\n" +
                    " - Simple, just be in the voice channel to get points.\n" +
                    "\n" +
                    "How many points do I get?\n" +
                    " - 1 point every 10 minutes.\n" +
                    " - Server Boosters get a 1.10x multiplier.\n" +
                    " - If there are 8+ people in a single channel, everyone in that channel gets a 2.00x multiplier.\n" +
                    " **Server Booster and Channel Members multiplier do stack.**\n" +
                    "\n" +
                    "Can I just AFK?\n" +
                    " - Nope. There has to be 2 or more people in the channel.\n" +
                    "\n" +
                    "Does the bot count as a person?\n" +
                    " - Nope. We fixed that ;)\n" +
                    "\n" +
                    "How many points do I need to get to the next role?\n" +
                    " - Bronze = 0\n" +
                    " - Silver = 100\n" +
                    " - Gold = 200\n" +
                    " - Platinum = 500\n" +
                    " - Diamond = 1250\n" +
                    " - Master = 2000\n" +
                    " - GrandMaster = 3500\n" +
                    "```";

        } else if (currentSeason.equalsIgnoreCase("season_2")) {

            roleInfoMessage = "```\n" +
                    "Season 2 Info:\n\n" +
                    "What is all this role business?\n" +
                    " - This server has 7 roles and they are Bronze, Silver, Gold, Platinum, Diamond, Master, & GrandMaster\n" +
                    "\n" +
                    "How do I get these roles?\n" +
                    " - Simple, just be in the voice channel to get points.\n" +
                    "\n" +
                    "How many points do I get?\n" +
                    " - 1 point every 10 minutes.\n" +
                    " - Server Boosters get a 1.10x multiplier.\n" +
                    " - If there are 8+ people in a single channel, everyone in that channel gets a 2.00x multiplier.\n" +
                    " **Server Booster and Channel Members multiplier do stack.**\n" +
                    "\n" +
                    "Can I just AFK?\n" +
                    " - Nope. There has to be 2 or more people in the channel.\n" +
                    "\n" +
                    "Does the bot count as a person?\n" +
                    " - Nope. We fixed that ;)\n" +
                    "\n" +
                    "How many points do I need to get to the next role?\n" +
                    " - Bronze = 0\n" +
                    " - Silver = 3\n" +
                    " - Gold = 30\n" +
                    " - Platinum = 100\n" +
                    " - Diamond = 750\n" +
                    " - Master = 2250\n" +
                    " - GrandMaster = 6000\n" +
                    "```";

        }

        if (isPrivate)
            event.getPrivateChannel().sendMessage(roleInfoMessage).queue();
        else
            event.getChannel().sendMessage(roleInfoMessage).queue();

    }

    private void sendCommand(MessageReceivedEvent event, Boolean isPrivate, String commandName) {

        commandRepository.findById(commandName).ifPresent(command -> {
            if (command.getActive() && !command.getCommandFileExtension().equalsIgnoreCase("mp3")) {
                if (isPrivate) {
                    event.getPrivateChannel().sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).queue();
                } else {

                    User author = event.getAuthor();

                    userInfoRepository.findById(author.getIdLong()).ifPresent(userInfo -> {

                        SeasonRole currentUserRank = userRankService.calculateRoleByRank(userInfo.getRank());
                        List<Role> roles = event.getGuild().getRolesByName(currentUserRank.getRoleName(), false);

                        if (!roles.isEmpty()) {

                            Color rankColor = roles.get(0).getColor();

                            EmbedBuilder eb = new EmbedBuilder();

                            eb.setColor(rankColor);
                            eb.setAuthor(author.getName(), null, author.getAvatarUrl());
                            eb.setImage("attachment://" + command.getCommandName() + "." + command.getCommandFileExtension());

                            event.getChannel().sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).embed(eb.build()).queue();

                        }

                    });
                }
            }
        });

    }

    private void commandVote(MessageReceivedEvent event, Message message, String content, Boolean override) {

        MessageChannel channel = event.getChannel();
        String action = content.split(" ")[0];

        if (action.equalsIgnoreCase("!create") ||
                ((action.equalsIgnoreCase("!update") || action.equalsIgnoreCase("!delete")) && override)) {

            if (action.equalsIgnoreCase("!create")) {

                createCommand(event, message, content, override);

            } else if (action.equalsIgnoreCase("!update")) {

                updateCommand(event, message, content, override);

            } else if (action.equalsIgnoreCase("!delete")) {

                deleteCommand(event, message, content, override);

            }

        } else {

            message.delete().queue();

            String helpMessage = "Incorrect command. Use one of the following:\n" +
                    "```!create commandName\n" +
                    "!update commandName\n" +
                    "!delete commandName\n" +
                    "ex: !create widePeppoHappy```";

            channel.sendMessage(helpMessage).queue(sentMessage -> {
                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                    sentMessage.delete().queue();
                });
            });

        }

    }

    private void createCommand(MessageReceivedEvent event, Message message, String content, Boolean override) {

        MessageChannel channel = event.getChannel();
        User author = event.getAuthor();

        List<String> contentList = Arrays.asList(content.split(" "));
        List<Message.Attachment> attachmentList = message.getAttachments();

        if (attachmentList.size() > 1) {

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

                }, () -> attachment.retrieveInputStream().whenComplete((inputStream, throwable) -> {

                    if (throwable != null)
                        log.error("Error:", throwable);

                    try {
                        commandSuggestion.setCommandFile(inputStream.readAllBytes());
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (commandSuggestion.getCommandFile() != null) {

                        if (override) {

                            commandSuggestion.setActive(true);
                            commandRepository.save(commandSuggestion);

                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(Color.green);
                            eb.setDescription("Command: " + commandSuggestion.getCommandName());
                            eb.setTitle("Voting Closed - Command Suggestion Approved", null);
                            eb.setAuthor(author.getName(), null, author.getAvatarUrl());
                            eb.setFooter("Override");

                            if (!commandSuggestion.getCommandFileExtension().equalsIgnoreCase("mp3"))
                                eb.setImage("attachment://" + commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension());

                            event.getChannel().sendFile(commandSuggestion.getCommandFile(), commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension()).embed(eb.build()).queue();

                        } else {

                            EmbedBuilder eb = new EmbedBuilder();

                            eb.setColor(Color.yellow);
                            eb.setDescription("Create Command: " + commandSuggestion.getCommandName());
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

                        }

                    } else {

                        log.info("File was empty");

                    }

                    message.delete().queue();

                }));

            }

        }

    }

    private void updateCommand(MessageReceivedEvent event, Message message, String content, Boolean override) {

        MessageChannel channel = event.getChannel();
        User author = event.getAuthor();

        List<String> contentList = Arrays.asList(content.split(" "));
        List<Message.Attachment> attachmentList = message.getAttachments();

        if (attachmentList.size() > 1) {

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

                String commandSuggestionName = contentList.get(1);

                Command commandSuggestion = new Command();
                commandSuggestion.setCommandName("~update~_" + commandSuggestionName);
                commandSuggestion.setCommandFileExtension(fileExtension);
                commandSuggestion.setActive(false);

                commandRepository.findById(commandSuggestionName).ifPresentOrElse(existingCommand -> {

                    if (existingCommand.getActive()) {

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

                                if (override) {

                                    commandSuggestion.setCommandName(commandSuggestionName);
                                    commandSuggestion.setActive(true);
                                    commandRepository.delete(existingCommand);
                                    commandRepository.save(commandSuggestion);

                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setColor(Color.green);
                                    eb.setDescription("Command: " + commandSuggestionName);
                                    eb.setTitle("Voting Closed - Command Update Approved", null);
                                    eb.setAuthor(author.getName(), null, author.getAvatarUrl());
                                    eb.setFooter("Override");

                                    if (!commandSuggestion.getCommandFileExtension().equalsIgnoreCase("mp3"))
                                        eb.setImage("attachment://" + commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension());

                                    event.getChannel().sendFile(commandSuggestion.getCommandFile(), commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension()).embed(eb.build()).queue();

                                } else {

                                    EmbedBuilder eb = new EmbedBuilder();

                                    eb.setColor(Color.yellow);
                                    eb.setDescription("Update Command: " + commandSuggestionName);
                                    eb.setAuthor(author.getName(), null, author.getAvatarUrl());

                                    if (commandSuggestion.getCommandFileExtension().equalsIgnoreCase("mp3")) {
                                        eb.setTitle("Update The Existing Voice Command to This?", null);
                                    } else {
                                        eb.setTitle("Update This Existing Emote to This?", null);
                                        eb.setImage("attachment://" + commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension());
                                    }

                                    channel.sendFile(commandSuggestion.getCommandFile(), commandSuggestion.getCommandName() + "." + commandSuggestion.getCommandFileExtension()).embed(eb.build()).queue(sentMessage -> {
                                        sentMessage.addReaction(reactionNoVote).queue();
                                        sentMessage.addReaction(reactionYesVote).queue();
                                        commandRepository.save(commandSuggestion);
                                    });

                                }

                            } else {

                                log.info("File was empty");

                            }

                            message.delete().queue();

                        });

                    } else {
                        message.delete().queue();
                        channel.sendMessage("The `" + existingCommand.getCommandName() + "` command is still being voted on.").queue(sentMessage -> {
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                sentMessage.delete().queue();
                            });
                        });
                    }

                }, () -> {
                    message.delete().queue();
                    channel.sendMessage("The `" + commandSuggestionName + "` command doesn't exist.").queue(sentMessage -> {
                        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                            sentMessage.delete().queue();
                        });
                    });
                });

            }

        }

    }

    private void deleteCommand(MessageReceivedEvent event, Message message, String content, Boolean override) {

        MessageChannel channel = event.getChannel();
        User author = event.getAuthor();

        List<String> contentList = Arrays.asList(content.split(" "));
        String commandName = contentList.get(1);

        commandRepository.findById(commandName).ifPresentOrElse(command -> {

            if (command.getActive()) {

                if (command.getCommandFile() != null) {

                    if (override) {
                        commandRepository.delete(command);
                        channel.sendMessage("The `" + command.getCommandName() + "` command has been deleted.").queue(sentMessage -> {
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                                sentMessage.delete().queue();
                            });
                        });
                    } else {

                        EmbedBuilder eb = new EmbedBuilder();

                        eb.setColor(Color.yellow);
                        eb.setDescription("Delete Command: " + command.getCommandName());
                        eb.setAuthor(author.getName(), null, author.getAvatarUrl());

                        if (command.getCommandFileExtension().equalsIgnoreCase("mp3")) {
                            eb.setTitle("Delete This Existing Voice Command?", null);
                        } else {
                            eb.setTitle("Delete This Existing Emote?", null);
                            eb.setImage("attachment://" + command.getCommandName() + "." + command.getCommandFileExtension());
                        }

                        channel.sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).embed(eb.build()).queue(sentMessage -> {
                            sentMessage.addReaction(reactionNoVote).queue();
                            sentMessage.addReaction(reactionYesVote).queue();
                        });

                    }

                } else {

                    log.info("File was empty");

                }

                message.delete().queue();

            } else {

                channel.sendMessage("The `" + command.getCommandName() + "` command is still being voted on.").queue(sentMessage -> {
                    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                        sentMessage.delete().queue();
                    });
                });

            }

        }, () -> {

            channel.sendMessage("The `" + commandName + "` command doesn't exist.").queue(sentMessage -> {
                CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                    sentMessage.delete().queue();
                });
            });

        });

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

            String commandAction = messageEmbed.getDescription().split(" ")[0];
            String commandName = messageEmbed.getDescription().split(": ")[1];
            String searchCommandName = "";
            Boolean isUpdateCommand = false;
            Boolean isDeleteCommand = false;

            if (commandAction.equalsIgnoreCase("update")) {
                isUpdateCommand = true;
                searchCommandName = "~update~_" + commandName;
            } else if (commandAction.equalsIgnoreCase("delete")) {
                isDeleteCommand = true;
            } else {
                searchCommandName = commandName;
            }

            String finalSearchCommandName = searchCommandName;
            Boolean finalIsUpdateCommand = isUpdateCommand;
            Boolean finalIsDeleteCommand = isDeleteCommand;

            commandRepository.findById(searchCommandName).ifPresent(command -> {

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

                if (noVotesCount >= 3 || yesVotesCount >= 3) {

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
                        if (finalIsUpdateCommand) {
                            responseMessage = "Command Update Approved";
                            command.setCommandName(command.getCommandName().replace("~update~_", ""));
                        } else if (finalIsDeleteCommand) {
                            responseMessage = "Command Delete Approved";
                            eb.setColor(Color.red);
                        }
                    }

                    eb.setTitle("Voting Closed - " + responseMessage, null);

                    eb.setAuthor(messageEmbed.getAuthor().getName(), null, messageEmbed.getAuthor().getIconUrl());
                    eb.setFooter(noVotesCount + " No Votes / " + yesVotesCount + " Yes Votes");

                    if (!command.getCommandFileExtension().equalsIgnoreCase("mp3"))
                        eb.setImage("attachment://" + command.getCommandName() + "." + command.getCommandFileExtension());

                    event.getChannel().sendFile(command.getCommandFile(), command.getCommandName() + "." + command.getCommandFileExtension()).embed(eb.build()).queue();

                    message.delete().queue();

                    if (approved && !finalIsDeleteCommand) {
                        if (finalIsUpdateCommand) {
                            commandRepository.findById(commandName).ifPresent(oldCommand -> {
                                commandRepository.delete(oldCommand);
                            });
                        }
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

    public void startSeason2(MessageReceivedEvent event) {

        log.info("Starting Season 2");

        var guild = event.getGuild();
        var commandTimestamp = new Timestamp(System.currentTimeMillis());

        // Check to make sure that the current Season is Season 1
        var currentSeason = seasonInfoRepository.findById("current_season").stream().findFirst().get();
        if (!currentSeason.getValue().equalsIgnoreCase("season_1")) {
            log.info("Can't start Season 2 if it is not Season 1");
        } else {

            // Create Legacy Roles and Assign to Guild Members
            List<SeasonRole> seasonRoleList = seasonRolesRepository.findAllBySeason("season_1").stream().sorted(Comparator.comparingInt(SeasonRole::getRoleOrder)).collect(Collectors.toList());

            List<Role> guildRoles = null;
            seasonRoleList.forEach(seasonRole -> {
                var guildRole = guild.getRolesByName(seasonRole.getRoleName(), false).stream().findFirst();
                if (guildRole.isPresent())
                    guildRoles.add(guildRole.get());
            });

            guildRoles.forEach(role -> {
                var roleName = role.getName();
                var roleColor = role.getColor();
                var roleId = role.getIdLong();

                AtomicReference<Role> legacyRole = null;

                log.info("Season 1 Role Name: {}, Role Color: {}, Role Id: {}", roleName, roleColor, roleId);
                guild.createRole().setName(String.format("S1_%s", roleName)).setColor(roleColor).queue(createdRole -> {
                    legacyRole.set(createdRole);
                    log.info("Created Legacy Role: {}", legacyRole.get().getName());
                });

                guild.getMembersWithRoles(role).forEach(member -> {
                    log.info("Setting Legacy Role {} to Member {}", legacyRole.get().getName(), member.getEffectiveName());
                    guild.addRoleToMember(member.getIdLong(), legacyRole.get()).queue();
                });
            });

            // Change to Season 2
            currentSeason.setValue("season_2");
            seasonInfoRepository.save(currentSeason);

            // Archive all Season 1 User Rank Details
            userInfoRepository.findAll().forEach(userInfo -> {

                SeasonArchive newArchive = new SeasonArchive();
                newArchive.setSeason("season_1");
                newArchive.setUserId(userInfo.getUserId());
                newArchive.setUserName(userInfo.getUserName());
                newArchive.setRank(userInfo.getRank());

                seasonArchiveRepository.save(newArchive);

            });

            // Reset USER_INFO Details
            List<UserInfo> updatedUserInfoList = null;

            userInfoRepository.findAll().forEach(userInfo -> {

                var bronzeRankValue = seasonRolesRepository.findAllBySeason("season_2").stream().filter(s -> s.getRoleName().equalsIgnoreCase("Bronze")).findFirst().get().getRoleValue();
                var silverRankValue = seasonRolesRepository.findAllBySeason("season_2").stream().filter(s -> s.getRoleName().equalsIgnoreCase("Silver")).findFirst().get().getRoleValue();

                // Start Users out in Silver if at least Silver in Previous Season
                if (userInfo.getRank() >= silverRankValue) {
                    userInfo.setRank(silverRankValue);
                } else {
                    userInfo.setRank(bronzeRankValue);
                }

                // Reset the User Joined Timestamp
                if (userInfo.getActive()) {
                    // Set Timestamp to Current Time and Leave Active
                    userInfo.setJoinedChannelTm(commandTimestamp);
                } else {
                    // Set Timestamp to Null and Leave In-Active
                    userInfo.setJoinedChannelTm(null);
                }

                updatedUserInfoList.add(userInfo);

            });

            userInfoRepository.saveAll(updatedUserInfoList);

            // Send Season 2 Message to General Channel
            var season2Message = "```\n" +
                    "Season 2 is here!\n" +
                    "With Season 2 we are resetting the Ranks and rebalancing them based on the Season 1 data.\n" +
                    "\n" +
                    "What Rank do we start at?\n" +
                    " - If you made it to Silver last Season then you will start as Silver in Season 2.\n" +
                    " - If you didn't get to Silver then you will start back as Bronze.\n" +
                    "\n" +
                    "Season 2 Updated Ranks:\n" +
                    " - Bronze = 0\n" +
                    " - Silver = 3\n" +
                    " - Gold = 30\n" +
                    " - Platinum = 100\n" +
                    " - Diamond = 750\n" +
                    " - Master = 2250\n" +
                    " - GrandMaster = 6000\n" +
                    "\n" +
                    "Why are the lower Ranks so low?\n" +
                    " - Looking at the data from Season 1, we had a lot of people that never got out of Bronze." +
                    " We want to make these lower Ranks easier to achieve and while also increasing the difficulty a bit for the higher Ranks." +
                    " These changes should help to have a more distributed base per Rank.\n" +
                    "\n" +
                    "What happens with my Season 1 Rank?\n" +
                    " - Season 1 is over and the Rank that you achieved is important. We have created Legacy Season Roles! Take a look at your profile to see yours.\n" +
                    "```";


            guild.getDefaultChannel().sendMessage(season2Message).queue(s -> log.info("Season 2 Message Sent to Guild Default Channel"));

        }

    }

}