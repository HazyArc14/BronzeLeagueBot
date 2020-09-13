package com.hazyarc14.service;

import com.google.gson.GsonBuilder;
import com.hazyarc14.model.steam.Game;
import com.hazyarc14.model.steam.OwnedGames;
import com.hazyarc14.model.steam.Response;
import com.hazyarc14.repository.UserInfoRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SteamAPIService {

    public static final Logger log = LoggerFactory.getLogger(UserRankService.class);

    @Autowired
    UserInfoRepository userInfoRepository;

    @Value("${steam.api.base.url")
    private String steamApiBaseUrl;

    @Value("${steam.api.key")
    private String steamAPIKey;

    public void findCommonSteamGames(MessageReceivedEvent event, List<Member> membersList) {

        List<Set<String>> sets = new ArrayList<>();
        List<String> privateUsersList = new ArrayList<>();
        List<String> misconfiguredUsersList = new ArrayList<>();

        membersList.forEach(member -> {

            userInfoRepository.findById(member.getIdLong()).ifPresent(userInfo -> {

                Long steamId = userInfo.getSteamId();

                if (steamId != null) {

                    try (CloseableHttpClient client = HttpClients.createDefault()) {

                        GsonBuilder builder = new GsonBuilder();
                        String requestUrl = steamApiBaseUrl + "&key=" + steamAPIKey + "&steamid=" + steamId;
                        HttpGet request = new HttpGet(requestUrl);
                        OwnedGames ownedGames = client.execute(request, httpResponse ->
                                builder.create().fromJson(String.valueOf(httpResponse.getEntity().getContent()), OwnedGames.class));

                        Optional<Response> ownedGamesResponse = Optional.ofNullable(ownedGames.getResponse());

                        if (ownedGamesResponse.isPresent()) {

                            List<Game> gamesList = ownedGames.getResponse().getGames();
                            List<String> gameNamesList = new ArrayList<>();
                            gamesList.forEach(game -> {  gameNamesList.add(game.getName()); });

                            Set<String> newSet = new HashSet<String>(gameNamesList);
                            sets.add(newSet);

                        } else {

                            privateUsersList.add(userInfo.getUserName());

                        }

                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {

                    misconfiguredUsersList.add(userInfo.getUserName());

                }

            });

        });

        StringBuilder misconfiguredUsersMessage = new StringBuilder();
        misconfiguredUsersMessage.append("```SteamID missing for users:\n");
        misconfiguredUsersList.forEach(user -> {
            misconfiguredUsersMessage.append(user + "\n");
        });
        misconfiguredUsersMessage.append("```");

        StringBuilder privateUsersMessage = new StringBuilder();
        privateUsersMessage.append("```Profiles private for users:\n");
        privateUsersList.forEach(user -> {
            privateUsersMessage.append(user + "\n");
        });
        privateUsersMessage.append("```\n\n");

        if (sets.size() > 1) {

            for (int i = 0; i <= sets.size(); i++) {
                if (i != 0) {
                    sets.get(0).retainAll(sets.get(i));
                }
            }

            StringBuilder commonGames = new StringBuilder();
            for (String s: sets.get(0)) {
                commonGames.append(s + "\n");
            }

            String commonGamesMessage = "```Common Games: \n" + commonGames.toString() + "```\n\n";
            String responseMessage = commonGamesMessage + privateUsersMessage.toString() + misconfiguredUsersMessage.toString();

            event.getChannel().sendMessage(responseMessage).queue();

        } else {

            String errorMessage = "Not enough users in the voice channel have their SteamID configured.\n\n" + misconfiguredUsersMessage.toString();
            event.getChannel().sendMessage(errorMessage).queue();

        }

    }

}