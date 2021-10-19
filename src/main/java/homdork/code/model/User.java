package homdork.code.model;

public class User {
    public String name;
    public String email;
    public String uuid;
    // public List<Device> userDevices;

    public User(String name, String email, String uuid) {
        this.name = name;
        this.email = email;
        this.uuid = uuid;
        // this.userDevices = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUuid() {
        return uuid;
    }
}
