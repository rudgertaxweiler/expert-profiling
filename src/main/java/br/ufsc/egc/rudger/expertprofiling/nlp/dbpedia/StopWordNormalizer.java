package br.ufsc.egc.rudger.expertprofiling.nlp.dbpedia;

import java.util.Set;
import java.util.StringJoiner;

import br.ufsc.egc.rudger.expertprofiling.normalizer.DefaultNormalizer;

public class StopWordNormalizer extends DefaultNormalizer {
    
    private Set<String> stopwords;

    public StopWordNormalizer(final Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    @Override
    public String normalize(final String value) {
        String text = super.normalize(value);
        
        String[] words = text.split("\\s");
        StringJoiner result = new StringJoiner(" ");
        
        for (String word : words){
            if(!this.stopwords.contains(word)){
                result.add(word);
            }
        }
        
        return result.toString();
    }

}
