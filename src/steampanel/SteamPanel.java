/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package steampanel;

import java.awt.Desktop;
import steampanel.helper.SteamApp;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.EventHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;
import steampanel.helper.Position;

/**
 *
 * @author ADarkHero
 */
public class SteamPanel extends Application {

    public Pane root;
    public Scene scene;
    private double displaywidth = 0;
    private double displayheight = 0;
    private final int startX = 100;
    private final int startY = 50;
    //Number of games in account
    private int gameCount = 0;
    private double playTime = 0;
    private int highestPlaytime = 0;
    //Temp variables
    private int tempX = startX;
    private int tempY = startY;
    //List of all Games+Playtime
    private ArrayList<SteamApp> appList = new ArrayList<>();
    //Possible Positions for next game
    private ArrayList<Position> positions = new ArrayList<>();

    /**
     * Starts the program 
     * 
     * @param primaryStage 
     */
    @Override
    public void start(Stage primaryStage) {      
        root = new Pane();
        root.setStyle("-fx-background-color: transparent;");

        //Gets display size
        getDisplaySize();

        //Magic.
        try {
            createFolders();                        //Creates cnf/img folders, if they not already exist
            parseInformation();                     //Parses information from Steam
            //displayListCmd();                     //Displays in Cmd
            createView();                           //Displays in GUI
        } catch (IOException ex) {
            Logger.getLogger(SteamPanel.class.getName()).log(Level.SEVERE, null, ex);
        }

        scene = new Scene(root, displaywidth, displayheight);

        scene.setFill(Color.TRANSPARENT);

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * Parses Json Information from Steam (Number of Games, Games, Playtime)
     * Also orders list and gets playtime at the end.
     *
     * @throws MalformedURLException
     * @throws IOException
     */
    private void parseInformation() throws MalformedURLException, IOException {
        String uid = getUID();

        String jsonURL = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=00B8A027FE0C5EDCA5AE39E1D0C6189D&steamid=" + uid + "&format=json";

        //Downloading Json file from Steam
        URL website = new URL(jsonURL);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream("cnf/games.json");
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        //Read Json file from Data
        InputStream is = new FileInputStream("cnf/games.json");
        StringBuilder sb = new StringBuilder(512);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            int c = 0;
            while ((c = r.read()) != -1) {
                sb.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Create Json Object
        JSONObject obj = new JSONObject(sb.toString());

        gameCount = obj.getJSONObject("response").getInt("game_count");

        JSONArray arr = obj.getJSONObject("response").getJSONArray("games");
        for (int i = 0; i < arr.length(); i++) {
            int appID = arr.getJSONObject(i).getInt("appid");
            int playTime = arr.getJSONObject(i).getInt("playtime_forever");
            appList.add(new SteamApp(appID, playTime));
        }

        orderList();
        getPlayTime();
    }

    /**
     * Orders the parsed Json list
     */
    private void orderList() {
        SteamApp temp;
        for (int i = 1; i < appList.size(); i++) {
            for (int j = 0; j < appList.size() - i; j++) {
                if (appList.get(j).getPlayTime() < appList.get(j + 1).getPlayTime()) {
                    temp = appList.get(j);
                    appList.set(j, appList.get(j + 1));
                    appList.set(j + 1, temp);
                }
            }

        }
    }

    /**
     * Gets the playtime of all games together
     */
    private void getPlayTime() {
        for (int i = 0; i < appList.size(); i++) {
            playTime += appList.get(i).getPlayTime();
            if(appList.get(i).getPlayTime() > highestPlaytime){
                highestPlaytime = appList.get(i).getPlayTime();
            }
        }

    }

    /**
     * Displays the list to the command line
     */
    private void displayListCmd() {
        System.out.println("Game Count: " + gameCount);
        System.out.println("Gametime: " + playTime + " minutes");
        System.out.println();
        for (SteamApp steamApp : appList) {
            System.out.println("AppID: " + steamApp.getAppID() + "\t Playtime: " + steamApp.getPlayTime() + " minutes");
        }
    }

    /**
     * Creates the view.
     */
    private void createView() {
        double temp = 0, xPercent = 0, yPercent = 0, width = 0, height = 0, multiplicator = 1;

        //Get image size multiplicator
        do {
            temp = displaywidth * (double) appList.get(0).getPlayTime() / highestPlaytime * multiplicator;
            multiplicator -= 0.01;
        } while (temp > 460);
        
        //First Position
        positions.add(new Position(tempX, tempY));

        for (SteamApp steamApp : appList) {
            if (steamApp.getPlayTime() != 0) {
                xPercent = (double) steamApp.getPlayTime() / highestPlaytime;
                yPercent = xPercent;
                width = displaywidth * xPercent * multiplicator;
                height = displayheight * yPercent * multiplicator;
                
                if (height < 1) { height = 1; }
                if (width < 1) { width = 1; }
                
                setNewTemp(width, height);
                addGame(width, height, steamApp.getAppID(), tempX, tempY);   
            }
        }
    }

    /**
     * Adds game to view.
     *
     * @param width
     * @param height
     * @param id Steam app ID
     */
    private void addGame(double width, double height, int id, int x, int y) {
        //Round variables
        width = Math.round(width);
        height = Math.round(height);

        if (width == 0) {
            width = 1;
        }
        if (height == 0) {
            height = 1;
        }

        try {
            downloadPicture(id);
        } catch (IOException ex) {
            Logger.getLogger(SteamPanel.class.getName()).log(Level.SEVERE, null, ex);
        }

        String imagepath = "file:img/" + id + ".jpg";
        Image image = new Image(imagepath);
        ImageView iv = new ImageView();
        iv.setImage(image);
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        Hyperlink hpl = new Hyperlink();
        hpl.setText("");
        hpl.setGraphic(iv);
        hpl.setLayoutX(x);
        hpl.setLayoutY(y);
        
        //Open Steam game, if pressed
        hpl.setOnAction(event -> {
        String url = "steam://rungameid/" + id;

        if(Desktop.isDesktopSupported()){
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else{
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        });
        root.getChildren().add(hpl);
    }

    /**
     * Downloads the header pictures of the games from Steam
     *
     * @param id Steam game id
     * @throws MalformedURLException
     * @throws IOException
     */
    private void downloadPicture(int id) throws MalformedURLException, IOException {
        String picURL = "http://cdn.akamai.steamstatic.com/steam/apps/" + id + "/header.jpg";
        String picPath = "img/" + id + ".jpg";

        File f = new File(picPath);
        if (f.exists() && !f.isDirectory()) {
            //Download only, if not already downloaded!
        } else {
            //Downloading Json file from Steam
            URL website = new URL(picURL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(picPath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    /**
     * Reads the Steam User ID from file or lets the user input it
     *
     * @return Steam user ID as String
     */
    private String getUID() {
        String uid = "";

        //Get user id or let them set it, if they start the program first
        try {
            Scanner in = new Scanner(new FileReader("cnf/id.txt"));
            uid = in.next();
            if (uid.equals("") || uid.equals("null")) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException | NoSuchElementException ex) {
            String newID = "";

            newID = JOptionPane.showInputDialog("Please enter your Steam 64-Id!\n"
                    + "You can look it up at http://steamid.co/");              //ToDo - Maybe make clickable
            PrintWriter writer = null;
            try {
                writer = new PrintWriter("cnf/id.txt", "UTF-8");
            } catch (FileNotFoundException | UnsupportedEncodingException ex1) {
                Logger.getLogger(SteamPanel.class.getName()).log(Level.SEVERE, null, ex1);
            }
            writer.println(newID);
            writer.close();
        }

        if (uid.equals("") || uid.equals("null")) {
            System.exit(0);
        }

        return uid;
    }

    /**
     * Sets new X/Y position for the pictures Does a magical algorithm.
     *
     * @param width
     * @param height
     */
    private void setNewTemp(double width, double height) {
        double newTempX = tempX;
        double newTempY = tempY;
        //Maximal width
        double maxdisplaywidth = displaywidth - width - startX;
        double maxdisplayheight = displayheight - height - startY;
        
        try{
            tempX = (int) positions.get(0).getX();
            tempY = (int) positions.get(0).getY();
        }
        catch(ArrayIndexOutOfBoundsException ex){
            
        }

        if (newTempX < maxdisplaywidth) {
            positions.add(new Position(newTempX + width, newTempY));
        }

        if (newTempY < maxdisplayheight) {
            positions.add(new Position(newTempX, newTempY + height));
        }
        
        positions.remove(0);

    }

    /**
     * Gets the users display size
     */
    private void getDisplaySize() {
        //Get width/height of Monitor
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        displaywidth = screenSize.getWidth();
        displayheight = screenSize.getHeight();
    }
    
    /**
     * Creates cnf/img folders, if they not already exist
     */
    private void createFolders() {
        File f = new File("/img");
        f.mkdir();
        
        f = new File("cnf/");
        f.mkdir();  
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
