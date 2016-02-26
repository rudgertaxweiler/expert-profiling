package br.ufsc.egc.rudger.expertprofiling.normalizer;

public class DefaultNormalizer implements Normalizer {
    
    @Override
    public String normalize(final String value) {
        String text = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD);
        text = text.replaceAll("[^\\p{ASCII}]", "");
       
        return text.toLowerCase();
    }

}
