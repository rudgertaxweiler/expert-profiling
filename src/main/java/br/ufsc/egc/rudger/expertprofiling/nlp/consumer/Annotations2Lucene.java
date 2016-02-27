package br.ufsc.egc.rudger.expertprofiling.nlp.consumer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.join.BitDocIdSetCachingWrapperFilter;
import org.apache.lucene.search.join.ToChildBlockJoinQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.gson.Gson;

import br.ufsc.egc.rudger.expertprofiling.nlp.types.DbpediaCategory;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.Organization;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.Person;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;

public class Annotations2Lucene extends JCasConsumer_ImplBase implements LuceneIndexFields {

    public static final String PARAM_INDEX_PATH = "indexPath";
    @ConfigurationParameter(name = PARAM_INDEX_PATH, mandatory = true)
    private String indexPath;

    public static final String PARAM_OWNER_CODE = "ownerCode";
    @ConfigurationParameter(name = PARAM_OWNER_CODE, mandatory = true)
    private String ownerCode;

    public static final String PARAM_OWNER_NAME = "ownerName";
    @ConfigurationParameter(name = PARAM_OWNER_NAME, mandatory = true)
    private String ownerName;

    private static final String DOC_SEQUENCE = "docSequence";

    private static final String DOC_ANNOTATION_SEQUENCE = "docAnnotationSequence";

    private Properties props;

    private AtomicLong docSequence;

    private AtomicLong docAnnotationSequence;

    private Path conf;

    private IndexWriter writer;

    private Gson gson;

    private BitDocIdSetCachingWrapperFilter parentsFilter;

    private boolean needMerge;

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        Path index = Paths.get(this.indexPath);

        this.props = new Properties();

        this.conf = Paths.get(this.indexPath + FileSystems.getDefault().getSeparator() + "conf.properties");

        this.gson = new Gson();

        try {
            if (Files.exists(index)) {
                this.getLogger().info("Index directory found in '" + index.toFile() + "'.");
            } else {
                this.getLogger().info("Index directory not found in " + index.toFile() + ". Creating new directory.");
            }

            if (Files.exists(this.conf)) {
                try (InputStream in = Files.newInputStream(this.conf)) {
                    this.props.load(in);
                }
                this.docSequence = this.loadProperty(DOC_SEQUENCE);
                this.docAnnotationSequence = this.loadProperty(DOC_ANNOTATION_SEQUENCE);
            } else {
                this.docSequence = new AtomicLong(0);
                this.docAnnotationSequence = new AtomicLong(0);
            }

            FSDirectory indexDir = FSDirectory.open(index);
            this.writer = this.createWriter(indexDir);

            this.parentsFilter = LuceneQueryUtil.createParentJoinDocument();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.batchProcessComplete();

        if (this.needMerge) {
            try {
                this.writer.forceMerge(1);
                this.writer.close();
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    @Override
    public void process(final JCas jCas) throws AnalysisEngineProcessException {
        this.needMerge = true;

        List<Document> docs = new ArrayList<>();
        DocumentMetaData metaData = JCasUtil.selectSingle(jCas, DocumentMetaData.class);

        long documentId = this.docSequence.incrementAndGet();

        int sentenceDocSeq = 0;
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            sentenceDocSeq++;

            for (DbpediaCategory dbpedia : JCasUtil.selectCovered(jCas, DbpediaCategory.class, sentence)) {
                docs.add(this.getDocFromAnnotation(dbpedia, sentence, sentenceDocSeq, documentId));
            }

            for (Person person : JCasUtil.selectCovered(jCas, Person.class, sentence)) {
                docs.add(this.getDocFromAnnotation(person, sentence, sentenceDocSeq, documentId));
            }

            for (Organization organization : JCasUtil.selectCovered(jCas, Organization.class, sentence)) {
                docs.add(this.getDocFromAnnotation(organization, sentence, sentenceDocSeq, documentId));
            }

            for (Timex3 timex3 : JCasUtil.selectCovered(jCas, Timex3.class, sentence)) {
                docs.add(this.getDocFromAnnotation(timex3, sentence, sentenceDocSeq, documentId));
            }
        }

        InputStream in = null;
        try {
            Path path = Paths.get(new URI(metaData.getDocumentUri()));
            in = Files.newInputStream(path);

            String docMd5 = DigestUtils.md5Hex(in);

            String metadataJson = this.metadataToJson(JCasUtil.select(jCas, MetaDataStringField.class));
            docs.add(this.getDocFromAnnotation(metaData, path, new InputStreamReader(in), docMd5, documentId, metadataJson));

            this.writer.deleteDocuments(this.getDeleteChildrenQuery(this.ownerCode, docMd5));
            this.writer.deleteDocuments(this.getDeleteParentQuery(this.ownerCode, docMd5));

            this.writer.addDocuments(docs);
            this.writer.commit();
            this.saveProperties();
        } catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private String metadataToJson(final Collection<MetaDataStringField> properties) {
        Map<String, String> values = new HashMap<>();

        for (MetaDataStringField property : properties) {
            values.put(property.getKey(), property.getValue());
        }

        return this.gson.toJson(values);
    }

    private Document getDocFromAnnotation(final Timex3 timex3, final Sentence sentence, final int sentenceDocSeq, final long documentId) {
        Document doc = this.createDocAnnotation(timex3, sentence, sentenceDocSeq, documentId);

        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_ANNOTATION_TIMEX3_VALUE, timex3.getTimexValue(), Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_ANNOTATION_TIMEX3_TYPE, timex3.getType().getName(), Store.YES));

        return doc;
    }

    private Document getDocFromAnnotation(final Organization organization, final Sentence sentence, final int sentenceDocSeq, final long documentId) {
        return this.createDocAnnotation(organization, sentence, sentenceDocSeq, documentId);
    }

    private Document getDocFromAnnotation(final Person person, final Sentence sentence, final int sentenceDocSeq, final long documentId) {
        return this.createDocAnnotation(person, sentence, sentenceDocSeq, documentId);
    }

    private Document getDocFromAnnotation(final DbpediaCategory dbpedia, final Sentence sentence, final int sentenceDocSeq, final long documentId) {
        Document doc = this.createDocAnnotation(dbpedia, sentence, sentenceDocSeq, documentId);

        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_ANNOTATION_DBPEDIA_CATEGORY_URI, dbpedia.getUri(), Store.YES));

        return doc;
    }

    private Document getDocFromAnnotation(final DocumentMetaData metaData, final Path path, final Reader docContent, final String docMd5, final long documentId,
            final String propertiesJson) throws IOException {
        Document doc = new Document();

        BasicFileAttributes fileAtts = Files.readAttributes(path, BasicFileAttributes.class);

        doc.add(new LongField(LuceneIndexFields.FIELD_DOC_ID, documentId, Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_TITLE, metaData.getDocumentTitle(), Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_URI, metaData.getDocumentUri(), Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_LANGUAGE, metaData.getLanguage(), Store.YES));
        doc.add(new TextField(LuceneIndexFields.FIELD_DOC_CONTENT, docContent));
        doc.add(new TextField(LuceneIndexFields.FIELD_DOC_MD5, docMd5, Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_METADATA_JSON, propertiesJson, Store.YES));
        doc.add(new LongField(LuceneIndexFields.FIELD_DOC_CREATION_TIME, fileAtts.creationTime().toMillis(), Store.YES));
        doc.add(new LongField(LuceneIndexFields.FIELD_DOC_LAST_MODIFICATION_TIME, fileAtts.lastAccessTime().toMillis(), Store.YES));
        doc.add(new LongField(LuceneIndexFields.FIELD_DOC_LAST_ACCESS_TIME, fileAtts.lastModifiedTime().toMillis(), Store.YES));

        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_OWNER_CODE, this.ownerCode, Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_OWNER_NAME, this.ownerName, Store.YES));
        doc.add(new StringField(LuceneIndexFields.TYPE, LuceneIndexFields.Type.DOCUMENT.name(), Store.YES));

        return doc;
    }

    private Document createDocAnnotation(final Annotation annotation, final Sentence sentence, final int sentenceDocSeq, final Long documentId) {
        Document doc = new Document();

        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_ANNOTATION_TYPE, annotation.getClass().getName(), Store.YES));
        doc.add(new LongField(LuceneIndexFields.FIELD_DOC_ANNOTATION_ID, this.docAnnotationSequence.incrementAndGet(), Store.YES));
        doc.add(new LongField(LuceneIndexFields.FIELD_DOC_ANNOTATION_DOCUMENT_ID, documentId, Store.YES));
        doc.add(new IntField(LuceneIndexFields.FIELD_DOC_ANNOTATION_SENTENCE_SEQ, sentenceDocSeq, Store.YES));
        doc.add(new IntField(LuceneIndexFields.FIELD_DOC_ANNOTATION_SENTENCE_BEGIN, sentence.getBegin(), Store.YES));
        doc.add(new IntField(LuceneIndexFields.FIELD_DOC_ANNOTATION_SENTENCE_END, sentence.getEnd(), Store.YES));
        doc.add(new StringField(LuceneIndexFields.FIELD_DOC_ANNOTATION_VALUE, annotation.getCoveredText(), Store.YES));
        doc.add(new StringField(LuceneIndexFields.TYPE, LuceneIndexFields.Type.ANNOTATION.name(), Store.YES));

        return doc;
    }

    private void saveProperties() throws AnalysisEngineProcessException {
        this.setProperty(DOC_SEQUENCE, this.docSequence);
        this.setProperty(DOC_ANNOTATION_SEQUENCE, this.docAnnotationSequence);

        try (OutputStream out = Files.newOutputStream(this.conf)) {
            this.props.store(out, null);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void setProperty(final String key, final AtomicLong value) {
        this.props.put(key, String.valueOf(value.get()));
    }

    private AtomicLong loadProperty(final String key) {
        return new AtomicLong(new Long(this.props.getProperty(key)));
    }

    private Query getDeleteChildrenQuery(final String ownerCode, final String docMd5) {
        return new ToChildBlockJoinQuery(this.getDeleteParentQuery(ownerCode, docMd5), this.parentsFilter);
    }

    private Query getDeleteParentQuery(final String ownerCode, final String docMd5) {
        BooleanQuery parentsQuery = new BooleanQuery();
        parentsQuery.add(new TermQuery(new Term(LuceneIndexFields.FIELD_DOC_OWNER_CODE, ownerCode)), Occur.MUST);
        parentsQuery.add(new TermQuery(new Term(LuceneIndexFields.FIELD_DOC_MD5, docMd5)), Occur.MUST);

        return parentsQuery;
    }

    private IndexWriter createWriter(final Directory indexDir) throws IOException {
        IndexWriterConfig cfg = new IndexWriterConfig(new StandardAnalyzer());
        cfg.setRAMBufferSizeMB(48);
        return new IndexWriter(indexDir, cfg);
    }
}