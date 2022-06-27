package com.example;

import com.google.gson.JsonObject;

public class StatRegister {
    private String game ;
    private TrackedStats gameEvent;
    private int minvalue;
    private int maxvalue;
    private int iconId;

    public StatRegister(TrackedStats gameEvent, int minvalue, int maxvalue, int iconId) {
        game = GamesensePlugin.game;
        this.gameEvent = gameEvent;
        this.minvalue = minvalue;
        this.maxvalue = maxvalue;
        this.iconId = iconId;
    }
    private JsonObject buildJson(){
        JsonObject object = new JsonObject();
        object.addProperty("game",game);
        object.addProperty("event",gameEvent.name());
        object.addProperty("min_value",minvalue);
        object.addProperty("max_value",maxvalue);
        object.addProperty("icon_id",iconId);
        return object;
    }

    public String toJsonString(){

        return buildJson().toString();
    }



}
