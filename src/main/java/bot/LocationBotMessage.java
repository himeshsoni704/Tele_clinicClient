package bot;

import com.google.gson.GsonBuilder;

public class LocationBotMessage extends BotMessage {

    private Double latitude;
    private Double longitude;

    public LocationBotMessage(String chatId, Double latitude, Double longitude) {
        super(chatId);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    @Override
    public void store() {
        // Optional: override if needed
    }
}
