package br.ufsc.egc.rudger.expertprofiling.nlp.dbpedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import br.ufsc.egc.rudger.expertprofiling.concurrent.ServiceThreadExecutor;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.DbpediaCategory;
import br.ufsc.egc.rudger.expertprofiling.normalizer.DefaultNormalizer;
import br.ufsc.egc.rudger.expertprofiling.normalizer.Normalizer;
import br.ufsc.egc.rudger.expertprofiling.skoslucene.SkosEngineImpl;
import br.ufsc.egc.rudger.expertprofiling.stopword.StopWordSet;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

//@formatter:off
@TypeCapability(
        inputs = {
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord"
            },
        outputs = {
            "br.org.stela.intelligentia.expertprofiling.nlp.types.DbpediaCategory" })
//@formatter:on
public class DbpediaAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_DBPEDIA_LINKS = "dbpediaLinks";
    @ConfigurationParameter(name = PARAM_DBPEDIA_LINKS, mandatory = true)
    private String[] dbpediaLinks;

    public static final String PARAM_DBPEDIA_INDEX_PATH = "dbpediaIndexPath";
    @ConfigurationParameter(name = PARAM_DBPEDIA_INDEX_PATH, mandatory = true)
    private String dbpediaIndexPath;

    public static final String PARAM_STOPWORD_LOCATION = ComponentParameters.PARAM_MODEL_LOCATION;
    @ConfigurationParameter(name = PARAM_STOPWORD_LOCATION, mandatory = false)
    private String[] swFileNames;

    public static final String PARAM_STOPWORD_ENCODING = ComponentParameters.PARAM_MODEL_ENCODING;
    @ConfigurationParameter(name = PARAM_STOPWORD_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String swFilesEncoding;

    private static final String MAX_TOKENS = "maxTokens";

    private SkosEngineImpl skosEngine;

    private Set<String> stopwords;

    private TokenCounterNormalizerWrapper normalizer;

    private int maxTokens;

    private Properties props;

    /*
     * Public API
     */

    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        this.props = new Properties();

        File index = new File(this.dbpediaIndexPath + "_" + this.createHash());
        File conf = new File(index.getAbsoluteFile() + File.separator + "conf.properties");

        try {

            this.stopwords = this.readStopWords();

            this.normalizer = this.createNormalizerWrapper();

            if (index.exists() && conf.exists()) {
                this.getLogger().info("Index directory found. Loading data from '" + index.getAbsolutePath() + "'.");
                this.skosEngine = new SkosEngineImpl(index, this.normalizer);
                this.skosEngine.createSearch();

                try (InputStream in = new FileInputStream(conf)) {
                    this.props.load(in);
                }
                this.maxTokens = new Integer(this.props.getProperty(MAX_TOKENS));
            } else {
                FileUtils.deleteQuietly(index);
                
                this.getLogger().info("Index directory not found in '" + index.getAbsolutePath() + "'. Downloading the SKOS data from DBpedia.");

                index.mkdirs();
                this.skosEngine = new SkosEngineImpl(index, this.normalizer);

                for (String url : this.dbpediaLinks) {
                    BZip2CompressorInputStream bzip = null;
                    try {
                        InputStream in = new URL(url).openStream();
                        bzip = new BZip2CompressorInputStream(in);

                        this.skosEngine.indexModel(bzip, "N3");

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        IOUtils.closeQuietly(bzip);
                    }
                }

                this.skosEngine.createSearch();

                this.maxTokens = this.normalizer.maxTokens;

                this.props.put(MAX_TOKENS, String.valueOf(this.maxTokens));

                try (Writer out = new FileWriter(conf)) {
                    this.props.store(new FileWriter(conf), null);
                }
            }
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(final JCas jCas) throws AnalysisEngineProcessException {
        ServiceThreadExecutor executor = ServiceThreadExecutor.newProcessorsThreadPool(100);
        List<LongestMatchResult> annotations = new Vector<>();
        try {
            for (Sentence currSentence : JCasUtil.select(jCas, Sentence.class)) {
                executor.submit(() -> {
                    try {
                        this.processSentence(jCas, currSentence, annotations);
                    } catch (AnalysisEngineProcessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

                for (LongestMatchResult longestMatchResult : annotations) {
                    for (String conceptUri : longestMatchResult.concepts) {
                        DbpediaCategory dbpediaCategory = new DbpediaCategory(jCas, longestMatchResult.begin, longestMatchResult.end);
                        dbpediaCategory.setUri(conceptUri);
                        dbpediaCategory.addToIndexes();
                    }
                }

            } catch (InterruptedException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    private void processSentence(final JCas jCas, final Sentence currSentence, final List<LongestMatchResult> annotations)
            throws AnalysisEngineProcessException {
        ArrayList<Token> tokens = new ArrayList<Token>(JCasUtil.selectCovered(Token.class, currSentence));

        int ngram = Math.min(tokens.size(), this.maxTokens);

        int i = 0;
        do {
            int end = Math.min(i + ngram, tokens.size());
            List<String> tokensNgram = new ArrayList<String>(end - i);

            for (int j = i; j < end; j++) {
                tokensNgram.add(tokens.get(j).getCoveredText());
            }

            LongestMatchResult longestMatch = null;
            try {
                longestMatch = this.getLongestMatch(tokensNgram);
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }

            if (longestMatch != null) {
                int lastTokenPos = i + longestMatch.tokens.size() - 1;

                Token beginToken = tokens.get(i);
                Token endToken = tokens.get(lastTokenPos);

                if (JCasUtil.selectAt(jCas, StopWord.class, beginToken.getBegin(), endToken.getEnd()).isEmpty()) {

                    longestMatch.begin = beginToken.getBegin();
                    longestMatch.end = beginToken.getEnd();

                    annotations.add(longestMatch);
                }
                i = lastTokenPos; // move to the found concept position
            }

            i++;
        } while (i < tokens.size());
    }

    /*
     * Private methods
     */

    private TokenCounterNormalizerWrapper createNormalizerWrapper() {
        if (this.stopwords != null) {
            return new TokenCounterNormalizerWrapper(new StopWordNormalizer(this.stopwords));
        }

        return new TokenCounterNormalizerWrapper(new DefaultNormalizer());
    }

    private LongestMatchResult getLongestMatch(final List<String> tokens) throws IOException {
        for (int i = tokens.size(); i >= 1; i--) {
            List<String> tokensToSentenceBegin = tokens.subList(0, i);

            Collection<String> concepts = this.skosEngine.getConcepts(StringUtils.join(tokensToSentenceBegin, " "), false);

            if (!concepts.isEmpty()) {
                LongestMatchResult result = new LongestMatchResult();
                result.tokens = tokensToSentenceBegin;
                result.concepts = concepts;

                return result;
            }
        }

        return null;
    }

    private String createHash() {
        List<String> identifiers = new ArrayList<String>();
        identifiers.addAll(Arrays.asList(this.dbpediaLinks));

        if (this.swFileNames != null) {
            identifiers.addAll(Arrays.asList(this.swFileNames));
        }

        Collections.sort(identifiers);

        return DigestUtils.md5Hex(StringUtils.join(identifiers.toArray()));
    }

    private Set<String> readStopWords() throws IOException {
        if (this.swFileNames != null) {
            return new StopWordSet(this.swFileNames).getData();
        }

        return null;
    }

    private class LongestMatchResult {
        List<String> tokens;
        Collection<String> concepts;

        int begin;
        int end;
    }

    private class TokenCounterNormalizerWrapper implements Normalizer {

        int maxTokens;

        Normalizer normalizer;

        TokenCounterNormalizerWrapper(final Normalizer normalizer) {
            this.normalizer = normalizer;
        }

        @Override
        public String normalize(final String value) {
            String normalize = this.normalizer.normalize(value);

            this.maxTokens = Math.max(this.maxTokens, normalize.split(" ").length);

            return normalize;
        }

    }

}
