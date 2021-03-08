package com.hazyarc14.model.steam;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Game {

    @SerializedName("appid")
    @Expose
    private Integer appid;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("playtime_forever")
    @Expose
    private Integer playtimeForever;
    @SerializedName("img_icon_url")
    @Expose
    private String imgIconUrl;
    @SerializedName("img_logo_url")
    @Expose
    private String imgLogoUrl;
    @SerializedName("has_community_visible_stats")
    @Expose
    private Boolean hasCommunityVisibleStats;
    @SerializedName("playtime_windows_forever")
    @Expose
    private Integer playtimeWindowsForever;
    @SerializedName("playtime_mac_forever")
    @Expose
    private Integer playtimeMacForever;
    @SerializedName("playtime_linux_forever")
    @Expose
    private Integer playtimeLinuxForever;
    @SerializedName("playtime_2weeks")
    @Expose
    private Integer playtime2weeks;

}