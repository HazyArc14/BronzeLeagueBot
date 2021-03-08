package com.hazyarc14.model.steam;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class OwnedGames {

    @SerializedName("response")
    @Expose
    private Response response;

}