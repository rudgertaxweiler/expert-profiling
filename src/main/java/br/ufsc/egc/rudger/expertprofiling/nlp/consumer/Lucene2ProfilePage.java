package br.ufsc.egc.rudger.expertprofiling.nlp.consumer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        ve.init();
        this.template = ve.getTemplate(this.velocityTemplateFile);
        this.parentsFilter = LuceneQueryUtil.createParentJoinDocument();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        File fileIndex = new File(this.indexPath);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");

        try {
            this.searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(fileIndex.toPath())));
        } catch (IOException e) {
            this.getLogger().info("No data to precess in '" + fileIndex.getAbsolutePath() + "'.");
        }

        if (this.searcher != null) {
            File fileDest = new File(this.targetFile);
            fileDest.getParentFile().mkdirs();
            
            try (FileWriter fw = new FileWriter(fileDest)) {
                VelocityContext context = new VelocityContext();

                Collection<DbPediaCategory> annotations = this.readAnnotationsFromIndex();

                //@formatter:off
                Collection<DbPediaCategory> tagCloud = annotations
                .stream()
                .sorted((o1, o2) -> new Long(o2.total - o1.total).intValue())
                .limit(100)
                .collect(Collectors.toList());
             

                Map<Long, List<DbPediaCategory>> timeline = annotations
                .stream()
                .collect(Collectors.groupingBy(DbPediaCategory::getDocumentLastModificationTime,
                        Collectors.collectingAndThen(Collectors.toList(),
                                l -> l.stream()
                                      .sorted((o1, o2) -> new Long(o2.total - o1.total).intValue())
                                      .limit(20).collect(Collectors.toList()))));
                
                //@formatter:on

                context.put("ownerName", this.ownerName);
                context.put("generatedDate", dateFormat.format(new Date()));
                context.put("timelineMap", timeline);
                context.put("dbpediaCategoryList", tagCloud);

                this.template.merge(context, fw);

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

    private Collection<DbPediaCategory> readAnnotationsFromIndex() throws IOException {
        Map<String, DbPediaCategory> result = new HashMap<>();

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
        return result.values();
    }

    private void processAnnotationsFromDocument(final UserDocument userDoc, final Map<String, DbPediaCategory> result) throws IOException {
        TopDocs searchDocuments = this.searcher.search(this.getAnnorationQuery(this.ownerCode, userDoc.id), Integer.MAX_VALUE);

        for (ScoreDoc scoreDoc : searchDocuments.scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = this.searcher.doc(docId);

            String uri = document.get(LuceneIndexFields.FIELD_DOC_ANNOTATION_DBPEDIA_CATEGORY_URI);

            if (uri != null) {
                int lastIndexOf = uri.lastIndexOf(':');
                String value = uri.substring(lastIndexOf + 1, uri.length()).replaceAll("_", " ");
                if (!StringUtils.isNumeric(value)) {

                    DbPediaCategory dbPediaCategory = result.get(value);
                    if (dbPediaCategory == null) {
                        dbPediaCategory = new DbPediaCategory();
                        dbPediaCategory.uri = uri;
                        dbPediaCategory.value = value;
                        dbPediaCategory.total = 0;

                        dbPediaCategory.documentId = userDoc.id;
                        dbPediaCategory.documentCreationTime = this.truncateToDate(userDoc.creationTime);
                        dbPediaCategory.documentLastModificationTime = this.truncateToDate(userDoc.lastModificationTime);
                        dbPediaCategory.documentLastAccessTime = this.truncateToDate(userDoc.lastAccessTime);

                        result.put(value, dbPediaCategory);
                    }
                    dbPediaCategory.total++;
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
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DST_OFFSET, 0);
        cal.set(Calendar.ZONE_OFFSET, 0);
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

    }

}