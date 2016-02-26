package br.ufsc.egc.rudger.expertprofiling.nlp.heideltime;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;

//@formatter:off
@TypeCapability(
        inputs = {
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
            },
        outputs = {
            "de.unihd.dbs.uima.types.heideltime.Token",
            "de.unihd.dbs.uima.types.heideltime.Sentence",
            "de.unihd.dbs.uima.types.heideltime.Timex3"})
//@formatter:on
public class HeidelTimeWrapper extends HeidelTime {

    public static final String PARAM_LANGUAGE = "Language";
    public static final String PARAM_TYPE_TO_PROCESS = "Type";
    public static final String PARAM_LOCALE = "locale";
    public static final String PARAM_DATE = "Date";
    public static final String PARAM_TIME = "Time";
    public static final String PARAM_DURATION = "Duration";
    public static final String PARAM_SET = "Set";
    public static final String PARAM_DEBUG = "Debugging";
    public static final String PARAM_GROUP = "ConvertDurations";
    
    
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(final JCas jCas) {
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            jCas.getCas().addFsToIndexes(this.sentenceToHeideltimeSetence(sentence, jCas));
        }

        for (Token token : JCasUtil.select(jCas, Token.class)) {
            jCas.getCas().addFsToIndexes(this.tokenToHeideltimeToken(token, jCas));
        }

        super.process(jCas);
    }

    private de.unihd.dbs.uima.types.heideltime.Sentence sentenceToHeideltimeSetence(final Sentence sentence, final JCas jcas) {
        de.unihd.dbs.uima.types.heideltime.Sentence hts = new de.unihd.dbs.uima.types.heideltime.Sentence(jcas);

        hts.setBegin(sentence.getBegin());
        hts.setEnd(sentence.getEnd());

        return hts;
    }

    private de.unihd.dbs.uima.types.heideltime.Token tokenToHeideltimeToken(final Token token, final JCas jcas) {
        de.unihd.dbs.uima.types.heideltime.Token htk = new de.unihd.dbs.uima.types.heideltime.Token(jcas);

        htk.setBegin(token.getBegin());
        htk.setEnd(token.getEnd());

        return htk;
    }

}