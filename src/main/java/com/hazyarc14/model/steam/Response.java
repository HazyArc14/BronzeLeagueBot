package com.hazyarc14.model.steam;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Response {

    @SerializedName("game_count")
    @Expose
    private Integer gameCount;
    @SerializedName("games")
    @Expose
    private List<Game> games = null;

}