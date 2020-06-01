package com.hazyarc14.listener;

import com.hazyarc14.model.UserRank;
import com.hazyarc14.repository.UserRanksRepository;
import com.hazyarc14.service.UserRankService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class RankListener extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(RankListener.class);

    @Autowired
    UserRanksRepository userRanksRepository;

    @Autowired
    UserRankService userRankService;

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        if (event.getMember().getUser().isBot()) return;

        Guild guild = event.getGuild();
        Member joinedMember = event.getMember();
        List<Role> newMemberRoles = guild.getRolesByName("Bronze", true);

        guild.modifyMemberRoles(joinedMember, newMemberRoles).queue();
        userRankService.createUserRank(joinedMember);

    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {

        if (event.getMember().getUser().isBot()) return;

        Guild guild = event.getGuild();
        Member eventMember = event.getMember();

        String userNickname = eventMember.getNickname();
        Long userId = eventMember.getIdLong();
        Timestamp userJoinedTmstp = new Timestamp(System.currentTimeMillis());

        Optional<List<Member>> membersInChannel = Optional.ofNullable(event.getChannelJoined().getMembers());

        membersInChannel.ifPresent(members -> {

            if (members.size() == 1) {

                //don't count anything

            } else if (members.size() == 2) {

                //start counting for both individuals
                members.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setJoinedChannelTm(userJoinedTmstp);
                        user.setLeftChannelTm(null);
                        userRanksRepository.save(user);

                    });

                });

            } else {

                //start counting for only the joined user
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    user.setJoinedChannelTm(userJoinedTmstp);
                    user.setLeftChannelTm(null);
                    userRanksRepository.save(user);

                });

            }

        });

    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {

        if (event.getMember().getUser().isBot()) return;

        Member eventMember = event.getMember();
        Timestamp userLeaveTmstp = new Timestamp(System.currentTimeMillis());

        Optional<List<Member>> membersInChannel = Optional.ofNullable(event.getChannelLeft().getMembers());
        membersInChannel.ifPresent(members -> {

            if (members.size() == 0) {

                //don't count anything

            } else if (members.size() == 1) {

                //update the leave tmstp for the remaining 1 member
                members.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        UserRank previousUserRank = new UserRank(user);
                        user.setLeftChannelTm(userLeaveTmstp);
                        user = userRankService.calculateUserRank(user);
                        userRankService.updateUserRoleByRank(event.getGuild(), member, previousUserRank, user);

                    });

                });

                //update the leave tmstp for the member that left
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    UserRank previousUserRank = new UserRank(user);
                    user.setLeftChannelTm(userLeaveTmstp);
                    user = userRankService.calculateUserRank(user);
                    userRankService.updateUserRoleByRank(event.getGuild(), eventMember, previousUserRank, user);

                });

            } else {

                //update the leave tmstp for only the member that left
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    UserRank previousUserRank = new UserRank(user);
                    user.setLeftChannelTm(userLeaveTmstp);
                    user = userRankService.calculateUserRank(user);
                    userRankService.updateUserRoleByRank(event.getGuild(), eventMember, previousUserRank, user);

                });

            }

        });

    }

}
