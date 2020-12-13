package is.yarr.rdf.config.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UserData {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("tokenPath")
    @Expose
    private String tokenPath;

    @SerializedName("uploadTo")
    @Expose
    private String uploadTo;

    @SerializedName("teamDrive")
    @Expose
    private String teamDrive;

    @SerializedName("file")
    @Expose
    private String file;

    @SerializedName("randomName")
    @Expose
    private boolean randomName;

    @SerializedName("threads")
    @Expose
    private int threads;

    @SerializedName("delay")
    @Expose
    private int delay;

    @SerializedName("count")
    @Expose
    private int count = -1;

    public String getName() {
        return name;
    }

    public UserData setName(String name) {
        this.name = name;
        return this;
    }

    public String getTokenPath() {
        return tokenPath;
    }

    public UserData setTokenPath(String tokenPath) {
        this.tokenPath = tokenPath;
        return this;
    }

    public String getUploadTo() {
        return uploadTo;
    }

    public UserData setUploadTo(String uploadTo) {
        this.uploadTo = uploadTo;
        return this;
    }

    public String getTeamDrive() {
        return teamDrive;
    }

    public UserData setTeamDrive(String teamDrive) {
        this.teamDrive = teamDrive;
        return this;
    }

    public String getFile() {
        return file;
    }

    public UserData setFile(String file) {
        this.file = file;
        return this;
    }

    public boolean getRandomName() {
        return randomName;
    }

    public UserData setRandomName(boolean randomName) {
        this.randomName = randomName;
        return this;
    }

    public int getThreads() {
        return threads;
    }

    public UserData setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public UserData setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public int getCount() {
        return count;
    }

    public UserData setCount(int count) {
        this.count = count;
        return this;
    }
}