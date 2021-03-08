package com.hazyarc14.service;

import com.hazyarc14.model.SeasonRole;
import com.hazyarc14.model.UserInfo;
import com.hazyarc14.repository.SeasonInfoRepository;
import com.hazyarc14.repository.SeasonRolesRepository;
import com.hazyarc14.repository.UserInfoRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserRankService {

    public static final Logger log = LoggerFactory.getLogger(UserRankService.class);

    public static final Double MINRANK = 0.0;
    public static final Double MAXRANK = 9999.9;

    public static final Double minsPerPointEarned = 10.0;
    public static final Double serverBoosterBonus = 1.10;

    private static final List<String> guildRoleNames = Arrays.asList(
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Master",
            "GrandMaster");

    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    SeasonInfoRepository seasonInfoRepository;

    @Autowired
    SeasonRolesRepository seasonRolesRepository;

    public void createUserRank(Member member) {

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(member.getIdLong());
        userInfo.setUserName(member.getEffectiveName());
        userInfo.setRank(0.0);
        userInfo.setActive(false);

        userInfoRepository.save(userInfo);

    }

    public UserInfo calculateUserRank(Guild guild, Member member, UserInfo userInfo) {

        Double currentRank = userInfo.getRank();

        Timestamp joinedTm = userInfo.getJoinedChannelTm();
        Timestamp requestTmstp = new Timestamp(System.currentTimeMillis());

        Long minutesInChannel = TimeUnit.MINUTES.convert(requestTmstp.getTime() - joinedTm.getTime(), TimeUnit.MILLISECONDS);
        Double pointsToAdd = Math.floor(minutesInChannel.doubleValue() / minsPerPointEarned);
        Double remainder = Math.abs(minutesInChannel.doubleValue() - pointsToAdd * minsPerPointEarned);

        if (userInfo.getActive()) {
            Long remainderMilliseconds = TimeUnit.MILLISECONDS.convert(remainder.longValue(), TimeUnit.MINUTES);
            Timestamp newJoinedTm = new Timestamp(requestTmstp.getTime() - remainderMilliseconds);
            userInfo.setJoinedChannelTm(newJoinedTm);
        }

        List<Member> serverBoosters = guild.getBoosters();
        for (Member serverBooster: serverBoosters) {
            if (serverBooster.getIdLong() == userInfo.getUserId())
                pointsToAdd *= serverBoosterBonus;
        }

        if (member != null) {

            if (member.getVoiceState().inVoiceChannel()) {
                VoiceChannel voiceChannel = member.getVoiceState().getChannel();
                if (!voiceChannel.getMembers().isEmpty()) {
                    Integer membersInChannelCount = voiceChannel.getMembers().size();

                    if (membersInChannelCount >= 8) {
                        pointsToAdd *= 2.0;
                    }
                }
            }

        }

        Double updatedRank = currentRank + pointsToAdd;
        if (updatedRank > MAXRANK)
            updatedRank = MAXRANK;
        if (updatedRank < MINRANK)
            updatedRank = MINRANK;

        userInfo.setRank(updatedRank);

        userInfoRepository.save(userInfo);
        updateRolesByUser(guild, member, userInfo);

        return userInfo;

    }

    public void updateRolesByUser(Guild guild, Member member, UserInfo userInfo) {

        List<Role> memberRoles = member.getRoles();
        List<Role> newMemberRoles = new ArrayList<>();

        SeasonRole currentUserRank = calculateRoleByRank(userInfo.getRank());
        List<Role> roles = guild.getRolesByName(currentUserRank.getRoleName(), false);

        if (!roles.isEmpty()) {

            Role newGuildRole = roles.get(0);

            for (int i = 0; i < memberRoles.size(); i++) {
                if (!guildRoleNames.contains(memberRoles.get(i).getName()))
                    newMemberRoles.add(memberRoles.get(i));
            }

            newMemberRoles.add(newGuildRole);
            guild.modifyMemberRoles(member, newMemberRoles).queue();

        }

    }

    public void updateAllUserRoles(Guild guild) {

        List<UserInfo> userInfoList = userInfoRepository.findAll();
        userInfoList.forEach(userRank -> {

            Member member;
            try {
                member = guild.retrieveMemberById(userRank.getUserId()).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorCode() == 10007)
                    userInfoRepository.delete(userRank);
                return;
            }

            List<Role> memberRoles = member.getRoles();
            List<Role> newMemberRoles = new ArrayList<>();

            SeasonRole currentUserRank = calculateRoleByRank(userRank.getRank());
            Role newGuildRole = guild.getRolesByName(currentUserRank.getRoleName(), false).get(0);

            for (int i = 0; i < memberRoles.size(); i++) {
                if (!guildRoleNames.contains(memberRoles.get(i).getName()))
                    newMemberRoles.add(memberRoles.get(i));
            }

            newMemberRoles.add(newGuildRole);
            guild.modifyMemberRoles(member, newMemberRoles).queue();

        });

    }

    public void updateAllUserRanks(JDA jda) {

        Guild guild = jda.getGuildById(93106003628806144L);
        TextChannel defaultChannel = guild.getDefaultChannel();

        List<UserInfo> userInfoList = userInfoRepository.findAll();

        if (!userInfoList.isEmpty()) {

            userInfoList.forEach(userRank -> {
                if (userRank.getActive()) {

                    guild.retrieveMemberById(userRank.getUserId()).queue(member -> {
                        UserInfo updatedUserInfo = calculateUserRank(guild, member, userRank);
                    });

                }
            });

        }

    }

    public SeasonRole calculateRoleByRank(Double rank) {

        var currentSeason = seasonInfoRepository.findById("current_season").stream().findFirst().get().getValue();
        var seasonRoles = seasonRolesRepository.findAllBySeason(currentSeason);

        log.info("Season Roles Size: {}", seasonRoles.size());

        SeasonRole role = null;

        SeasonRole bronze = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Bronze")).findFirst().get();
        SeasonRole silver = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Silver")).findFirst().get();
        SeasonRole gold = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Gold")).findFirst().get();
        SeasonRole platinum = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Platinum")).findFirst().get();
        SeasonRole diamond = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Diamond")).findFirst().get();
        SeasonRole master = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("Master")).findFirst().get();
        SeasonRole grandMaster = seasonRoles.stream().filter(s -> s.getRoleName().equalsIgnoreCase("GrandMaster")).findFirst().get();

        if (rank < silver.getRoleValue()) {
            role = bronze;
        } else if (rank >= silver.getRoleValue() && rank < gold.getRoleValue()) {
            role = silver;
        } else if (rank >= gold.getRoleValue() && rank < platinum.getRoleValue()) {
            role = gold;
        } else if (rank >= platinum.getRoleValue() && rank < diamond.getRoleValue()) {
            role = platinum;
        } else if (rank >= diamond.getRoleValue() && rank < master.getRoleValue()) {
            role = diamond;
        } else if (rank >= master.getRoleValue() && rank < grandMaster.getRoleValue()) {
            role = master;
        } else if (rank >= grandMaster.getRoleValue()) {
            role = grandMaster;
        }

        return role;

    }

    public SeasonRole nextSeasonRole(SeasonRole seasonRole) {
        return seasonRolesRepository.findAllBySeasonAndRoleOrder(seasonRole.getSeason(), seasonRole.getRoleOrder() + 1).stream().findFirst().get();
    }

}