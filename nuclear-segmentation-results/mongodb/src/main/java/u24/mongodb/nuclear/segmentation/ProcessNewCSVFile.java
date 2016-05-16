package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Comma Separated Values file handler.
 */
public class ProcessNewCSVFile implements ProcessFile {
    private String fileName;
    private String fileFolder;
    private String colorVal;
    private String caseId;
    private AnalysisExecutionMetadata executionMetadata;
    private ResultsDatabase segDB;
    private ImageExecutionMapping imgExecMap;
    private double min_x, min_y, max_x, max_y;
    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final boolean normalize = true;

    public ProcessNewCSVFile() {

    }

    public ProcessNewCSVFile(String fileFolder, String fileName, String caseId,
                             AnalysisExecutionMetadata executionMetadata, String colorVal,
                             ResultsDatabase segDB) {
        this.fileFolder = fileFolder;
        this.fileName = fileName;
        this.caseId = caseId;
        this.executionMetadata = executionMetadata;
        this.colorVal = colorVal;
        this.segDB = segDB;
        this.imgExecMap = new ImageExecutionMapping();
    }

    public boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }


    List<String> readSmallTextFile(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        return Files.readAllLines(path, ENCODING);
    }

    /**
     * Does normalization.
     */
    ArrayList<Double> getNormalizedPoints(String[] points, int idx, double img_width,
                                          double img_height) {

        ArrayList<Double> out_points = new ArrayList<>();

        for (int i = idx; i < points.length; i += 2) {
            double x = Double.parseDouble(points[i]);
            double y = Double.parseDouble(points[i + 1]);

            x = x / img_width;
            y = y / img_height;

            out_points.add(x);
            out_points.add(y);
        }

        return out_points;
    }

    /**
     * Performs the processing operation.
     */
    public void processFile() {
        try {
            // Query and retrieve image metadata values
            BasicDBObject imgQuery = new BasicDBObject();
            imgQuery.put("case_id", caseId);
            DBObject qryResult = segDB.getImagesCollection().findOne(imgQuery);
            if (qryResult == null) {
                System.err.println("ERROR: Cannot find caseid: " + caseId);
                return;
            }

            double objective = Double.parseDouble(qryResult.get("objective").toString());
            double mpp_x = Double.parseDouble(qryResult.get("mpp_x").toString());
            double mpp_y = Double.parseDouble(qryResult.get("mpp_y").toString());
            double image_width = Double.parseDouble(qryResult.get("width").toString());
            double image_height = Double.parseDouble(qryResult.get("height").toString());
            String cancer_type = qryResult.get("cancer_type").toString();

            String[] tmpVal = caseId.split("-");
            String subject_id = tmpVal[0] + "-" + tmpVal[1] + "-" + tmpVal[2];

            // Check if dimensions are negative or zero
            if (image_width <= 0.0 || image_height <= 0.0) {
                System.err.println("ERROR: Cannot find caseId: "
                        + caseId);
                return;
            }

            SimpleImageMetadata imgMeta = new SimpleImageMetadata();
            imgMeta.setIdentifier(caseId);
            imgMeta.setCaseid(caseId);
            imgMeta.setSubjectid(subject_id);
            imgMeta.setMpp_x(mpp_x);
            imgMeta.setMpp_y(mpp_y);
            imgMeta.setWidth(image_width);
            imgMeta.setHeight(image_height);
            imgMeta.setObjective(objective);
            imgMeta.setCancertype(cancer_type);

            List<String> lines = readSmallTextFile(fileFolder + "/" + fileName);

            // Check and register image to analysis mapping information
            imgExecMap.setMetadataDoc(executionMetadata, imgMeta, colorVal);

            if (!imgExecMap.checkExists(segDB, executionMetadata.getIdentifier(), imgMeta.getCaseid())) {
                segDB.submitMetadataDocument(imgExecMap.getMetadataDoc());
            }

            System.out.println("Lines: " + lines.size());

            // Read lines [no header (i = 0)].
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                // Parse the segmentation results
                String[] values = line.split(",");

                // 22 = index of area
                ArrayList<Double> normPoints = new ArrayList<>();
                normPoints = getNormalizedPoints(
                        values, 22, image_width, image_height);

                Poinsettia pointSetter = new Poinsettia();
                pointSetter.getBoundingBox(normPoints);

                min_x = pointSetter.getMin_x();
                min_y = pointSetter.getMin_y();
                max_x = pointSetter.getMax_x();
                max_y = pointSetter.getMax_y();

                BasicDBList objPointsList = pointSetter.getPolygonPoints(normPoints);

                Image2DMarkupGeoJSON obj_2d = new Image2DMarkupGeoJSON();

                // Check markup data
                if (objPointsList.size() > 0) {
                    // Set markup data
                    obj_2d.setMarkup(min_x,
                            min_y, max_x, max_y, "Polygon", normalize, objPointsList);
                }

                // Set scalar features
                HashMap<String, Object> features = setFeatures(values, mpp_x, mpp_y, obj_2d);

                HashMap<String, HashMap<String, Object>> ns_features = new HashMap<>();
                String namespace = "http://u24.bmi.stonybrook.edu/v1";
                ns_features.put(namespace, features);

                obj_2d.setScalarFeatures(ns_features);

                // Set provenance data
                obj_2d.setProvenance(executionMetadata, imgMeta);

                // load to segmentation results database
                segDB.submitObjectsDocument(obj_2d.getMetadataDoc());
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }


    public HashMap<String, Object> setFeatures(String[] values, double mpp_x, double mpp_y, Image2DMarkupGeoJSON obj_2d) {
        HashMap<String, Object> features = new HashMap<>();
                /*
                1. bonding box top left x
				2. bonding box top left y
				3. bonding box bottom right x
				4. bonding box bottom right y
				5. NumberOfPixels
				6. PhysicalSize
				7. NumberOfPixelsOnBorder
				8. FeretDiameter
				9. PrincipalMoments[0]
				10. PrincipalMoments[1]
				11. Elongation
				12. Perimeter
				13. Roundness
				14. EquivalentSphericalRadius
				15. EquivalentSphericalPerimeter
				16. EquivalentEllipsoidDiameter[0]
				17. EquivalentEllipsoidDiameter[1]
				18. Flatness

				Then, followed by:
				19. PolygonNo
				20. centroid X
				21. centroid Y
				22. Area
				23-. Boundaries (length may vary)
				*/


        // NumberOfPixels
        if (isNumeric(values[4]))
            features.put("NumberOfPixels", Float.parseFloat(values[4]));

        // Physical Size
        if (isNumeric(values[5]))
            features.put("PhysicalSize", mpp_x * mpp_y * Float.parseFloat(values[5]));

        // NumberOfPixelsOnBorder
        if (isNumeric(values[6]))
            features.put("NumberOfPixelsOnBorder", Float.parseFloat(values[6]));

        // FeretDiameter
        if (isNumeric(values[7]))
            features.put("FeretDiameter", Float.parseFloat(values[7]));

        // PrincipalMoments0
        if (isNumeric(values[8]))
            features.put("PrincipalMoments0", Float.parseFloat(values[8]));

        // PrincipalMoment1
        if (isNumeric(values[9]))
            features.put("PrincipalMoments1", Float.parseFloat(values[9]));

        // Elongation
        if (isNumeric(values[10]))
            features.put("Elongation", Float.parseFloat(values[10]));

        // Perimeter
        if (isNumeric(values[11]))
            features.put("Perimeter", Float.parseFloat(values[11]));

        // Roundness
        if (isNumeric(values[12]))
            features.put("Roundness", Float.parseFloat(values[12]));

        // EquivalentSphericalRadius
        if (isNumeric(values[13]))
            features.put("EquivalentSphericalRadius", Float.parseFloat(values[13]));

        // EquivalentSphericalPerimeter
        if (isNumeric(values[14]))
            features.put("EquivalentSphericalPerimeter", Float.parseFloat(values[14]));

        // EquivalentEllipsoidDiameter0
        if (isNumeric(values[15]))
            features.put("EquivalentEllipsoidDiameter0", Float.parseFloat(values[15]));

        // EquivalentEllipsoidDiameter1
        if (isNumeric(values[16]))
            features.put("EquivalentEllipsoidDiameter1", Float.parseFloat(values[16]));

        // Flatness
        if (isNumeric(values[17]))
            features.put("Flatness", Float.parseFloat(values[17]));

        // Area
        if (isNumeric(values[21])) {
            float area = Float.parseFloat(values[21]);
            features.put("Area", area);
            obj_2d.setArea(area);
        }

        return features;
    }

}
