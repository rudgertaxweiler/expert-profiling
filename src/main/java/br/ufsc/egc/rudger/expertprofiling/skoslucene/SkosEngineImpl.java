package br.ufsc.egc.rudger.expertprofiling.skoslucene;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

import br.ufsc.egc.rudger.expertprofiling.normalizer.Normalizer;

/**
 * SKOSEngine Implementation for Lucene. Each SKOS concept is stored/indexed as a Lucene document. All labels are
 * normalized.
 */
public class SkosEngineImpl implements SkosEngine, LuceneIndexFields {

    private static final Logger logger = LoggerFactory.getLogger(SkosEngineImpl.class);

    /**
     * Records the total number of matches
     */
    public static class AllDocCollector extends SimpleCollector {

        private final List<Integer> docs = new ArrayList<>();
        private int base;

        @Override
        public void setScorer(final Scorer scorer) throws IOException {
        }

        @Override
        public void collect(final int doc) throws IOException {
            this.docs.add(doc + this.base);
        }

        @Override
        protected void doSetNextReader(final LeafReaderContext context) throws IOException {
            this.base = context.docBase;
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        public List<Integer> getDocs() {
            return this.docs;
        }
    }

    /**
     * The input SKOS model
     */
    private Model skosModel;
    /**
     * The location of the concept index
     */
    private final Directory indexDir;
    /**
     * Provides access to the index
     */
    private IndexSearcher searcher;
    /**
     * The languages to be considered when returning labels.
     *
     * If NULL, all languages are supported
     */

    private Normalizer normalizer;

    public SkosEngineImpl(final File indexDir, final Normalizer normalizer) throws IOException {
        this.indexDir = FSDirectory.open(indexDir.toPath());
        this.normalizer = normalizer;
    }

    @Override
    public void indexModel(final InputStream inputStream, final String format) throws IOException {
        if (!("N3".equals(format) || "RDF/XML".equals(format) || "TURTLE".equals(format))) {
            throw new IOException("Invalid RDF serialization format");
        }

        this.skosModel = ModelFactory.createDefaultModel();
        this.skosModel.read(inputStream, null, format);

        this.entailSKOSModel();
        this.indexSKOSModel();
    }

    @Override
    public void createSearch() throws IOException {
        this.searcher = new IndexSearcher(DirectoryReader.open(this.indexDir));
    }

    private void entailSKOSModel() {
        GraphStore graphStore = GraphStoreFactory.create(this.skosModel);
        //@formatter:off
        String sparqlQuery = 
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" + 
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "INSERT { ?subject rdf:type skos:Concept }\n" + 
                "WHERE {\n" + "{ ?subject skos:prefLabel ?text } UNION\n" +
                    "{ ?subject skos:altLabel ?text } UNION\n" + 
                    "{ ?subject skos:hiddenLabel ?text }\n" + 
                "}";
        //@formatter:on
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateAction.execute(request, graphStore);
    }

    /**
     * Creates lucene documents from SKOS concept. In order to allow language restrictions, one document per language is
     * created.
     */
    private Document createDocumentsFromConcept(final Resource skos_concept) {
        Document conceptDoc = new Document();
        String conceptURI = skos_concept.getURI();
        Field uriField = new Field(FIELD_URI, conceptURI, StringField.TYPE_STORED);
        conceptDoc.add(uriField);
        // store the preferred lexical labels
        this.indexAnnotation(skos_concept, conceptDoc, Skos.prefLabel, FIELD_PREF_LABEL, false);
        // store the alternative lexical labels
        this.indexAnnotation(skos_concept, conceptDoc, Skos.altLabel, FIELD_ALT_LABEL, false);
        // store the hidden lexical labels
        this.indexAnnotation(skos_concept, conceptDoc, Skos.hiddenLabel, FIELD_HIDDEN_LABEL, false);
        // store the URIs of the broader concepts
        this.indexObject(skos_concept, conceptDoc, Skos.broader, FIELD_BROADER);
        // store the URIs of the broader transitive concepts
        this.indexObject(skos_concept, conceptDoc, Skos.broaderTransitive, FIELD_BROADER_TRANSITIVE);
        // store the URIs of the narrower concepts
        this.indexObject(skos_concept, conceptDoc, Skos.narrower, FIELD_NARROWER);
        // store the URIs of the narrower transitive concepts
        this.indexObject(skos_concept, conceptDoc, Skos.narrowerTransitive, FIELD_NARROWER_TRANSITIVE);
        // store the URIs of the related concepts
        this.indexObject(skos_concept, conceptDoc, Skos.related, FIELD_RELATED);

        // store the preferred lexical labels normalized
        this.indexAnnotation(skos_concept, conceptDoc, Skos.prefLabel, FIELD_PREF_LABEL_NORM, true);
        // store the alternative lexical labels normalized
        this.indexAnnotation(skos_concept, conceptDoc, Skos.altLabel, FIELD_ALT_LABEL_NORM, true);
        // store the hidden lexical labels normalized
        this.indexAnnotation(skos_concept, conceptDoc, Skos.hiddenLabel, FIELD_HIDDEN_LABEL_NORM, true);

        return conceptDoc;
    }

    @Override
    public Collection<String> getAltLabels(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_ALT_LABEL);
    }

    @Override
    public Collection<String> getAltTerms(final String label, final boolean normalize) throws IOException {
        Set<String> result = new HashSet<>();
        // convert the query to lower-case
        String queryString = label.toLowerCase(Locale.ROOT);
        try {
            Collection<String> conceptURIs = this.getConcepts(queryString, normalize);
            if (conceptURIs != null) {
                for (String conceptURI : conceptURIs) {
                    Collection<String> altLabels = this.getAltLabels(conceptURI);
                    if (altLabels != null) {
                        result.addAll(altLabels);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Collection<String> getHiddenLabels(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_HIDDEN_LABEL);
    }

    @Override
    public Collection<String> getBroaderConcepts(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_BROADER);
    }

    @Override
    public Collection<String> getBroaderLabels(final String conceptURI) throws IOException {
        return this.getLabels(conceptURI, FIELD_BROADER);
    }

    @Override
    public Collection<String> getBroaderTransitiveConcepts(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_BROADER_TRANSITIVE);
    }

    @Override
    public Collection<String> getBroaderTransitiveLabels(final String conceptURI) throws IOException {
        return this.getLabels(conceptURI, FIELD_BROADER_TRANSITIVE);
    }

    @Override
    public Collection<String> getConcepts(final String label, final boolean normalize) throws IOException {
        Set<String> concepts = new HashSet<>();
        String queryString = normalize ? this.normalizer.normalize(label) : label;
        AllDocCollector collector = new AllDocCollector();
       
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term(FIELD_PREF_LABEL_NORM, queryString)), Occur.SHOULD);
        query.add(new TermQuery(new Term(FIELD_ALT_LABEL_NORM, queryString)), Occur.SHOULD);
        query.add(new TermQuery(new Term(FIELD_HIDDEN_LABEL_NORM, queryString)), Occur.SHOULD);
        this.searcher.search(query, collector);
        
        for (Integer hit : collector.getDocs()) {
            Document doc = this.searcher.doc(hit);
            String conceptURI = doc.getValues(FIELD_URI)[0];
            concepts.add(conceptURI);
        }
        return concepts;
    }

    private Collection<String> getLabels(final String conceptURI, final String field) throws IOException {
        Set<String> labels = new HashSet<>();
        Collection<String> concepts = this.readConceptFieldValues(conceptURI, field);
        if (concepts != null) {
            for (String aConceptURI : concepts) {
                labels.addAll(this.getPrefLabels(aConceptURI));
                labels.addAll(this.getAltLabels(aConceptURI));
            }
        }
        return labels;
    }

    @Override
    public Collection<String> getNarrowerConcepts(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_NARROWER);
    }

    @Override
    public Collection<String> getNarrowerLabels(final String conceptURI) throws IOException {
        return this.getLabels(conceptURI, FIELD_NARROWER);
    }

    @Override
    public Collection<String> getNarrowerTransitiveConcepts(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_NARROWER_TRANSITIVE);
    }

    @Override
    public Collection<String> getNarrowerTransitiveLabels(final String conceptURI) throws IOException {
        return this.getLabels(conceptURI, FIELD_NARROWER_TRANSITIVE);
    }

    @Override
    public Collection<String> getPrefLabels(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_PREF_LABEL);
    }

    @Override
    public Collection<String> getRelatedConcepts(final String conceptURI) throws IOException {
        return this.readConceptFieldValues(conceptURI, FIELD_RELATED);
    }

    @Override
    public Collection<String> getRelatedLabels(final String conceptURI) throws IOException {
        return this.getLabels(conceptURI, FIELD_RELATED);
    }

    private void indexAnnotation(final Resource skos_concept, final Document conceptDoc, final AnnotationProperty property, final String field,
            final boolean normalized) {
        StmtIterator stmt_iter = skos_concept.listProperties(property);
        while (stmt_iter.hasNext()) {
            Literal labelLiteral = stmt_iter.nextStatement().getObject().as(Literal.class);
            String label = labelLiteral.getLexicalForm();

            if (normalized) {
                label = this.normalizer.normalize(label);
            }

            Field labelField = new Field(field, label, StringField.TYPE_STORED);

            conceptDoc.add(labelField);
        }
    }

    private void indexObject(final Resource skos_concept, final Document conceptDoc, final ObjectProperty property, final String field) {
        StmtIterator stmt_iter = skos_concept.listProperties(property);
        while (stmt_iter.hasNext()) {
            RDFNode concept = stmt_iter.nextStatement().getObject();
            if (!concept.canAs(Resource.class)) {
                logger.warn("Error when indexing relationship of concept " + skos_concept.getURI() + " .");
                continue;
            }
            Resource resource = concept.as(Resource.class);
            Field conceptField = new Field(field, resource.getURI(), StringField.TYPE_STORED);
            conceptDoc.add(conceptField);
        }
    }

    /**
     * Creates the synonym index
     *
     * @throws IOException
     */
    private void indexSKOSModel() throws IOException {
        IndexWriterConfig cfg = new IndexWriterConfig(new StandardAnalyzer());
        cfg.setRAMBufferSizeMB(48);
        IndexWriter writer = new IndexWriter(this.indexDir, cfg);

        /* iterate SKOS concepts, create Lucene docs and add them to the index */
        if (this.skosModel != null) {
            ResIterator concept_iter = this.skosModel.listResourcesWithProperty(RDF.type, Skos.Concept);

            while (concept_iter.hasNext()) {
                Resource skos_concept = concept_iter.next();
                Document concept_doc = this.createDocumentsFromConcept(skos_concept);
                writer.addDocument(concept_doc);
            }
        }

        writer.forceMerge(1);
        writer.close();
    }

    /**
     * Returns the values of a given field for a given concept
     */
    private Collection<String> readConceptFieldValues(final String conceptURI, final String field) throws IOException {
        Query query = new TermQuery(new Term(FIELD_URI, conceptURI));
        TopDocs docs = this.searcher.search(query, 1);
        ScoreDoc[] results = docs.scoreDocs;
        if (results.length != 1) {
            logger.warn("Unknown concept " + conceptURI);
            return null;
        }
        Document conceptDoc = this.searcher.doc(results[0].doc);
        return Arrays.asList(conceptDoc.getValues(field));
    }

}
