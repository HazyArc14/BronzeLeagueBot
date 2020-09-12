package com.hazyarc14.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hazyarc14.model.steam.Game;
import com.hazyarc14.model.steam.OwnedGames;
import com.hazyarc14.repository.UserInfoRepository;
import net.dv8tion.jda.api.entities.Member;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SteamAPIService {

    public static final Logger log = LoggerFactory.getLogger(UserRankService.class);

    @Autowired
    UserInfoRepository userInfoRepository;

    @Value("${steam.api.base.url")
    private String steamApiBaseUrl;

    @Value("${steam.api.key")
    private String steamAPIKey;

    public Set<String> findCommonSteamGames(List<Member> membersList) {

        List<Set<String>> sets = new ArrayList<>();

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

                        List<Game> gamesList = ownedGames.getResponse().getGames();
                        List<String> gameNamesList = new ArrayList<>();
                        gamesList.forEach(game -> {  gameNamesList.add(game.getName()); });

                        Set<String> newSet = new HashSet<String>(gameNamesList);
                        sets.add(newSet);

                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            });

        });

        if (sets.size() > 1) {

            for (int i = 0; i <= sets.size(); i++) {
                if (i != 0) {
                    sets.get(0).retainAll(sets.get(i));
                }
            }

        }

        return sets.get(0);

    }

}
