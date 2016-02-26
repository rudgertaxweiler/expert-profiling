package br.ufsc.egc.rudger.expertprofiling;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;

import br.ufsc.egc.rudger.expertprofiling.nlp.consumer.Annotations2Lucene;
import br.ufsc.egc.rudger.expertprofiling.nlp.consumer.Lucene2ProfilePage;
import br.ufsc.egc.rudger.expertprofiling.nlp.dbpedia.DbpediaAnnotator;
import br.ufsc.egc.rudger.expertprofiling.nlp.heideltime.HeidelTimeWrapper;
import br.ufsc.egc.rudger.expertprofiling.nlp.io.DocumentReader;
import br.ufsc.egc.rudger.expertprofiling.nlp.stopword.StopWordAnnotator;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.Organization;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.Person;
import de.tudarmstadt.ukp.dkpro.core.dictionaryannotator.DictionaryAnnotator;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class ExpertProfilingPipeline {

    public static void main(final String[] args) throws IOException, UIMAException, URISyntaxException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<String> stopWordFiles = new ArrayList<>();
        stopWordFiles.add("src/main/resources/stopwords/stopwords_pt_BR.txt");
        stopWordFiles.add("src/main/resources/stopwords/stopwords_en.txt");

        List<String> dppediaFiles = new ArrayList<>();
        dppediaFiles.add("http://downloads.dbpedia.org/2015-10/core-i18n/pt/skos_categories_pt.ttl.bz2");
        dppediaFiles.add("http://downloads.dbpedia.org/2015-10/core-i18n/en/skos_categories_en.ttl.bz2");

        String userCode = "04188289970";
        String userName = "Rudger Nowasky do Nascimento";

        String targetFile = "target/profiles/" + userName + ".html";
        boolean debug = true;
        boolean parseDates = false;

        //@formatter:off
        CollectionReaderDescription reader = createReaderDescription(
                DocumentReader.class,
                DocumentReader.PARAM_NORMALIZE_TEXT, true,
                DocumentReader.PARAM_SOURCE_LOCATION, "content/docs-examples",
                DocumentReader.PARAM_PATTERNS, new String[]{"**/*.pdf", "**/*.txt", "**/*.docx", "**/*.doc", "**/*.xls", "**/*.xlsx", "**/*.ppt", "**/*.pptx"},
                DocumentReader.PARAM_LANGUAGE, "pt_BR");

        AnalysisEngineDescription tokenizer = createEngineDescription(
                BreakIteratorSegmenter.class);
        
        AnalysisEngineDescription stopword = createEngineDescription(
                StopWordAnnotator.class,
                StopWordAnnotator.PARAM_MODEL_LOCATION, stopWordFiles);

        AnalysisEngineDescription dbpedia = createEngineDescription(
                DbpediaAnnotator.class,
                DbpediaAnnotator.PARAM_DBPEDIA_LINKS, dppediaFiles,
                DbpediaAnnotator.PARAM_DBPEDIA_INDEX_PATH, AppConfiguration.getPath("dbpedia_index"));
        
        AnalysisEngineDescription heidel = createEngineDescription(
                HeidelTimeWrapper.class,
                HeidelTimeWrapper.PARAM_LANGUAGE, "portuguese",
                HeidelTimeWrapper.PARAM_TYPE_TO_PROCESS, "narrative",
                HeidelTimeWrapper.PARAM_DATE, true,
                HeidelTimeWrapper.PARAM_TIME, false,
                HeidelTimeWrapper.PARAM_DURATION, false,
                HeidelTimeWrapper.PARAM_SET, true,
                HeidelTimeWrapper.PARAM_DEBUG, false,
                HeidelTimeWrapper.PARAM_GROUP, true);
        
        AnalysisEngineDescription nameFinder = createEngineDescription(
                DictionaryAnnotator.class,
                DictionaryAnnotator.PARAM_MODEL_LOCATION, "content/dictionaries/names.txt",
                DictionaryAnnotator.PARAM_ANNOTATION_TYPE, Person.class);
       
        AnalysisEngineDescription organizationFinder = createEngineDescription(
               DictionaryAnnotator.class,
               DictionaryAnnotator.PARAM_MODEL_LOCATION, "content/dictionaries/organizations.txt",
               DictionaryAnnotator.PARAM_ANNOTATION_TYPE, Organization.class);
        
        AnalysisEngineDescription luceneWriter = createEngineDescription(
                Annotations2Lucene.class,
                Annotations2Lucene.PARAM_OWNER_CODE, userCode,
                Annotations2Lucene.PARAM_OWNER_NAME, userName,
                Annotations2Lucene.PARAM_INDEX_PATH, AppConfiguration.getPath("document_annotation_index"));
        
        AnalysisEngineDescription timelineJsWriter = createEngineDescription(
                Lucene2ProfilePage.class,
                Lucene2ProfilePage.PARAM_OWNER_CODE, userCode,
                Lucene2ProfilePage.PARAM_OWNER_NAME, userName,
                Lucene2ProfilePage.PARAM_INDEX_PATH, AppConfiguration.getPath("document_annotation_index"),
                Lucene2ProfilePage.PARAM_VELOCITY_TEMPLATE_FILE, "src/main/resources/templates/profile.html",
                Lucene2ProfilePage.PARAM_TARGET_FILE, targetFile);
        
        AnalysisEngineDescription dumper = createEngineDescription(
                XmiWriter.class,
                XmiWriter.PARAM_TARGET_LOCATION, "target/analysis/result",
                XmiWriter.PARAM_TYPE_SYSTEM_FILE, "target/analysis/type/TypeSystem.xml",
                XmiWriter.PARAM_OVERWRITE, true);
       //@formatter:on

        List<AnalysisEngineDescription> engines = new ArrayList<>();
        engines.add(tokenizer);
        engines.add(stopword);
        engines.add(dbpedia);
        engines.add(nameFinder);
        engines.add(organizationFinder);
        if (parseDates) {
            engines.add(heidel);
        }
        engines.add(luceneWriter);
        if (debug) {
            engines.add(dumper);
        }
        engines.add(timelineJsWriter);

        SimplePipeline.runPipeline(reader, engines.toArray(new AnalysisEngineDescription[engines.size()]));

        stopWatch.stop();
        System.out.println(stopWatch);
        
        openWebpage(new File(targetFile).toURI());
    }

    public static void openWebpage(final URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
