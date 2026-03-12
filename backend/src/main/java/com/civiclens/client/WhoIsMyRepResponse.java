package com.civiclens.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Response from whoismyrepresentative.com getall_mems.php?zip=...&output=json
 * @see <a href="https://whoismyrepresentative.com/api">API</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhoIsMyRepResponse {

    @JsonProperty("results")
    private List<Result> results = Collections.emptyList();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String name;
        private String party;
        private String state;
        private String district;
        private String phone;
        private String office;
        private String link;
    }
}
