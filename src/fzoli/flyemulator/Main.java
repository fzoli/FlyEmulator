package fzoli.flyemulator;

import com.ardor3d.framework.DisplaySettings;
import fzoli.flyemulator.view.Base3;
import fzoli.jogl_native.PathLoader;

public class Main {
    
    private static int getInt(String tf, int fail) {
        try {
            return Integer.parseInt(tf);
        }
        catch(Exception e) {
            return fail;
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0 || args.length == 3) PathLoader.loadPaths(); //-Djava.library.path=natives
        Base3 main = null;
        if (args.length == 0) {
            DisplaySettings settings = new DisplaySettings(640, 480, 24, 0, 0, 8, 0, 0, false, false);
            main = new Base3(settings, true);
        }
        else {
            if (args.length == 3) {
                DisplaySettings settings = new DisplaySettings(getInt(args[0], 640), getInt(args[1], 480), 24, 0, 0, 8, 0, 0, false, false);
                main = new Base3(settings, Boolean.parseBoolean(args[2]));
            }
            else {
                System.err.println("3 paramétert kérek: szélesség (int), magasság (int), anaglif (boolean)");
                System.exit(1);
            }
        }
        main.run();
    }
    
}