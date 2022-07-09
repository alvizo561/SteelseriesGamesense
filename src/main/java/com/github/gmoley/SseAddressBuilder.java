package com.github.gmoley;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class SseAddressBuilder {
    private String coreProps;

    public SseAddressBuilder(String corePropsLine) {
        coreProps = corePropsLine;
    }

    public String getUrl(){

        JsonObject obj = new Gson().fromJson(coreProps,JsonObject.class);
        return "http://" + obj.get("address").getAsString();

    }
}
