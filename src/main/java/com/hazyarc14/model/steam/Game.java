package com.hazyarc14.model.steam;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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

    public Integer getAppid() {
        return appid;
    }

    public void setAppid(Integer appid) {
        this.appid = appid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPlaytimeForever() {
        return playtimeForever;
    }

    public void setPlaytimeForever(Integer playtimeForever) {
        this.playtimeForever = playtimeForever;
    }

    public String getImgIconUrl() {
        return imgIconUrl;
    }

    public void setImgIconUrl(String imgIconUrl) {
        this.imgIconUrl = imgIconUrl;
    }

    public String getImgLogoUrl() {
        return imgLogoUrl;
    }

    public void setImgLogoUrl(String imgLogoUrl) {
        this.imgLogoUrl = imgLogoUrl;
    }

    public Boolean getHasCommunityVisibleStats() {
        return hasCommunityVisibleStats;
    }

    public void setHasCommunityVisibleStats(Boolean hasCommunityVisibleStats) {
        this.hasCommunityVisibleStats = hasCommunityVisibleStats;
    }

    public Integer getPlaytimeWindowsForever() {
        return playtimeWindowsForever;
    }

    public void setPlaytimeWindowsForever(Integer playtimeWindowsForever) {
        this.playtimeWindowsForever = playtimeWindowsForever;
    }

    public Integer getPlaytimeMacForever() {
        return playtimeMacForever;
    }

    public void setPlaytimeMacForever(Integer playtimeMacForever) {
        this.playtimeMacForever = playtimeMacForever;
    }

    public Integer getPlaytimeLinuxForever() {
        return playtimeLinuxForever;
    }

    public void setPlaytimeLinuxForever(Integer playtimeLinuxForever) {
        this.playtimeLinuxForever = playtimeLinuxForever;
    }

    public Integer getPlaytime2weeks() {
        return playtime2weeks;
    }

    public void setPlaytime2weeks(Integer playtime2weeks) {
        this.playtime2weeks = playtime2weeks;
    }

}