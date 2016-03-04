package br.ufsc.egc.rudger.expertprofiling.nlp.consumer;

public interface LuceneIndexFields {

    public enum Type {
        DOCUMENT,
        ANNOTATION
    }

    String TYPE = "type";

    /*
     * Document
     */
    String FIELD_DOC_ID = "doc_id";
    String FIELD_DOC_TITLE = "doc_title";
    String FIELD_DOC_URI = "doc_uri";
    String FIELD_DOC_LANGUAGE = "doc_language";
    String FIELD_DOC_CREATION_TIME = "doc_creation_time";
    String FIELD_DOC_LAST_MODIFICATION_TIME = "doc_last_modification_time";
    String FIELD_DOC_LAST_ACCESS_TIME = "doc_last_access_time";
    String FIELD_DOC_METADATA_JSON = "doc_metadata_json";
    String FIELD_DOC_MD5 = "doc_md5";
    String FIELD_DOC_CONTENT = "doc_content";

    /*
     * Document owner
     */

    String FIELD_DOC_OWNER_NAME = "doc_owner_name";
    String FIELD_DOC_OWNER_CODE = "doc_owner_code";

    /*
     * Annotation
     */
    String FIELD_DOC_ANNOTATION_TYPE = "doc_annotation_type";
    String FIELD_DOC_ANNOTATION_ID = "doc_annotation_id";
    String FIELD_DOC_ANNOTATION_DOCUMENT_ID = "doc_annotation_document_id";
    String FIELD_DOC_ANNOTATION_VALUE = "doc_annotation_value";
    String FIELD_DOC_ANNOTATION_SENTENCE_SEQ = "doc_annotation_sentence_seq";
    String FIELD_DOC_ANNOTATION_SENTENCE_BEGIN = "doc_annotation_sentence_begin";
    String FIELD_DOC_ANNOTATION_SENTENCE_END = "doc_annotation_sentence_end";

    /*
     * Annotation Time3
     */
    String FIELD_DOC_ANNOTATION_TIMEX3_VALUE = "doc_annotation_timex3_value";
    String FIELD_DOC_ANNOTATION_TIMEX3_TYPE = "doc_annotation_timex3_type";

    /*
     * Annotation DbPediaCategory
     */
    String FIELD_DOC_ANNOTATION_DBPEDIA_CATEGORY_URI = "doc_annotation_dbpedia_category_uri";

}
