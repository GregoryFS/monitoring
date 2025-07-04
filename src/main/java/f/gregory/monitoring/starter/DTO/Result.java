package f.gregory.monitoring.starter.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Result {

    @JsonProperty(value = "code")
    private String code;

    @JsonProperty(value = "description")
    private String description;

    public Result() {
    }

    public Result(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
