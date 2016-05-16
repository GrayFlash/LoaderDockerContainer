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
import java.util.StringTokenizer;

/**
 * Tab Separated Values file handler.
 */
public class ProcessTSVSBUFile implements ProcessFile {
    private String fileName;
    private String fileFolder;
    private String colorVal;
    private boolean geoJSON;
    private AnalysisExecutionMetadata executionMetadata;
    private ResultsDatabase segDB;
    private ImageExecutionMapping imgExecMap;
    private double min_x, min_y, max_x, max_y;
    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final boolean normalize = true;


    public ProcessTSVSBUFile(String fileFolder, String fileName,
                             AnalysisExecutionMetadata executionMetadata, String colorVal,
                             ResultsDatabase segDB, boolean isGeoJSON) {
        System.out.println("### ProcessTSVSBUFile ###");
        this.fileFolder = fileFolder;
        this.fileName = fileName;
        this.geoJSON = isGeoJSON;
        this.executionMetadata = executionMetadata;
        this.colorVal = colorVal;
        this.segDB = segDB;
        this.imgExecMap = new ImageExecutionMapping();
    }


    public ProcessTSVSBUFile(boolean isGeoJSON) {
        System.out.println("### ProcessTSVSBUFile ###");
        geoJSON = isGeoJSON;
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
    ArrayList<Double> getNormalizedPoints(String points, double img_width,
                                          double img_height) {

        StringTokenizer pp = new StringTokenizer(points, ";");
        ArrayList<Double> out_points = new ArrayList<>();
        while (pp.hasMoreTokens()) {
            String[] xy = pp.nextToken().split(",");

            double x = Double.parseDouble(xy[0]);
            double y = Double.parseDouble(xy[1]);

            x = x / img_width;
            y = y / img_height;

            out_points.add(x);
            out_points.add(y);
        }

        return out_points;
    }

    /**
     * Parse file name.
     */
    public List<String> parseFileName() {
        List<String> fnameTokens = new ArrayList<>();
        StringTokenizer fst = new StringTokenizer(fileName, ".");
        while (fst.hasMoreTokens()) {
            fnameTokens.add(fst.nextToken());
        }

        return fnameTokens;
    }

    /**
     * Performs the processing operation.
     */
    public void processFile() {
        try {
            List<String> fnameTokens = parseFileName();
            String caseId = fnameTokens.get(0);

            System.out.println("Filename tokens:");
            System.out.println(fnameTokens.toString());

            // Query and retrieve image metadata values
            BasicDBObject imgQuery = new BasicDBObject();
            imgQuery.put("case_id", caseId);
            DBObject qryResult = segDB.getImagesCollection().findOne(imgQuery);

            if (qryResult == null) {
                System.err.println("ERROR: Cannot find case_id: "
                        + caseId);
                return;
            }

            double objective = Double.parseDouble(qryResult.get("objective")
                    .toString());

            double mpp_x = Double
                    .parseDouble(qryResult.get("mpp_x").toString());

            double mpp_y = Double
                    .parseDouble(qryResult.get("mpp_y").toString());

            double image_width = Double.parseDouble(qryResult.get("width")
                    .toString());

            double image_height = Double.parseDouble(qryResult.get("height")
                    .toString());

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
            imgMeta.setIdentifier(caseId + "." + fnameTokens.get(1));
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

            // Parse header information
            List<String> headers = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(lines.get(0), "\t");
            while (st.hasMoreTokens()) {
                headers.add(st.nextToken());
            }

            // Read lines following header (i = 1).
            for (int i = 1; i < lines.size(); i++) {
                // Parse the segmentation results
                List<String> values = new ArrayList<>();

                StringTokenizer vst = new StringTokenizer(lines.get(i), " \t");
                while (vst.hasMoreTokens()) {
                    values.add(vst.nextToken());
                }

                ArrayList<Double> normPoints = getNormalizedPoints(
                        values.get(headers.size() - 1), image_width,
                        image_height);

                Poinsettia flowerPot = new Poinsettia();
                flowerPot.getBoundingBox(normPoints);

                min_x = flowerPot.getMin_x();
                min_y = flowerPot.getMin_y();
                max_x = flowerPot.getMax_x();
                max_y = flowerPot.getMax_y();

                BasicDBList objPointsList = flowerPot.getPolygonPoints(normPoints);

                Image2DMarkupGeoJSON obj_2d = new Image2DMarkupGeoJSON();

                // Check markup data
                if (objPointsList.size() > 0) {
                    // Set markup data
                    obj_2d.setMarkup(min_x,
                            min_y, max_x, max_y, "Polygon", normalize,
                            objPointsList);
                }

                // Set scalar features
                HashMap<String, Object> features = setFeatures(headers, values, obj_2d);

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

    public HashMap<String, Object> setFeatures(List<String> headers, List<String> values, Image2DMarkupGeoJSON obj_2d) {
        HashMap<String, Object> features = new HashMap<>();
        for (int j = 3; j < headers.size() - 1; j++) {
            if (isNumeric(values.get(j))) {
                String name = headers.get(j);
                float value = Float.parseFloat(values.get(j));
                features.put(name, value);

                if (name.equalsIgnoreCase("area")) {
                    obj_2d.setArea(value);
                }

            }

        }

        return features;

    }

    public boolean isGeoJSON() {
        return geoJSON;
    }

}
