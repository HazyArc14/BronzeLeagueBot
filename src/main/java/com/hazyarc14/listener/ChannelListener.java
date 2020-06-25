package com.hazyarc14.listener;

import com.hazyarc14.model.EventLog;
import com.hazyarc14.model.UserInfo;
import com.hazyarc14.repository.EventLogRepository;
import com.hazyarc14.repository.UserInfoRepository;
import com.hazyarc14.service.UserRankService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChannelListener extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    EventLogRepository eventLogRepository;

    @Autowired
    UserRankService userRankService;

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent guildJoinEvent) {

        if (guildJoinEvent.getMember().getUser().isBot()) return;

        Timestamp eventTimestamp = new Timestamp(System.currentTimeMillis());

        Guild guild = guildJoinEvent.getGuild();
        Member joinedMember = guildJoinEvent.getMember();
        List<Role> newMemberRoles = guild.getRolesByName("Bronze", true);

        guild.modifyMemberRoles(joinedMember, newMemberRoles).queue();
        userRankService.createUserRank(joinedMember);

        EventLog newEvent = new EventLog();
        newEvent.setTm(eventTimestamp);
        newEvent.setUserName(joinedMember.getEffectiveName());
        newEvent.setUserId(joinedMember.getIdLong());
        newEvent.setType("guildJoinEvent");
        newEvent.setMessage(joinedMember.getEffectiveName() + " Joined the Guild");
        eventLogRepository.save(newEvent);

    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent joinEvent) {

        if (joinEvent.getMember().getUser().isBot()) return;

        updateUserStatus(joinEvent, null, null);

    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent leaveEvent) {

        if (leaveEvent.getMember().getUser().isBot()) return;

        updateUserStatus(null, leaveEvent, null);

    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent moveEvent) {

        if (moveEvent.getMember().getUser().isBot()) return;

        updateUserStatus(null, null, moveEvent);

    }

    public void updateUserStatus(@Nullable GuildVoiceJoinEvent joinEvent, @Nullable GuildVoiceLeaveEvent leaveEvent, @Nullable GuildVoiceMoveEvent moveEvent) {

        Timestamp eventTimestamp = new Timestamp(System.currentTimeMillis());

        EventLog newEvent = new EventLog();
        newEvent.setTm(eventTimestamp);

        Guild guild = null;
        Member eventMember = null;

        if (joinEvent != null) {
//            log.info("joinEvent");
            guild = joinEvent.getGuild();
            eventMember = joinEvent.getMember();
            newEvent.setType("joinEvent");
            newEvent.setMessage("Joined " + joinEvent.getChannelJoined().getName());
        }
        if (leaveEvent != null) {
//            log.info("leaveEvent");
            guild = leaveEvent.getGuild();
            eventMember = leaveEvent.getMember();
            newEvent.setType("leaveEvent");
            newEvent.setMessage("Left " + leaveEvent.getChannelLeft().getName());
        }
        if (moveEvent != null) {
//            log.info("moveEvent");
            guild = moveEvent.getGuild();
            eventMember = moveEvent.getMember();
            newEvent.setType("moveEvent");
            newEvent.setMessage("Left " + moveEvent.getChannelLeft().getName() + " and Joined " + moveEvent.getChannelJoined().getName());
        }

        newEvent.setUserName(eventMember.getEffectiveName());
        newEvent.setUserId(eventMember.getIdLong());

        eventLogRepository.save(newEvent);

        List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
        VoiceChannel afkChannel = guild.getAfkChannel();

        if (!afkChannel.getMembers().isEmpty()) {

//            log.info("checking AFK channel");

            afkChannel.getMembers().forEach(member -> {

                if (member.getUser().isBot())
                    return;

                Optional<UserInfo> userInfoOptional = userInfoRepository.findById(member.getIdLong());
                if (userInfoOptional.isPresent()) {

                    UserInfo userInfo = userInfoOptional.get();
                    userInfo.setActive(false);
                    userInfoRepository.save(userInfo);

//                    log.info("updating AFK channel user");

                }

            });

        }

        voiceChannels.forEach(voiceChannel -> {

            if (voiceChannel.getIdLong() == afkChannel.getIdLong())
                return;

            List<Member> memberListWithoutBots = new ArrayList<>();
            for (Member member : voiceChannel.getMembers()) {
                if (!member.getUser().isBot())
                    memberListWithoutBots.add(member);
            }

//            log.info(voiceChannel.getName() + " memberListWithoutBots size: " + memberListWithoutBots.size());

            if (memberListWithoutBots.size() >= 2) {

                memberListWithoutBots.forEach(member -> {

                    if (member.getUser().isBot())
                        return;

                    Optional<UserInfo> userInfoOptional = userInfoRepository.findById(member.getIdLong());
                    if (userInfoOptional.isPresent()) {

                        UserInfo userInfo = userInfoOptional.get();
                        if (userInfo.getActive() == false) {

                            userInfo.setActive(true);
                            userInfo.setJoinedChannelTm(eventTimestamp);
                            userInfoRepository.save(userInfo);

//                            log.info("setting channel user to active");

                        }

                    }

                });

            } else if (memberListWithoutBots.size() == 1) {

                memberListWithoutBots.forEach(member -> {

                    if (member.getUser().isBot())
                        return;

                    Optional<UserInfo> userInfoOptional = userInfoRepository.findById(member.getIdLong());
                    if (userInfoOptional.isPresent()) {

                        UserInfo userInfo = userInfoOptional.get();
                        userInfo.setActive(false);
                        userInfoRepository.save(userInfo);

                    }

//                    log.info("setting channel user to inactive");

                });

            }

        });

        if (leaveEvent != null) {

            Optional<UserInfo> userInfoOptional = userInfoRepository.findById(eventMember.getIdLong());
            if (userInfoOptional.isPresent()) {

                UserInfo userInfo = userInfoOptional.get();
                userInfo.setActive(false);
                userRankService.calculateUserRank(guild, eventMember, userInfo);

            }

//            log.info("setting channel user to inactive");

        }

    }

}