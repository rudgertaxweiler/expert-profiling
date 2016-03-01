package br.ufsc.egc.rudger.expertprofiling;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger logger = LoggerFactory.getLogger(ExpertProfilingPipeline.class);

    public static class Configuration {

        private List<String> stopWordFiles;
        private List<String> dppediaFiles;

        private String userCode;
        private String userName;

        private List<String> extensions;

        private String personDictionaryLocation;
        private String organizationDictionaryLocation;

        private String sourceLocation;

        private boolean useXmiDumper;
        private boolean useHeidelTime;
        private boolean createNewAnnotationIndex;

        public List<String> getStopWordFiles() {
            return this.stopWordFiles;
        }

        public void setStopWordFiles(final List<String> stopWordFiles) {
            this.stopWordFiles = stopWordFiles;
        }

        public List<String> getDppediaFiles() {
            return this.dppediaFiles;
        }

        public void setDppediaFiles(final List<String> dppediaFiles) {
            this.dppediaFiles = dppediaFiles;
        }

        public String getUserCode() {
            return this.userCode;
        }

        public void setUserCode(final String userCode) {
            this.userCode = userCode;
        }

        public String getUserName() {
            return this.userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public List<String> getExtensions() {
            return this.extensions;
        }

        public void setExtensions(final List<String> extensions) {
            this.extensions = extensions;
        }

        public String getPersonDictionaryLocation() {
            return this.personDictionaryLocation;
        }

        public void setPersonDictionaryLocation(final String personDictionaryLocation) {
            this.personDictionaryLocation = personDictionaryLocation;
        }

        public String getOrganizationDictionaryLocation() {
            return this.organizationDictionaryLocation;
        }

        public void setOrganizationDictionaryLocation(final String organizationDictionaryLocation) {
            this.organizationDictionaryLocation = organizationDictionaryLocation;
        }

        public String getSourceLocation() {
            return this.sourceLocation;
        }

        public void setSourceLocation(final String sourceLocation) {
            this.sourceLocation = sourceLocation;
        }

        public boolean isUseXmiDumper() {
            return this.useXmiDumper;
        }

        public void setUseXmiDumper(final boolean useXmiDumper) {
            this.useXmiDumper = useXmiDumper;
        }

        public boolean isUseHeidelTime() {
            return this.useHeidelTime;
        }

        public void setUseHeidelTime(final boolean useHeidelTime) {
            this.useHeidelTime = useHeidelTime;
        }

        public boolean isCreateNewAnnotationIndex() {
            return this.createNewAnnotationIndex;
        }

        public void setCreateNewAnnotationIndex(final boolean createNewAnnotationIndex) {
            this.createNewAnnotationIndex = createNewAnnotationIndex;
        }

    }

    public File run(final Configuration config) throws IOException, UIMAException, URISyntaxException {
        this.runtimeParameters();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String targetFile = ExpertProfilingPathUtil.getPath("profiles") + config.userName + ".html";

        List<AnalysisEngineDescription> engines = new ArrayList<>();

        //@formatter:off
        CollectionReaderDescription reader = createReaderDescription(
                DocumentReader.class,
                DocumentReader.PARAM_NORMALIZE_TEXT, true,
                DocumentReader.PARAM_SOURCE_LOCATION, config.sourceLocation,
                DocumentReader.PARAM_PATTERNS, config.extensions,
                DocumentReader.PARAM_LANGUAGE, Locale.getDefault());
        
        AnalysisEngineDescription tokenizer = createEngineDescription(
                BreakIteratorSegmenter.class);
        engines.add(tokenizer);
        
        if(config.stopWordFiles != null && !config.stopWordFiles.isEmpty()){
            AnalysisEngineDescription stopword = createEngineDescription(
                    StopWordAnnotator.class,
                    StopWordAnnotator.PARAM_MODEL_LOCATION, config.stopWordFiles);
            engines.add(stopword);
        }

        AnalysisEngineDescription dbpedia = createEngineDescription(
                DbpediaAnnotator.class,
                DbpediaAnnotator.PARAM_DBPEDIA_LINKS, config.dppediaFiles,
                DbpediaAnnotator.PARAM_DBPEDIA_INDEX_PATH, ExpertProfilingPathUtil.getPath("dbpedia_index"));
        engines.add(dbpedia);
        
        if(config.useHeidelTime){
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
            engines.add(heidel);
        }
       
        
        if(config.personDictionaryLocation != null){
            AnalysisEngineDescription nameFinder = createEngineDescription(
                    DictionaryAnnotator.class,
                    DictionaryAnnotator.PARAM_MODEL_LOCATION, config.personDictionaryLocation,
                    DictionaryAnnotator.PARAM_ANNOTATION_TYPE, Person.class);
            
            engines.add(nameFinder);
        }
        
        if(config.organizationDictionaryLocation != null){
            AnalysisEngineDescription organizationFinder = createEngineDescription(
                   DictionaryAnnotator.class,
                   DictionaryAnnotator.PARAM_MODEL_LOCATION, config.organizationDictionaryLocation,
                   DictionaryAnnotator.PARAM_ANNOTATION_TYPE, Organization.class);
            engines.add(organizationFinder);
        }
        
        AnalysisEngineDescription luceneWriter = createEngineDescription(
                Annotations2Lucene.class,
                Annotations2Lucene.PARAM_OWNER_CODE, config.userCode,
                Annotations2Lucene.PARAM_OWNER_NAME, config.userName,
                Annotations2Lucene.PARAM_CREATE_NEW_INDEX, config.createNewAnnotationIndex,
                Annotations2Lucene.PARAM_INDEX_PATH, ExpertProfilingPathUtil.getPath("document_annotation_index"));
        engines.add(luceneWriter);
        
        AnalysisEngineDescription timelineJsWriter = createEngineDescription(
                Lucene2ProfilePage.class,
                Lucene2ProfilePage.PARAM_OWNER_CODE, config.userCode,
                Lucene2ProfilePage.PARAM_OWNER_NAME, config.userName,
                Lucene2ProfilePage.PARAM_INDEX_PATH, ExpertProfilingPathUtil.getPath("document_annotation_index"),
                Lucene2ProfilePage.PARAM_VELOCITY_TEMPLATE_FILE, "/templates/profile.html",
                Lucene2ProfilePage.PARAM_TARGET_FILE, targetFile);
        engines.add(timelineJsWriter);
        
        if(config.useXmiDumper){
            AnalysisEngineDescription dumper = createEngineDescription(
                    XmiWriter.class,
                    XmiWriter.PARAM_TARGET_LOCATION, ExpertProfilingPathUtil.getPath("document_annotation_index") + "analysis/result",
                    XmiWriter.PARAM_TYPE_SYSTEM_FILE, ExpertProfilingPathUtil.getPath("document_annotation_index") + "analysis/type/TypeSystem.xml",
                    XmiWriter.PARAM_OVERWRITE, true);
            engines.add(dumper);
        }
       //@formatter:on

        SimplePipeline.runPipeline(reader, engines.toArray(new AnalysisEngineDescription[engines.size()]));

        stopWatch.stop();
        logger.info("Successfully completed process. Total processing time " + stopWatch + ".");

        return new File(targetFile);
    }

    public void runtimeParameters() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> aList = bean.getInputArguments();

        StringJoiner sj = new StringJoiner(" ");
        for (int i = 0; i < aList.size(); i++) {
            sj.add(aList.get(i));
        }
        
        String parameters =  sj.toString();
        
        if(parameters.length() > 0){
            logger.info("Running with " + sj.toString() + " VM parameters");
        } else {
            logger.info("Running without VM parameters");
        }
    }

}
