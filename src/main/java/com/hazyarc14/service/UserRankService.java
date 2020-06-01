package com.hazyarc14.service;

import com.hazyarc14.model.UserRank;
import com.hazyarc14.repository.UserRanksRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserRankService {

    public static final Logger log = LoggerFactory.getLogger(UserRankService.class);

    public static final Integer BRONZE = 0;
    public static final Integer SILVER = 100;
    public static final Integer GOLD = 250;
    public static final Integer PLATINUM = 500;
    public static final Integer DIAMOND = 750;
    public static final Integer MASTER = 1000;
    public static final Integer GRANDMASTER = 2000;
    public static final Integer MAXRANK = 2200;

    public static final Integer minsPerPointEarned = 10;

    @Autowired
    UserRanksRepository userRanksRepository;

    public void createUserRank(Member member) {

        UserRank userRank = new UserRank();
        userRank.setUserId(member.getIdLong());
        userRank.setUserName(member.getEffectiveName());
        userRank.setRank(0);

        userRanksRepository.save(userRank);

    }

    public UserRank calculateUserRank(UserRank userRank) {

        Integer currentRank = userRank.getRank();
        Timestamp joinedTm = userRank.getJoinedChannelTm();
        Timestamp leftTm = userRank.getLeftChannelTm();

        // need to see how long they were in the channel and update rank
        Long minutesInChannel = TimeUnit.MINUTES.convert(leftTm.getTime() - joinedTm.getTime(), TimeUnit.MILLISECONDS);
        log.info("minutesInChannel: " + minutesInChannel);

        Integer pointsToAdd = Math.floorDiv(minutesInChannel.intValue(), minsPerPointEarned);
        log.info("pointsToAdd: " + pointsToAdd);

        Integer updatedRank = currentRank + pointsToAdd;
        if (updatedRank > MAXRANK)
            updatedRank = MAXRANK;

        userRank.setRank(updatedRank);

        userRanksRepository.save(userRank);

        return userRank;

    }

    public void updateUserRoleByRank(Guild guild, Member member, UserRank previousUserRank, UserRank userRank) {

        List<Role> memberRoles = member.getRoles();
        List<Role> newMemberRoles = new ArrayList<>();

        Role oldGuildRole = guild.getRolesByName(calculateRoleByRank(previousUserRank.getRank()), false).get(0);
        Role newGuildRole = guild.getRolesByName(calculateRoleByRank(userRank.getRank()), false).get(0);

        if (oldGuildRole.getName() == newGuildRole.getName())
            return;

        for (int i = 0; i < memberRoles.size(); i++) {

            if (memberRoles.get(i).getName() != oldGuildRole.getName())
                newMemberRoles.add(memberRoles.get(i));

        }

        newMemberRoles.add(newGuildRole);

        guild.modifyMemberRoles(member, newMemberRoles).queue();

    }

    @Scheduled(cron = "0 0 5 * * ?")
    public void scheduledDecayCalculation() {

        List<UserRank> userRankList = userRanksRepository.findAll();
        Timestamp currentTm = new Timestamp(System.currentTimeMillis());

        userRankList.forEach(userRank -> {

            Integer currentRank = userRank.getRank();
            Timestamp leftTm = userRank.getLeftChannelTm();

            if (leftTm != null) {

                Long temp = TimeUnit.DAYS.convert(currentTm.getTime() - leftTm.getTime(), TimeUnit.MILLISECONDS);
                Integer daySinceChannelJoined = temp.intValue();

                if (daySinceChannelJoined > 7) {

                    Integer pointsToRemove = calculateDecayValue(currentRank);

                    UserRank previousUserRank = new UserRank(userRank);
                    userRank.setRank(currentRank - pointsToRemove);
                    userRanksRepository.save(userRank);
//                    updateUserRoleByRank(event.getGuild(), eventMember, previousUserRank, userRank);

                }

            }

        });

    }

    public Integer calculateDecayValue(Integer rank) {

        if (rank >= BRONZE && rank < SILVER)
            return 0;

        if (rank >= SILVER && rank < GOLD)
            return 1;

        if (rank >= GOLD && rank < PLATINUM)
            return 5;

        if (rank >= PLATINUM && rank < DIAMOND)
            return 10;

        if (rank >= DIAMOND && rank < MASTER)
            return 15;

        if (rank >= MASTER && rank < GRANDMASTER)
            return 20;

        if (rank >= GRANDMASTER)
            return 30;

        return 0;

    }

    public String calculateRoleByRank(Integer rank) {

        if (rank >= BRONZE && rank < SILVER)
            return "Bronze";

        if (rank >= SILVER && rank < GOLD)
            return "Silver";

        if (rank >= GOLD && rank < PLATINUM)
            return "Gold";

        if (rank >= PLATINUM && rank < DIAMOND)
            return "Platinum";

        if (rank >= DIAMOND && rank < MASTER)
            return "Diamond";

        if (rank >= MASTER && rank < GRANDMASTER)
            return "Master";

        if (rank >= GRANDMASTER)
            return "GrandMaster";

        return "";

    }

}
