package u24.mongodb.nuclear.segmentation;

import org.apache.commons.cli.*;

/**
 * Parses command line arguments.
 * Uses Apache Commons CLI.
 */
public class CommandLineArguments {
    private static Options allOpts;
    private static OptionGroup normOptGrp;
    private static CommandLine cmdLine;

    private static String dbServer = null;
    private static String dbConnectionType = null;

    private static String inpType = null;
    private static String inpList = null;

    private static String outFolder = null;

    private static String caseID = null;
    private static String subjectID = null;
    private static boolean doNormalize = false;
    private static boolean doSelfNormalize = false;

    private static String algoID = null;
    private static String algoType = null;
    private static String algoTitle = null;
    private static String algoColor = null;
    private static String algoComp = null;

    // Assigned but not used:
    private static boolean getFromDB = false;
    private static int shiftX = 0;
    private static int shiftY = 0;
    private static int width = -1;
    private static int height = -1;
    private static String imgFile = null;
    private static String inpFile = null;


    private static void defineDBOptions() {
        String desc1 = "  MongoDB interface: mongodb://host:port/database_name.";
        String desc2 = "  REST interface: http://host:port/database_name.";

        OptionGroup dbOptGrp = new OptionGroup();
        // Database options
        Option dbServer = Option.builder().longOpt("mongo")
                .desc(desc1).hasArg()
                .argName("server").build();
        Option restServer = Option.builder().longOpt("rest")
                .desc(desc2).hasArg()
                .argName("server").build();
        dbOptGrp.addOption(dbServer);
        dbOptGrp.addOption(restServer);
        allOpts.addOptionGroup(dbOptGrp);
    }

    /**
     * Input options.
     */
    private static void defineInputTypeOptions() {

        Option inpType = Option
                .builder()
                .longOpt("inptype")
                .desc("Input type: maskfile (mask file(s)), masktile (mask tiles in U24 file name format), tsv (Legacy matlab tab-separated value text segmentation files.), aperio (Aperio XML markup files.)")
                .hasArg().argName("maskfile|masktile|tsv|aperio").required(true)
                .build();
        allOpts.addOption(inpType);

        OptionGroup inpOptGrp = new OptionGroup();
        Option inpFile = Option.builder().longOpt("inpfile")
                .desc("Input file.").hasArg().argName("filename").build();
        Option inpList = Option
                .builder()
                .longOpt("inplist")
                .desc("File or folder containing a list of masks with U24 filename format or CSV/TSV files.")
                .hasArg().argName("filename or folder").build();
        inpOptGrp.addOption(inpFile);
        inpOptGrp.addOption(inpList);
        allOpts.addOptionGroup(inpOptGrp);
    }

    /**
     * Shift and normalization options for input segmentations.
     */
    private static void defineShiftNormalizationOptions() {

        Option shift = Option.builder().longOpt("shift")
                .desc("Shift in X and Y dimensions for a mask tile.")
                .numberOfArgs(2).argName("x,y").valueSeparator(',').build();
        Option normalize = Option
                .builder()
                .longOpt("norm")
                .desc("Normalize polygons to [0,1] using width (w) and height (h).")
                .numberOfArgs(2).argName("w,h").valueSeparator(',').build();
        Option normbyself = Option
                .builder()
                .longOpt("self")
                .desc("Normalize polygons to [0,1] using width and height of input mask files.")
                .build();
        allOpts.addOption(shift);
        normOptGrp.addOption(normalize);
        normOptGrp.addOption(normbyself);
    }

    /**
     * Image source options.
     */
    private static void defineImageSourceOptions() {

        Option caseID = Option.builder().longOpt("cid").desc("Case ID")
                .hasArg().argName("id").build();
        Option subjectID = Option.builder().longOpt("sid").desc("Subject ID")
                .hasArg().argName("id").build();
        Option imgFile = Option.builder().longOpt("img")
                .desc("Image file from which mask files were generated.")
                .hasArg().argName("filename").build();
        Option getFromDB = Option.builder().longOpt("fromdb")
                .desc("Get image metadata from FeatureDB.").build();
        allOpts.addOption(caseID);
        allOpts.addOption(subjectID);
        normOptGrp.addOption(imgFile);
        normOptGrp.addOption(getFromDB);
    }

    /**
     * Analysis provenance.
     */
    private static void defineAnalysisProvenanceOptions() {

        Option algoID = Option.builder().longOpt("eid")
                .desc("Analysis execution id.").hasArg().argName("id")
                .required(true).build();

        Option algoType = Option.builder().longOpt("etype")
                .desc("Analysis type: human|computer.").hasArg()
                .argName("type").required(true).build();

        Option algoTitle = Option
                .builder()
                .longOpt("etitle")
                .desc("Analysis title for FeatureDB storage and visualization.")
                .hasArg().argName("title").build();

        Option color = Option.builder().longOpt("ecolor")
                .desc("Color of segmentations for visualization.").hasArg()
                .argName("color").build();

        Option algoComp = Option.builder().longOpt("ecomp")
                .desc("Analysis computation type: segmentation").hasArg()
                .argName("computation").build();

        allOpts.addOption(algoID);
        allOpts.addOption(algoType);
        allOpts.addOption(algoTitle);
        allOpts.addOption(color);
        allOpts.addOption(algoComp);
    }

    /**
     * Destination.
     */
    private static void defineDestinationOptions() {

        Option dest = Option.builder().longOpt("dest")
                .desc("Output: JSON file or FeatureDB database.").hasArg()
                .argName("file|db").required(true).build();
        Option outFile = Option.builder().longOpt("outfolder")
                .desc("Folder where output files will be written.").hasArg()
                .argName("folder").build();
        allOpts.addOption(dest);
        allOpts.addOption(outFile);
    }

    public static void initCommandLineOptions() {
        allOpts = new Options();
        normOptGrp = new OptionGroup();

        defineDBOptions();
        defineInputTypeOptions();

        defineImageSourceOptions();
        defineShiftNormalizationOptions();
        allOpts.addOptionGroup(normOptGrp);

        defineAnalysisProvenanceOptions();
        defineDestinationOptions();
    }

    public static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);

        System.out.println("Program to process mask, TSV, or Aperio XML files. Output is either written to a JSON file or loaded to FeatureDB.\n");

        formatter.printHelp("DataLoader", " ", allOpts, " ", true);
    }


    /**
     * Get input values.
     */
    private static boolean parseInputTypeOptions() {

        String requiresInputList = "requires --inplist parameter.";
        String requiresInputFile = "requires --inpfile parameter.";

        inpType = cmdLine.getOptionValue("inptype");
        switch (inpType) {
        	case "aperio":
        		if (cmdLine.hasOption("inpfile")) {
        			inpFile = cmdLine.getOptionValue("inpfile");
        		} else {
        			System.err.println("ERROR: aperio option" + requiresInputFile);
        		}
        		break;
            case "csv":
                if (cmdLine.hasOption("inplist")) {
                    inpList = cmdLine.getOptionValue("inplist");
                } else {
                    System.err
                            .println("ERROR: csv option " + requiresInputList);
                    return false;
                }
                break;
            case "tsv":
                if (cmdLine.hasOption("inplist")) {
                    inpList = cmdLine.getOptionValue("inplist");
                } else {
                    System.err
                            .println("ERROR: tsv option " + requiresInputList);
                    return false;
                }
                break;
            case "masktile":
                if (cmdLine.hasOption("inplist")) {
                    inpList = cmdLine.getOptionValue("inplist");
                } else {
                    System.err
                            .println("masktiles option " + requiresInputList);
                    return false;
                }
                break;
            case "maskfile":
                if (cmdLine.hasOption("inplist")) {
                    inpList = cmdLine.getOptionValue("inplist");
                } else if (cmdLine.hasOption("inpfile")) {
                    inpFile = cmdLine.getOptionValue("inpfile");
                } else {
                    System.err
                            .println("maskfile option requires --inplist or --inpfile parameter.");
                    return false;
                }
                break;
            default:
                System.err.println("ERROR: Unknown value for inptype parameter.");
                return false;
        }
        return true;
    }

    private static boolean parseDatabaseOptions() {

        if (cmdLine.hasOption("mongo")) {
            dbConnectionType = "mongodb";
            dbServer = cmdLine.getOptionValue("mongo");
        } else {
            if (cmdLine.hasOption("rest")) {
                dbConnectionType = "http";
                dbServer = cmdLine.getOptionValue("rest");
            } else {
                System.err.println("No mongo or rest option is provided.");
                return false;
            }
        }

        return true;

    }

    /**
     * Check destination and get values.
     */
    private static boolean parseDestinationOptions() {

        String destVal = cmdLine.getOptionValue("dest");
        switch (destVal) {
            case "db":
                if (cmdLine.hasOption("mongo") || cmdLine.hasOption("rest")) {
                    parseDatabaseOptions();
                } else {
                    System.err.println("Destination is FeatureDB, but no DB host:port defined.");
                    return false;
                }
                break;
            case "file":
                if (!cmdLine.hasOption("outfolder")) {
                    System.err
                            .println("Destination is file, but no foldername given.");
                    return false;
                } else {
                    outFolder = cmdLine.getOptionValue("outfolder");
                }
                break;
            default:
                System.err.println("ERROR: Unknown value for 'dest' argument.");
                return false;
        }
        return true;
    }

    /**
     * Get source image values.
     */
    private static boolean parseImageSourceOptions() {

        if (cmdLine.hasOption("img")) {
            imgFile = cmdLine.getOptionValue("img");
            doNormalize = true;
        }
        if (cmdLine.hasOption("cid")) {
            caseID = cmdLine.getOptionValue("cid");
        } else {
            caseID = "Undefined";
        }
        if (cmdLine.hasOption("sid")) {
            subjectID = cmdLine.getOptionValue("sid");
        } else {
            subjectID = "Undefined";
        }

        // Get image metadata from DB?
        if (cmdLine.hasOption("fromdb")) {
            getFromDB = true;
            doNormalize = true;
        }
        return true;
    }

    /**
     * Get shift and normalization values.
     */
    private static boolean parseShiftNormalizationOptions() {

        if (cmdLine.hasOption("shift")) {
            shiftX = Integer.parseInt(cmdLine.getOptionValues("shift")[0]);
            shiftY = Integer.parseInt(cmdLine.getOptionValues("shift")[1]);
        }
        if (cmdLine.hasOption("norm")) {
            doNormalize = true;
            width = Integer.parseInt(cmdLine.getOptionValues("norm")[0]);
            height = Integer.parseInt(cmdLine.getOptionValues("norm")[1]);
        }
        if (cmdLine.hasOption("self")) {
            doNormalize = true;
            doSelfNormalize = true;
        }
        return true;
    }

    /**
     * Get analysis values.
     */
    private static boolean parseAnalysisProvenanceOptions() {

        algoID = cmdLine.getOptionValue("eid");
        algoType = cmdLine.getOptionValue("etype");

        System.out.println("Alg: " + algoID + " type " + algoType);

        algoTitle = cmdLine.hasOption("etitle") ? cmdLine.getOptionValue("etitle") : "Algorithm: " + algoID;

        algoColor = cmdLine.hasOption("ecolor") ? cmdLine.getOptionValue("ecolor") : "yellow";

        algoComp = cmdLine.hasOption("ecomp") ? cmdLine.getOptionValue("ecomp") : "segmentation";

        return true;
    }

    public static boolean parseCommandLineArgs(String args[]) {
        CommandLineParser parser = new DefaultParser();
        try {
            cmdLine = parser.parse(allOpts, args);
        } catch (ParseException exp) {
            System.err.println("ERROR: " + exp.getMessage());
            return false;
        }

        return parseInputTypeOptions() && parseDestinationOptions() && parseImageSourceOptions() && parseShiftNormalizationOptions() && parseAnalysisProvenanceOptions();

    }

    // Getters for DB
    public static String getDBServer() {
        return dbServer;
    }

    public static String getDBConnectionType() {
        return dbConnectionType;
    }

    // Getters for input type
    public static boolean isMaskFile() {
        return inpType.equals("maskfile");
    }

    public static boolean isMaskTile() {
        return inpType.equals("masktile");
    }

    public static boolean isTSV() {
        return inpType.equals("tsv");
    }

    public static boolean isCSV() {
        return inpType.equals("csv");
    }
    
    public static boolean isAperio() {
    	return inpType.equals("aperio");
    }

    public static String getInpList() {
        return inpList;
    }
    
    public static String getInpFile() {
    	return inpFile;
    }

    public static String getOutFoldername() {
        return outFolder;
    }
    
    public static String getCaseID() {
        return caseID;
    }

    public static String getSubjectID() {
        return subjectID;
    }

    public static boolean isNormalize() {
        return doNormalize;
    }

    public static boolean isSelfNormalize() {
        return doSelfNormalize;
    }
    
    public static int getShiftX() {
    	return shiftX;
    }
    public static int getShiftY() {
    	return shiftY;
    }
    public static int getWidth() {
    	return width;
    }
    public static int getHeight() {
    	return height;
    }
    
    public static boolean isGetFromDB() {
    	return getFromDB;
    }
    
    public static String getInpImage() {
    	return imgFile;
    }
    
    public static String getAnalysisID() {
        return algoID;
    }

    public static String getAnalysisType() {
        return algoType;
    }

    public static String getAnalysisTitle() {
        return algoTitle;
    }

    public static String getColor() {
        return algoColor;
    }

    public static String getAnalysisComputation() {
        return algoComp;
    }
}
