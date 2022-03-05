package entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class M3UEntry {

    private String data;
    private String url;

    public String getChannel() {
        return data + System.lineSeparator() + url;
    }

}
