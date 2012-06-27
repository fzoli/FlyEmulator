package fzoli.flyemulator.mp3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class MP3 {
    private File file;
    private Player player; 

    // constructor that takes the name of an MP3 file
    public MP3(String filename) {
        try {
            //URI uri = this.getClass().getResource(filename).toURI();
            
            file = new File("lib/mp3/"+filename);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void close() { if (player != null) player.close(); }
    
    private void init() throws FileNotFoundException, JavaLayerException {
            FileInputStream fis     = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            player = new Player(bis);
    }
    
    // play the MP3 file to the sound card
    public void play() {
        try {
            init();
        }
        catch (Exception e) {
            System.out.println("Problem playing file " + file.getName());
            System.out.println(e);
        }

        // run in new thread to play in background
        
        new Thread() {
            public void run() {
                try {
                    player.play();                    
                }
                catch (Exception e) { System.out.println(e); }
            }
        }.start();

    }

    public boolean isComplete() {
        return player.isComplete();
    }
    
}