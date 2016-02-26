package br.ufsc.egc.rudger.expertprofiling.nlp.stopword;

import static de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils.resolveLocation;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import br.ufsc.egc.rudger.expertprofiling.normalizer.DefaultNormalizer;
import br.ufsc.egc.rudger.expertprofiling.stopword.StopWordSet;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

//@formatter:off
@TypeCapability(
      inputs = {
          "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
          "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
          },
      outputs = {
          "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord" })
//@formatter:on
public class StopWordAnnotator extends JCasAnnotator_ImplBase {

    /**
     * A list of URLs from which to load the stop word lists. If an URL is prefixed with a language code in square
     * brackets, the stop word list is only used for documents in that language. Using no prefix or the prefix "[*]"
     * causes the list to be used for every document. Example: "[de]classpath:/stopwords/en_articles.txt"
     */
    public static final String PARAM_MODEL_LOCATION = ComponentParameters.PARAM_MODEL_LOCATION;
    @ConfigurationParameter(name = PARAM_MODEL_LOCATION, mandatory = true)
    private Set<String> swFileNames;

    /**
     * The character encoding used by the model.
     */
    public static final String PARAM_MODEL_ENCODING = ComponentParameters.PARAM_MODEL_ENCODING;
    @ConfigurationParameter(name = PARAM_MODEL_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String modelEncoding;

    private Map<String, StopWordSet> stopWordSets;

    private DefaultNormalizer normalizer;

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        try {
            this.stopWordSets = new HashMap<String, StopWordSet>();
            this.normalizer = new DefaultNormalizer();

            for (String swFileName : this.swFileNames) {
                String fileLocale = "*";
                // Check if a locale is defined for the file
                if (swFileName.startsWith("[")) {
                    fileLocale = swFileName.substring(1, swFileName.indexOf(']'));
                    swFileName = swFileName.substring(swFileName.indexOf(']') + 1);
                }

                // Fetch the set for the specified locale
                StopWordSet set = this.stopWordSets.get(fileLocale);
                if (set == null) {
                    set = new StopWordSet();
                    this.stopWordSets.put(fileLocale, set);
                }

                // Load the set
                URL source = resolveLocation(swFileName, this, context);
                InputStream is = null;
                try {
                    is = source.openStream();
                    set.load(is, this.modelEncoding);
                } finally {
                    closeQuietly(is);
                }

                this.getLogger().info("Loaded stopwords for locale [" + fileLocale + "] from [" + source + "]");
            }
        } catch (IOException e1) {
            throw new ResourceInitializationException(e1);
        }
    }

    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        StopWordSet anyLocaleSet = this.stopWordSets.get("*");
        StopWordSet casLocaleSet = this.stopWordSets.get(aJCas.getDocumentLanguage());

        if (anyLocaleSet != null || casLocaleSet != null) {
            for (Token token : JCasUtil.select(aJCas, Token.class)) {
                String candidate = this.normalizer.normalize(token.getCoveredText().toLowerCase());
                if ((casLocaleSet != null && casLocaleSet.contains(candidate)) || (anyLocaleSet != null && anyLocaleSet.contains(candidate))) {
                    aJCas.getCas().addFsToIndexes(new StopWord(aJCas, token.getBegin(), token.getEnd()));
                }
            }
        }

    }

}
