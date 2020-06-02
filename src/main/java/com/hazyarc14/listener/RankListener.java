package com.hazyarc14.listener;

import com.hazyarc14.model.UserRank;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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

        Guild guild = event.getGuild();
        Member eventMember = event.getMember();

        if (eventMember.getUser().isBot()) return;
        if (event.getChannelJoined() == guild.getAfkChannel()) return;
        Timestamp userJoinedTmstp = new Timestamp(System.currentTimeMillis());

        Optional<List<Member>> membersInChannel = Optional.ofNullable(event.getChannelJoined().getMembers());
        membersInChannel.ifPresent(members -> {

            Stream<Member> membersWithoutBot = members.stream().filter(member -> !member.getUser().isBot());

            if (membersWithoutBot.count() == 2) {

                //start counting for both individuals
                membersWithoutBot.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setJoinedChannelTm(userJoinedTmstp);
                        user.setLeftChannelTm(null);
                        userRanksRepository.save(user);

                    });

                });

            } else if (membersWithoutBot.count() > 2) {

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

        Guild guild = event.getGuild();
        Member eventMember = event.getMember();

        if (eventMember.getUser().isBot()) return;
        if (event.getChannelLeft() == guild.getAfkChannel()) return;
        Timestamp userLeaveTmstp = new Timestamp(System.currentTimeMillis());

        Optional<List<Member>> membersInChannel = Optional.ofNullable(event.getChannelLeft().getMembers());
        membersInChannel.ifPresent(members -> {

            Stream<Member> membersWithoutBot = members.stream().filter(member -> !member.getUser().isBot());

            if (membersWithoutBot.count() == 1) {

                //update the leave tmstp for the remaining 1 member
                membersWithoutBot.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setLeftChannelTm(userLeaveTmstp);
                        user = userRankService.calculateUserRank(guild, member, user);
                        userRankService.updateRolesByUser(guild, member, user);

                    });

                });

                //update the leave tmstp for the member that left
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    user.setLeftChannelTm(userLeaveTmstp);
                    user = userRankService.calculateUserRank(guild, eventMember, user);
                    userRankService.updateRolesByUser(guild, eventMember, user);

                });

            } else if (membersWithoutBot.count() > 1) {

                //update the leave tmstp for only the member that left
                Optional<UserRank> userRank = userRanksRepository.findById(eventMember.getIdLong());
                userRank.ifPresent(user -> {

                    user.setLeftChannelTm(userLeaveTmstp);
                    user = userRankService.calculateUserRank(guild, eventMember, user);
                    userRankService.updateRolesByUser(guild, eventMember, user);

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

        AtomicReference<Boolean> userRankProgressing = new AtomicReference<>(false);
        Optional<UserRank> eventMemberUserRank = userRanksRepository.findById(eventMember.getIdLong());

        eventMemberUserRank.ifPresent(user -> {
            if (user.getLeftChannelTm() == null)
                userRankProgressing.set(true);
        });

        // If moved to AFK channel
        if (event.getChannelJoined() == guild.getAfkChannel()) {

            if (userRankProgressing.get())
                eventMemberUserRank.ifPresent(user -> {

                    user.setLeftChannelTm(userMoveTmstp);
                    user = userRankService.calculateUserRank(guild, eventMember, user);
                    userRankService.updateRolesByUser(guild, eventMember, user);

                });

        } else if (event.getChannelLeft() == guild.getAfkChannel()) {

            eventMemberUserRank.ifPresent(user -> {

                user.setJoinedChannelTm(userMoveTmstp);
                user.setLeftChannelTm(null);
                userRanksRepository.save(user);

            });

        }

        //Check the Left Channel to Update Users
        Optional<List<Member>> membersInLeftChannel = Optional.ofNullable(event.getChannelLeft().getMembers());
        membersInLeftChannel.ifPresent(members -> {

            Stream<Member> membersWithoutBot = members.stream().filter(member -> !member.getUser().isBot());

            if (membersWithoutBot.count() == 1) {

                //update the leave tmstp for the remaining 1 member
                membersWithoutBot.forEach(member -> {

                    Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                    userRank.ifPresent(user -> {

                        user.setLeftChannelTm(userMoveTmstp);
                        user = userRankService.calculateUserRank(guild, member, user);
                        userRankService.updateRolesByUser(guild, member, user);

                    });

                });

            }

        });

        //Check the Joined Channel to Update Users
        Optional<List<Member>> membersInJoinedChannel = Optional.ofNullable(event.getChannelJoined().getMembers());
        membersInJoinedChannel.ifPresent(members -> {

            Stream<Member> membersWithoutBot = members.stream().filter(member -> !member.getUser().isBot());

            if (membersWithoutBot.count() == 2) {

                //start counting for both individuals
                membersWithoutBot.forEach(member -> {

                    if (userRankProgressing.get()) {

                        if (member.getIdLong() != eventMember.getIdLong()) {

                            Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                            userRank.ifPresent(user -> {

                                user.setJoinedChannelTm(userMoveTmstp);
                                user.setLeftChannelTm(null);
                                userRanksRepository.save(user);

                            });

                        }

                    } else {

                        Optional<UserRank> userRank = userRanksRepository.findById(member.getIdLong());
                        userRank.ifPresent(user -> {

                            user.setJoinedChannelTm(userMoveTmstp);
                            user.setLeftChannelTm(null);
                            userRanksRepository.save(user);

                        });

                    }

                });

            } else if (membersWithoutBot.count() > 2) {

                //start counting for only the joined user
                eventMemberUserRank.ifPresent(user -> {

                    user.setJoinedChannelTm(userMoveTmstp);
                    user.setLeftChannelTm(null);
                    userRanksRepository.save(user);

                });

            }

        });

    }

}
