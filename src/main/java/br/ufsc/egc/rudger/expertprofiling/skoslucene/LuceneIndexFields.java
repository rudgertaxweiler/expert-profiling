package br.ufsc.egc.rudger.expertprofiling.skoslucene;

public interface LuceneIndexFields {

    String FIELD_URI = "uri";
    String FIELD_PREF_LABEL = "pref";
    String FIELD_ALT_LABEL = "alt";
    String FIELD_HIDDEN_LABEL = "hidden";
    String FIELD_BROADER = "broader";
    String FIELD_NARROWER = "narrower";
    String FIELD_BROADER_TRANSITIVE = "broader_transitive";
    String FIELD_NARROWER_TRANSITIVE = "narrower_transitive";
    String FIELD_RELATED = "related";

    String FIELD_PREF_LABEL_NORM = "pref_norm";
    String FIELD_ALT_LABEL_NORM = "alt_norm";
    String FIELD_HIDDEN_LABEL_NORM = "hidden_norm";

}
