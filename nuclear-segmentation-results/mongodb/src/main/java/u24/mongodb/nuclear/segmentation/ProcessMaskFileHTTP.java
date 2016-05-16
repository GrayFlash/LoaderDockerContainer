package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.opencv.core.Point;
import u24.masktopoly.MaskToPoly;
import u24.masktopoly.PolygonData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

public class ProcessMaskFileHTTP implements ProcessFile {
    private String fileName;
    private AnalysisExecutionMetadata executionMetadata;
    private String colorVal;
    private int shift_x, shift_y;
    private String caseID;
    private String subjectID;
    private ResultsDatabaseHTTP inpDB;
    private int img_width, img_height;
    private boolean getFromDB;
    private boolean normalize;
    private boolean fromself;
    private ResultsDatabaseHTTP outDB;
    private FileWriter outFileWriter;
    private BufferedWriter bufferedWriter;
    private MaskToPoly maskToPoly;
    private ImageExecutionMapping imgExecMap;

    SimpleImageMetadata imgMeta;
    private double min_x, min_y, max_x, max_y;

    public ProcessMaskFileHTTP(String fileName,
                               AnalysisExecutionMetadata executionMetadata,
                               int shift_x, int shift_y,
                               ResultsDatabaseHTTP outDB) {
        this.fileName = fileName;
        this.executionMetadata = executionMetadata;
        this.colorVal = "yellow";
        this.shift_x = shift_x;
        this.shift_y = shift_y;
        this.outDB = outDB;
        this.outFileWriter = null;
        this.bufferedWriter = null;

        this.caseID = "Undefined";
        this.subjectID = "Undefined";
        this.inpDB = null;
        this.getFromDB = false;
        this.img_width = 1;
        this.img_width = 1;
        this.normalize = false;
        this.fromself = false;
        this.imgExecMap = new ImageExecutionMapping();
        this.maskToPoly = new MaskToPoly();
        this.imgMeta = new SimpleImageMetadata();
    }

    public ProcessMaskFileHTTP(String fileName,
                               AnalysisExecutionMetadata executionMetadata,
                               int shift_x, int shift_y,
                               FileWriter outFileWriter) {
        this.fileName = fileName;
        this.executionMetadata = executionMetadata;
        this.colorVal = "yellow";
        this.shift_x = shift_x;
        this.shift_y = shift_y;
        this.outDB = null;
        this.outFileWriter = outFileWriter;
        this.bufferedWriter = new BufferedWriter(outFileWriter);

        this.caseID = "Undefined";
        this.subjectID = "Undefined";
        this.inpDB = null;
        this.getFromDB = false;
        this.img_width = 1;
        this.img_width = 1;
        this.normalize = false;
        this.fromself = false;
        this.imgExecMap = new ImageExecutionMapping();
        this.maskToPoly = new MaskToPoly();
        this.imgMeta = new SimpleImageMetadata();
    }

    public void setCaseID(String caseID) {
        this.caseID = caseID;
    }

    public void setColor(String colorVal) {
        this.colorVal = colorVal;
    }

    public void setImgMetaFromDB(ResultsDatabaseHTTP inpDB) {
        this.getFromDB = true;
        this.inpDB = inpDB;
    }

    public void doNormalization(boolean normalize, boolean fromself) {
        this.normalize = normalize;
        this.fromself = fromself;
    }

    void normalizePoints(Point[] points) {
        for (int i = 0; i < points.length; i++) {
            points[i].x = (points[i].x + shift_x) / img_width;
            points[i].y = (points[i].y + shift_y) / img_height;
        }
    }


    boolean setImageMetadata() {
        if (getFromDB) {
            String imgDocument = inpDB.getImageObject(caseID);
            DBObject qryResult = (DBObject) JSON.parse(imgDocument);
            if (qryResult == null) {
                System.err.println("ERROR: Cannot find caseid: " + caseID);
                return false;
            }

            img_width = (int) (Double.parseDouble(qryResult.get("width")
                    .toString()));
            img_height = (int) (Double.parseDouble(qryResult.get("height")
                    .toString()));

            if (qryResult.get("subject_id") != null) {
                subjectID = qryResult.get("subject_id").toString();
            }

            // Check if dimensions are negative or zero
            if (img_width <= 0 || img_height <= 0) {
                System.err.println("ERROR: Image dimensions are wrong: ("
                        + img_width + "x" + img_height + ")");
                return false;
            }
        }

        imgMeta.setIdentifier(caseID);
        imgMeta.setCaseid(caseID);
        imgMeta.setSubjectid(subjectID);
        imgMeta.setWidth((double) img_width);
        imgMeta.setHeight((double) img_height);

        return true;
    }

    public void processFile() {
        try {
            if (!setImageMetadata())
                return;

            // Extract polygons from the mask file
            maskToPoly.readMask(fileName);
            maskToPoly.extractPolygons();
            if (fromself) {
                this.img_width = maskToPoly.getImgWidth();
                this.img_height = maskToPoly.getImgHeight();
            }

            if (outDB != null) {
                // Check and register image to analysis mapping information
                imgExecMap.setMetadataDoc(executionMetadata, imgMeta, colorVal);
                String case_id = imgMeta.getCaseid();
                String exec_id = executionMetadata.getIdentifier();
                if (!outDB.checkMetadataExists(case_id, exec_id)) {
                    System.out.println("SUBMITTING MAPPING");
                    outDB.submitMetadataDocument(imgExecMap.getMetadataDoc().toString());
                } else {
                    System.out.println("NO SUBMISSION");
                }
            }

            List<PolygonData> polygons = maskToPoly.getPolygons();
            PolygonData polygon;
            Point[] points;
            for (int i = 0; i < polygons.size(); i++) {
                polygon = polygons.get(i);
                points = polygon.points;
                if (normalize)
                    normalizePoints(points);

                Poinsettia pointSetter = new Poinsettia();
                pointSetter.computeBoundingBox(points);
                min_x = pointSetter.getMin_x();
                min_y = pointSetter.getMin_y();
                max_x = pointSetter.getMax_x();
                max_y = pointSetter.getMax_y();

                Image2DMarkupGeoJSON obj_2d = new Image2DMarkupGeoJSON();

                // Get polygon points
                BasicDBList objPointsList = pointSetter.getPolygonPoints(points);

                // Check markup data
                if (objPointsList.size() > 0) {
                    // Set markup data
                    obj_2d.setMarkup(min_x,
                            min_y, max_x, max_y, "Polygon", normalize,
                            objPointsList);
                }

                // Set quantitative features
                HashMap<String, Object> features = new HashMap<>();
                features.put("Area", polygon.area);

                HashMap<String, HashMap<String, Object>> ns_features = new HashMap<>();
                String namespace = "http://u24.bmi.stonybrook.edu/v1";
                ns_features.put(namespace, features);

                obj_2d.setScalarFeatures(ns_features);

                // Set provenance data
                obj_2d.setProvenance(executionMetadata, imgMeta);

                if (outDB != null) {
                    // load to segmentation results database
                    outDB.submitObjectsDocument(obj_2d.getMetadataDoc().toString());
                } else if (bufferedWriter != null) {
                    // Write segmentation results to file in JSON format
                    bufferedWriter.write(obj_2d.getMetadataDoc().toString() + "\n");
                }
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
                outFileWriter.close();
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
