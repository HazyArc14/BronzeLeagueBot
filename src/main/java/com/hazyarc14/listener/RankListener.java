package com.hazyarc14.listener;

import com.hazyarc14.model.UserLog;
import com.hazyarc14.model.UserRank;
import com.hazyarc14.repository.UserLogRepository;
import com.hazyarc14.repository.UserRanksRepository;
import com.hazyarc14.service.UserRankService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RankListener extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(RankListener.class);

    @Autowired
    UserRanksRepository userRanksRepository;

    @Autowired
    UserLogRepository userLogRepository;

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

        Guild guild = event.getGuild();
        Member eventMember = event.getMember();

        if (eventMember.getUser().isBot()) return;
        if (event.getChannelJoined() == guild.getAfkChannel()) return;
        Timestamp userJoinedTmstp = new Timestamp(System.currentTimeMillis());

        Optional<List<Member>> membersInChannel = Optional.ofNullable(event.getChannelJoined().getMembers());
        membersInChannel.ifPresent(members -> {

            List<Member> memberListWithoutBots = new ArrayList<>();
            for (Member member : members) {
                if (!member.getUser().isBot())
                    memberListWithoutBots.add(member);
            }

            if (memberListWithoutBots.size() == 2) {

                //start counting for both individuals
                memberListWithoutBots.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setJoinedChannelTm(userJoinedTmstp);
                        user.setActive(true);

                        UserLog userLog = new UserLog();
                        userLog.setUserId(user.getUserId());
                        userLog.setUserName(user.getUserName());
                        userLog.setMethodCall("onGuildVoiceJoin - 1");
                        userLog.setOldRank(user.getRank());
                        userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                        userLog.setActive(user.getActive());

                        userRanksRepository.save(user);
                        userLogRepository.save(userLog);

                    });

                });

            } else if (memberListWithoutBots.size() > 2) {

                //start counting for only the joined user
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    user.setJoinedChannelTm(userJoinedTmstp);
                    user.setActive(true);

                    UserLog userLog = new UserLog();
                    userLog.setUserId(user.getUserId());
                    userLog.setUserName(user.getUserName());
                    userLog.setMethodCall("onGuildVoiceJoin - 1");
                    userLog.setOldRank(user.getRank());
                    userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                    userLog.setActive(user.getActive());

                    userRanksRepository.save(user);
                    userLogRepository.save(userLog);

                });

            }

        });

    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {

        Guild guild = event.getGuild();
        Member eventMember = event.getMember();

        if (eventMember.getUser().isBot()) return;
        if (event.getChannelLeft() == guild.getAfkChannel()) return;

        Optional<List<Member>> membersInChannel = Optional.ofNullable(event.getChannelLeft().getMembers());
        membersInChannel.ifPresent(members -> {

            List<Member> memberListWithoutBots = new ArrayList<>();
            for (Member member : members) {
                if (!member.getUser().isBot())
                    memberListWithoutBots.add(member);
            }

            if (memberListWithoutBots.size() == 1) {

                //update the leave tmstp for the remaining 1 member
                memberListWithoutBots.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setActive(false);

                        UserLog userLog = new UserLog();
                        userLog.setUserId(user.getUserId());
                        userLog.setUserName(user.getUserName());
                        userLog.setMethodCall("onGuildVoiceLeave - 1");
                        userLog.setOldRank(user.getRank());
                        userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                        userLog.setActive(user.getActive());

                        userRankService.calculateUserRank(guild, member, user, userLog);

                    });

                });

                //update the leave tmstp for the member that left
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    user.setActive(false);

                    UserLog userLog = new UserLog();
                    userLog.setUserId(user.getUserId());
                    userLog.setUserName(user.getUserName());
                    userLog.setMethodCall("onGuildVoiceLeave - 2");
                    userLog.setOldRank(user.getRank());
                    userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                    userLog.setActive(user.getActive());

                    userRankService.calculateUserRank(guild, eventMember, user, userLog);

                });

            } else if (memberListWithoutBots.size() > 1) {

                //update the leave tmstp for only the member that left
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    user.setActive(false);

                    UserLog userLog = new UserLog();
                    userLog.setUserId(user.getUserId());
                    userLog.setUserName(user.getUserName());
                    userLog.setMethodCall("onGuildVoiceLeave - 3");
                    userLog.setOldRank(user.getRank());
                    userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                    userLog.setActive(user.getActive());

                    userRankService.calculateUserRank(guild, eventMember, user, userLog);

                });

            }

        });

    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {

        Guild guild = event.getGuild();
        Member eventMember = event.getMember();

        if (eventMember.getUser().isBot()) return;
        Timestamp userMoveTmstp = new Timestamp(System.currentTimeMillis());

        Optional<UserRank> eventMemberUserRank = userRanksRepository.findById(eventMember.getIdLong());

        // If moved to AFK channel
        if (event.getChannelJoined() == guild.getAfkChannel()) {

            if (eventMemberUserRank.get().getActive())
                eventMemberUserRank.ifPresent(user -> {

                    user.setActive(false);

                    UserLog userLog = new UserLog();
                    userLog.setUserId(user.getUserId());
                    userLog.setUserName(user.getUserName());
                    userLog.setMethodCall("onGuildVoiceMove - 1");
                    userLog.setOldRank(user.getRank());
                    userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                    userLog.setActive(user.getActive());

                    userRankService.calculateUserRank(guild, eventMember, user, userLog);

                });

        }

        //Check the Left Channel to Update Users
        Optional<List<Member>> membersInLeftChannel = Optional.ofNullable(event.getChannelLeft().getMembers());
        membersInLeftChannel.ifPresent(members -> {

            if (event.getChannelLeft() == guild.getAfkChannel()) return;

            List<Member> memberListWithoutBots = new ArrayList<>();
            for (Member member : members) {
                if (!member.getUser().isBot())
                    memberListWithoutBots.add(member);
            }

            if (memberListWithoutBots.size() == 1) {

                //update the leave tmstp for the remaining 1 member
                memberListWithoutBots.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setActive(false);

                        UserLog userLog = new UserLog();
                        userLog.setUserId(user.getUserId());
                        userLog.setUserName(user.getUserName());
                        userLog.setMethodCall("onGuildVoiceMove - 2");
                        userLog.setOldRank(user.getRank());
                        userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                        userLog.setActive(user.getActive());

                        userRankService.calculateUserRank(guild, member, user, userLog);

                    });

                });

            }

        });

        //Check the Joined Channel to Update Users
        Optional<List<Member>> membersInJoinedChannel = Optional.ofNullable(event.getChannelJoined().getMembers());
        membersInJoinedChannel.ifPresent(members -> {

            if (event.getChannelJoined() == guild.getAfkChannel()) return;

            List<Member> memberListWithoutBots = new ArrayList<>();
            for (Member member : members) {
                if (!member.getUser().isBot())
                    memberListWithoutBots.add(member);
            }

            if (memberListWithoutBots.size() == 1) {

                if (eventMemberUserRank.get().getActive()) {

                    eventMemberUserRank.ifPresent(user -> {

                        user.setActive(false);

                        UserLog userLog = new UserLog();
                        userLog.setUserId(user.getUserId());
                        userLog.setUserName(user.getUserName());
                        userLog.setMethodCall("onGuildVoiceMove - 3");
                        userLog.setOldRank(user.getRank());
                        userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                        userLog.setActive(user.getActive());

                        userRankService.calculateUserRank(guild, eventMember, user, userLog);

                    });

                }

            } else if (memberListWithoutBots.size() == 2) {

                //start counting for both individuals
                memberListWithoutBots.forEach(member -> {

                    if (eventMemberUserRank.get().getActive()) {

                        if (member.getIdLong() != eventMember.getIdLong()) {

                            Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                            userRank.ifPresent(user -> {

                                user.setJoinedChannelTm(userMoveTmstp);
                                user.setActive(true);

                                UserLog userLog = new UserLog();
                                userLog.setUserId(user.getUserId());
                                userLog.setUserName(user.getUserName());
                                userLog.setMethodCall("onGuildVoiceMove - 4");
                                userLog.setOldRank(user.getRank());
                                userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                                userLog.setActive(user.getActive());

                                userRanksRepository.save(user);
                                userLogRepository.save(userLog);

                            });

                        }

                    } else {

                        Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                        userRank.ifPresent(user -> {

                            user.setJoinedChannelTm(userMoveTmstp);
                            user.setActive(true);

                            UserLog userLog = new UserLog();
                            userLog.setUserId(user.getUserId());
                            userLog.setUserName(user.getUserName());
                            userLog.setMethodCall("onGuildVoiceMove - 5");
                            userLog.setOldRank(user.getRank());
                            userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                            userLog.setActive(user.getActive());

                            userRanksRepository.save(user);
                            userLogRepository.save(userLog);

                        });

                    }

                });

            } else if (memberListWithoutBots.size() > 2) {

                //start counting for only the joined user
                eventMemberUserRank.ifPresent(user -> {

                    user.setJoinedChannelTm(userMoveTmstp);
                    user.setActive(true);

                    UserLog userLog = new UserLog();
                    userLog.setUserId(user.getUserId());
                    userLog.setUserName(user.getUserName());
                    userLog.setMethodCall("onGuildVoiceMove - 6");
                    userLog.setOldRank(user.getRank());
                    userLog.setUpdateTm(new Timestamp(System.currentTimeMillis()));
                    userLog.setActive(user.getActive());

                    userRanksRepository.save(user);
                    userLogRepository.save(userLog);

                });

            }

        });

    }

}
