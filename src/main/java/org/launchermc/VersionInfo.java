package org.launchermc;

public class VersionInfo {
    private String name;
    private String type;

    public VersionInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
