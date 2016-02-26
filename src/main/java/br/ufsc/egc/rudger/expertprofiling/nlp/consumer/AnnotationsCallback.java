package br.ufsc.egc.rudger.expertprofiling.nlp.consumer;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import br.ufsc.egc.rudger.expertprofiling.nlp.types.DbpediaCategory;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.Organization;
import br.ufsc.egc.rudger.expertprofiling.nlp.types.Person;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;

public class AnnotationsCallback extends JCasConsumer_ImplBase {

    private static ThreadLocal<Callback> TL = new ThreadLocal<AnnotationsCallback.Callback>();

    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        Callback callback = TL.get();
        if (callback != null) {

            for (Sentence sentence : JCasUtil.select(aJCas, Sentence.class)) {
                for (DbpediaCategory dbpedia : JCasUtil.selectCovered(aJCas, DbpediaCategory.class, sentence)) {
                    callback.onSelect(sentence, dbpedia);
                }

                for (Person person : JCasUtil.selectCovered(aJCas, Person.class, sentence)) {
                    callback.onSelect(sentence, person);
                }

                for (Organization organization : JCasUtil.selectCovered(aJCas, Organization.class, sentence)) {
                    callback.onSelect(sentence, organization);
                }

                for (Timex3 timex3 : JCasUtil.selectCovered(aJCas, Timex3.class, sentence)) {
                    callback.onSelect(sentence, timex3);
                }
            }

            TL.remove();
        }
    }

    public static void setCallbackThreadLocal(final Callback callback) {
        TL.set(callback);
    }

    public static interface Callback {

        void onSelect(Sentence sentence, DbpediaCategory annotation);

        void onSelect(Sentence sentence, Person annotation);

        void onSelect(Sentence sentence, Organization annotation);

        void onSelect(Sentence sentence, Timex3 annotation);

    }
}