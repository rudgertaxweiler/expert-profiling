package br.ufsc.egc.rudger.expertprofiling;

import java.io.File;

public class AppConfiguration {

    private static final String APPLICATION_PATH = System.getProperty("user.home") + "/expert-profiling/";

    public static File getPath(final String fileOrDir) {
        return new File(APPLICATION_PATH + fileOrDir);
    }
    
    public static File getApplicationBasePath() {
        return new File(APPLICATION_PATH);
    }

}