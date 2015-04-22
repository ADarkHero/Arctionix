package steampanel.helper;

public class SteamApp {

    private int appID;
    private int PlayTime;
    
    public SteamApp(int appID, int PlayTime){
        this.appID = appID;
        this.PlayTime = PlayTime;
    }

    public int getAppID() {
        return appID;
    }

    public void setAppID(int appID) {
        this.appID = appID;
    }

    public int getPlayTime() {
        return PlayTime;
    }

    public void setPlayTime(int PlayTime) {
        this.PlayTime = PlayTime;
    }
    
    

}
