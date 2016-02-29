package br.ufsc.egc.rudger.expertprofiling.nlp.consumer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.join.BitDocIdSetCachingWrapperFilter;
import org.apache.lucene.search.join.ToChildBlockJoinQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class Lucene2ProfilePage extends JCasConsumer_ImplBase implements LuceneIndexFields {

    public static final String PARAM_OWNER_CODE = "ownerCode";
    @ConfigurationParameter(name = PARAM_OWNER_CODE, mandatory = true)
    private String ownerCode;

    public static final String PARAM_OWNER_NAME = "ownerName";
    @ConfigurationParameter(name = PARAM_OWNER_NAME, mandatory = true)
    private String ownerName;

    public static final String PARAM_INDEX_PATH = "indexPath";
    @ConfigurationParameter(name = PARAM_INDEX_PATH, mandatory = true)
    private String indexPath;

    public static final String PARAM_VELOCITY_TEMPLATE_FILE = "velocityTemplateFile";
    @ConfigurationParameter(name = PARAM_VELOCITY_TEMPLATE_FILE, mandatory = true)
    private String velocityTemplateFile;

    public static final String PARAM_TARGET_FILE = "targetFile";
    @ConfigurationParameter(name = PARAM_TARGET_FILE, mandatory = true)
    private String targetFile;

    private Template template;

    private IndexSearcher searcher;

    private BitDocIdSetCachingWrapperFilter parentsFilter;

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        ve.setProperty(RuntimeConstants.OUTPUT_ENCODING, "UTF-8");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        this.template = ve.getTemplate(this.velocityTemplateFile);
        this.parentsFilter = LuceneQueryUtil.createParentJoinDocument();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        File fileIndex = new File(this.indexPath);

        try {
            this.searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(fileIndex.toPath())));
        } catch (IOException e) {
            this.getLogger().info("No data to precess in '" + fileIndex.getAbsolutePath() + "'.");
        }

        if (this.searcher != null) {
            File fileDest = new File(this.targetFile);
            fileDest.getParentFile().mkdirs();
            
            try ( OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(fileDest),"UTF-8")) {
                VelocityContext context = new VelocityContext();

                List<DbPediaCategory> annotations = this.readAnnotationsFromIndex();

                //@formatter:off
                Map<String, Long> tagCloud = annotations
                        .stream()
                        .map(DbPediaCategory::getUri)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Long> comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                        .limit(100)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
                Map<Long, Map<String, Long>> timeline = annotations
                        .stream()
                        .collect(Collectors.groupingBy(DbPediaCategory::getDocumentLastModificationTime, 
                                Collectors.collectingAndThen(Collectors.groupingBy(DbPediaCategory::getUri, Collectors.counting()), 
                                        m -> m.entrySet()
                                        .stream()
                                        .sorted(Map.Entry.<String, Long> comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                                        .limit(100)
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2)-> v1, LinkedHashMap<String, Long>::new))))); 
                
                timeline = timeline
                .entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Map<String, Long>> comparingByKey(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2)-> v1, LinkedHashMap<Long, Map<String, Long>>::new));
                
                Map<String, String> dbpediaCategories = annotations
                        .stream()
                        .collect(Collectors.toMap(DbPediaCategory::getUri, DbPediaCategory::getValue, (v1, v2) -> v1));
                //@formatter:on

                context.put("ownerName", this.ownerName);
                context.put("generatedDate", new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(new Date()));
                context.put("tagcloud", tagCloud);
                context.put("timeline", timeline);
                context.put("dbpediaCategories", dbpediaCategories);
                context.put("dateUtil", new DateUtil(new SimpleDateFormat("yyyy")));

                this.template.merge(context, osw);

                this.getLogger().info("Profile data file created in '" + fileDest.getAbsolutePath() + "'.");
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    @Override
    public void process(final JCas jCas) throws AnalysisEngineProcessException {
        // NOOP
    }

    private List<DbPediaCategory> readAnnotationsFromIndex() throws IOException {
        List<DbPediaCategory> result = new ArrayList<>();

        TopDocs searchDocuments = this.searcher.search(this.getDocumentsQuery(this.ownerCode), Integer.MAX_VALUE);

        for (ScoreDoc scoreDoc : searchDocuments.scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = this.searcher.doc(docId);

            UserDocument userDoc = new UserDocument();

            userDoc.id = document.getField(LuceneIndexFields.FIELD_DOC_ID).numericValue().longValue();
            userDoc.creationTime = document.getField(LuceneIndexFields.FIELD_DOC_CREATION_TIME).numericValue().longValue();
            userDoc.lastModificationTime = document.getField(LuceneIndexFields.FIELD_DOC_LAST_MODIFICATION_TIME).numericValue().longValue();
            userDoc.lastAccessTime = document.getField(LuceneIndexFields.FIELD_DOC_LAST_ACCESS_TIME).numericValue().longValue();

            this.processAnnotationsFromDocument(userDoc, result);
        }
        return result;
    }

    private void processAnnotationsFromDocument(final UserDocument userDoc, final List<DbPediaCategory> result) throws IOException {
        TopDocs searchDocuments = this.searcher.search(this.getAnnorationQuery(this.ownerCode, userDoc.id), Integer.MAX_VALUE);

        for (ScoreDoc scoreDoc : searchDocuments.scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = this.searcher.doc(docId);

            String uri = document.getField(LuceneIndexFields.FIELD_DOC_ANNOTATION_DBPEDIA_CATEGORY_URI).stringValue();

            if (uri != null) {
                int lastIndexOf = uri.lastIndexOf(':');
                String value = uri.substring(lastIndexOf + 1, uri.length()).replaceAll("_", " ");
                if (!StringUtils.isNumeric(value)) {
                    DbPediaCategory dbPediaCategory = new DbPediaCategory();
                    dbPediaCategory.uri = uri;
                    dbPediaCategory.value = value;

                    dbPediaCategory.documentId = userDoc.id;
                    dbPediaCategory.documentCreationTime = this.truncateToDate(userDoc.creationTime);
                    dbPediaCategory.documentLastModificationTime = this.truncateToDate(userDoc.lastModificationTime);
                    dbPediaCategory.documentLastAccessTime = this.truncateToDate(userDoc.lastAccessTime);
                    result.add(dbPediaCategory);
                }
            }
        }
    }

    private Query getDocumentsQuery(final String ownerCode) {
        return new TermQuery(new Term(LuceneIndexFields.FIELD_DOC_OWNER_CODE, ownerCode));
    }

    private Query getDocumentQuery(final String ownerCode, final Long docId) {
        BytesRefBuilder refBuilder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(docId, 0, refBuilder);

        BooleanQuery parentsQuery = new BooleanQuery();
        parentsQuery.add(new TermQuery(new Term(LuceneIndexFields.FIELD_DOC_OWNER_CODE, ownerCode)), Occur.MUST);
        parentsQuery.add(new TermQuery(new Term(LuceneIndexFields.FIELD_DOC_ID, refBuilder.toBytesRef())), Occur.MUST);
        return parentsQuery;
    }

    private Query getAnnorationQuery(final String ownerCode, final Long docId) {
        return new ToChildBlockJoinQuery(this.getDocumentQuery(ownerCode, docId), this.parentsFilter);
    }

    private long truncateToDate(final long dateAndTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateAndTime);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DST_OFFSET, 0);
        cal.set(Calendar.ZONE_OFFSET, 0);

        // rounding to last day of year
        cal.add(Calendar.YEAR, 1);
        cal.add(Calendar.MILLISECOND, 1);
        return cal.getTimeInMillis();
    }

    public static class UserDocument {
        private long id;
        private long creationTime;
        private long lastAccessTime;
        private long lastModificationTime;

        public long getId() {
            return this.id;
        }

        public long getCreationTime() {
            return this.creationTime;
        }

        public long getLastAccessTime() {
            return this.lastAccessTime;
        }

        public long getLastModificationTime() {
            return this.lastModificationTime;
        }

    }

    public static class DbPediaCategory {
        private String value;
        private String uri;
        private long total;
        private long documentId;
        private long documentCreationTime;
        private long documentLastAccessTime;
        private long documentLastModificationTime;

        public String getValue() {
            return this.value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getUri() {
            return this.uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        public long getTotal() {
            return this.total;
        }

        public void setTotal(final long total) {
            this.total = total;
        }

        public long getDocumentId() {
            return this.documentId;
        }

        public long getDocumentCreationTime() {
            return this.documentCreationTime;
        }

        public long getDocumentLastAccessTime() {
            return this.documentLastAccessTime;
        }

        public long getDocumentLastModificationTime() {
            return this.documentLastModificationTime;
        }

        public void setDocumentId(final long documentId) {
            this.documentId = documentId;
        }

        public void setDocumentCreationTime(final long documentCreationTime) {
            this.documentCreationTime = documentCreationTime;
        }

        public void setDocumentLastAccessTime(final long documentLastAccessTime) {
            this.documentLastAccessTime = documentLastAccessTime;
        }

        public void setDocumentLastModificationTime(final long documentLastModificationTime) {
            this.documentLastModificationTime = documentLastModificationTime;
        }
    }

    public static class DateUtil {

        private SimpleDateFormat sdf;

        public DateUtil(final SimpleDateFormat sdf) {
            this.sdf = sdf;
        }

        public String format(final long date) {
            return this.sdf.format(new Date(date));
        }

    }

}