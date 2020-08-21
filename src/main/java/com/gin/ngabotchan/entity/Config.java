package com.gin.ngabotchan.entity;

import lombok.Data;

@Data
public class Config {
    String name,value;

    public Config(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
