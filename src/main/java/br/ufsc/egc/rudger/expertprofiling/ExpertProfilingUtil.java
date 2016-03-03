package br.ufsc.egc.rudger.expertprofiling;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ExpertProfilingUtil {

    private static final String APPLICATION_PATH = System.getProperty("java.io.tmpdir") + "/expert-profiling/";

    public static File getPath(final String fileOrDir) {
        return new File(APPLICATION_PATH + fileOrDir);
    }
    
    
    public static ExpertProfilingPipeline.Configuration createConfig() {
        ExpertProfilingPipeline.Configuration config = new ExpertProfilingPipeline.Configuration();

        List<String> dppediaFiles = new ArrayList<>();
        dppediaFiles.add("http://downloads.dbpedia.org/2015-10/core-i18n/pt/skos_categories_pt.ttl.bz2");
        dppediaFiles.add("http://downloads.dbpedia.org/2015-10/core-i18n/en/skos_categories_en.ttl.bz2");
        config.setDppediaFiles(dppediaFiles);

        List<String> extensions = new ArrayList<>();
        extensions.add("**/*.pdf");
        extensions.add("**/*.txt");
        extensions.add("**/*.docx");
        extensions.add("**/*.doc");
        extensions.add("**/*.ppt");
        extensions.add("**/*.pptx");
        config.setExtensions(extensions);

        List<String> stopwordFiles = new ArrayList<>();
        stopwordFiles.add("stopwords/stopwords_pt_BR.txt");
        stopwordFiles.add("stopwords/stopwords_en.txt");
        config.setStopWordFiles(stopwordFiles);

        config.setCreateNewAnnotationIndex(true);

        return config;
    }


}