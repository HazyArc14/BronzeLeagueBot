package com.hazyarc14.model.steam;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Response {

    @SerializedName("game_count")
    @Expose
    private Integer gameCount;
    @SerializedName("games")
    @Expose
    private List<Game> games = null;

    public Integer getGameCount() {
        return gameCount;
    }

    public void setGameCount(Integer gameCount) {
        this.gameCount = gameCount;
    }

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

}