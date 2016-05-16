package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

/**
 * Maps case ID to execution ID.
 */
public class ImageExecutionMapping {

    private BasicDBObject metadataDoc;

    /**
     * Query the 'metadata' collection.
     */
    public boolean checkExists(ResultsDatabase db, String executionIdentifier, String imageCaseID) {

        BasicDBObject imgQuery = new BasicDBObject();
        imgQuery.put("image.caseid", imageCaseID);
        imgQuery.put("provenance.analysis_execution_id",
                executionIdentifier);
        DBCursor cursor = db.submitAnalysisExecutionMappingQuery(imgQuery);

        return cursor.size() != 0;

    }

    /**
     * GET metadataDoc.
     */
    public BasicDBObject getMetadataDoc() {
        return metadataDoc;
    }

    /**
     * SET metadataDoc.
     */
    public void setMetadataDoc(AnalysisExecutionMetadata execMeta,
                               SimpleImageMetadata imgMeta, String color) {

        metadataDoc = new BasicDBObject();
        metadataDoc.put("color", color);
        metadataDoc.put("title", execMeta.getTitle());

        BasicDBObject imgmeta_doc = new BasicDBObject();
        imgmeta_doc.put("subjectid", imgMeta.getSubjectid());
        imgmeta_doc.put("caseid", imgMeta.getCaseid());

        BasicDBObject provenance_doc = new BasicDBObject();
        provenance_doc.put("analysis_execution_id",
                execMeta.getIdentifier());
        provenance_doc.put("type", execMeta.getSource());

        metadataDoc.put("image", imgmeta_doc);
        metadataDoc.put("provenance", provenance_doc);

    }

}
