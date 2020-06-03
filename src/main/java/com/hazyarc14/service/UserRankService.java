package com.hazyarc14.service;

import com.hazyarc14.enums.RANK;
import com.hazyarc14.model.UserRank;
import com.hazyarc14.repository.UserRanksRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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
    public static final Double MAXRANK = 2200.0;

    public static final Double minsPerPointEarned = 10.0;
    public static final Double serverBoosterBonus = 1.25;

    private static final List<String> guildRoleNames = Arrays.asList("Bronze", "Silver", "Gold", "Platinum", "Diamond", "Master", "GrandMaster");

    @Autowired
    UserRanksRepository userRanksRepository;

    public void createUserRank(Member member) {

        UserRank userRank = new UserRank();
        userRank.setUserId(member.getIdLong());
        userRank.setUserName(member.getEffectiveName());
        userRank.setRank(0.0);

        userRanksRepository.save(userRank);

    }

    public UserRank calculateUserRank(Guild guild, Member member, UserRank userRank) {

        Double currentRank = userRank.getRank();

        Timestamp joinedTm = userRank.getJoinedChannelTm();
        Timestamp requestTmstp = new Timestamp(System.currentTimeMillis());

        Long minutesInChannel = TimeUnit.MINUTES.convert(requestTmstp.getTime() - joinedTm.getTime(), TimeUnit.MILLISECONDS);
        Double pointsToAdd = Math.floor(minutesInChannel.doubleValue() / minsPerPointEarned);
        Double remainder = Math.abs(minutesInChannel.doubleValue() - pointsToAdd * minsPerPointEarned);

        if (userRank.getActive()) {
            Long remainderMilliseconds = TimeUnit.MILLISECONDS.convert(remainder.longValue(), TimeUnit.MINUTES);
            Timestamp newJoinedTm = new Timestamp(requestTmstp.getTime() - remainderMilliseconds);
            userRank.setJoinedChannelTm(newJoinedTm);
        }

        List<Member> serverBoosters = guild.getBoosters();
        for (Member serverBooster: serverBoosters) {
            if (serverBooster.getIdLong() == userRank.getUserId())
                pointsToAdd *= serverBoosterBonus;
        }

        Double updatedRank = currentRank + pointsToAdd;
        if (updatedRank > MAXRANK)
            updatedRank = MAXRANK;
        if (updatedRank < MINRANK)
            updatedRank = MINRANK;

        userRank.setRank(updatedRank);

        userRanksRepository.save(userRank);
        updateRolesByUser(guild, member, userRank);

        return userRank;

    }

    public void updateRolesByUser(Guild guild, Member member, UserRank userRank) {

        List<Role> memberRoles = member.getRoles();
        List<Role> newMemberRoles = new ArrayList<>();

        RANK currentUserRank = calculateRoleByRank(userRank.getRank());
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

        List<UserRank> userRankList = userRanksRepository.findAll();
        userRankList.forEach(userRank -> {

            Member member;
            try {
                member = guild.retrieveMemberById(userRank.getUserId()).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorCode() == 10007)
                    userRanksRepository.delete(userRank);
                return;
            }

            List<Role> memberRoles = member.getRoles();
            List<Role> newMemberRoles = new ArrayList<>();

            RANK currentUserRank = calculateRoleByRank(userRank.getRank());
            Role newGuildRole = guild.getRolesByName(currentUserRank.getRoleName(), false).get(0);

            for (int i = 0; i < memberRoles.size(); i++) {
                if (!guildRoleNames.contains(memberRoles.get(i).getName()))
                    newMemberRoles.add(memberRoles.get(i));
            }

            newMemberRoles.add(newGuildRole);
            guild.modifyMemberRoles(member, newMemberRoles).queue();

        });

    }

    public void applyDecayToUserRanks(JDA jda) {

        Guild guild = jda.getGuildsByName("Bronze League", true).get(0);

        List<UserRank> userRankList = userRanksRepository.findAll();
        Timestamp currentTm = new Timestamp(System.currentTimeMillis());

        userRankList.forEach(userRank -> {

            Double currentRank = userRank.getRank();
            Timestamp joinedTm = userRank.getJoinedChannelTm();

            if (joinedTm != null && !userRank.getActive()) {

                Long temp = TimeUnit.DAYS.convert(currentTm.getTime() - joinedTm.getTime(), TimeUnit.MILLISECONDS);
                Integer daySinceChannelJoined = temp.intValue();

                if (daySinceChannelJoined > 7) {

                    Double pointsToRemove = calculateDecayValue(currentRank);

                    userRank.setRank(currentRank - pointsToRemove);
                    userRanksRepository.save(userRank);

                }

            }

        });

        updateAllUserRoles(guild);

    }

    public Double calculateDecayValue(Double rank) {

        if (rank < RANK.SILVER.getValue())
            return 0.0;

        if (rank >= RANK.SILVER.getValue() && rank < RANK.GOLD.getValue())
            return 1.0;

        if (rank >= RANK.GOLD.getValue() && rank < RANK.PLATINUM.getValue())
            return 5.0;

        if (rank >= RANK.PLATINUM.getValue() && rank < RANK.DIAMOND.getValue())
            return 10.0;

        if (rank >= RANK.DIAMOND.getValue() && rank < RANK.MASTER.getValue())
            return 15.0;

        if (rank >= RANK.MASTER.getValue() && rank < RANK.GRANDMASTER.getValue())
            return 20.0;

        if (rank >= RANK.GRANDMASTER.getValue())
            return 30.0;

        return 0.0;

    }

    public RANK calculateRoleByRank(Double rank) {

        if (rank < RANK.SILVER.getValue())
            return RANK.BRONZE;

        if (rank >= RANK.SILVER.getValue() && rank < RANK.GOLD.getValue())
            return RANK.SILVER;

        if (rank >= RANK.GOLD.getValue() && rank < RANK.PLATINUM.getValue())
            return RANK.GOLD;

        if (rank >= RANK.PLATINUM.getValue() && rank < RANK.DIAMOND.getValue())
            return RANK.PLATINUM;

        if (rank >= RANK.DIAMOND.getValue() && rank < RANK.MASTER.getValue())
            return RANK.DIAMOND;

        if (rank >= RANK.MASTER.getValue() && rank < RANK.GRANDMASTER.getValue())
            return RANK.MASTER;

        if (rank >= RANK.GRANDMASTER.getValue())
            return RANK.GRANDMASTER;

        return null;

    }

}
