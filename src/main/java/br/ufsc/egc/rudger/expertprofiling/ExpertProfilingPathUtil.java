package br.ufsc.egc.rudger.expertprofiling;

import java.io.File;

class ExpertProfilingPathUtil {

    private static final String APPLICATION_PATH = System.getProperty("java.io.tmpdir") + "/expert-profiling/";

    public static File getPath(final String fileOrDir) {
        return new File(APPLICATION_PATH + fileOrDir);
    }

}