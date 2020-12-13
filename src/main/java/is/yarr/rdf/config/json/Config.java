package is.yarr.rdf.config.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Config {

    @SerializedName("users")
    @Expose
    private List<UserData> users = null;

    public List<UserData> getUsers() {
        return users;
    }

    public Config setUsers(List<UserData> users) {
        this.users = users;
        return this;
    }
}