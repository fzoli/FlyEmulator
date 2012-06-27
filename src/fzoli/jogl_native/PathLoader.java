package fzoli.jogl_native;

import java.io.File;
import java.lang.reflect.Field;

public class PathLoader {
    
    private static String getPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").equals("x86") ? "x86" : "x64";
        String path = "lib/native/";
        boolean isMac = os.indexOf( "mac" ) >= 0;
        if (os.indexOf( "win" ) >= 0) {
            path += "windows";
        }
        else if (os.indexOf( "nix") >= 0 || os.indexOf( "nux") >= 0) {
            path += "linux";
        }
        else if (isMac) {
            path += "mac";
        }
        if (!isMac) path += "/" + arch;
        return path;
    }
    
    public static void loadPaths() {
        String binaryPath = getPath();
        System.out.println("add java.library.path: " + binaryPath);
        try {
            String newLibPath = binaryPath + File.pathSeparator
                    + System.getProperty("java.library.path");
            System.setProperty("java.library.path", newLibPath);
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            if (fieldSysPath != null) {
                fieldSysPath.set(System.class.getClassLoader(), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}