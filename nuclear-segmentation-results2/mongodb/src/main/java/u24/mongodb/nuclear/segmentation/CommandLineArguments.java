package u24.mongodb.nuclear.segmentation;

import org.apache.commons.cli.*;

/**
 * Parses command line arguments.
 * Uses Apache Commons CLI.
 */
public class CommandLineArguments {
    private static Options     allOpts;
    private static OptionGroup normOptGrp;
    private static CommandLine cmdLine;

    private static String dbServer 	= null;
    private static int    dbPort 	= 27017;
    private static String dbHost 	= "localhost";
    private static String dbName 	= null;
    private static String dbUser    = null;
    private static String dbPasswd  = null;

    private static String inpType = null;
    
    private static String inpList   = null;
    private static String imgFile   = null;
    private static String inpFile   = null;
    private static String outFolder = null;

    private static String  caseID    = null;
    private static String  subjectID = null;
    
    private static boolean doNormalize     = false;
    private static boolean doSelfNormalize = false;
    private static boolean getFromDB       = false;
    
    private static int width 	= 1;
    private static int height 	= 1;
    private static int shiftX 	= 0;
    private static int shiftY 	= 0;
    
    private static String algoID 	= null;
    private static String studyID 	= null;
	private static String batchID 	= "b0";
	private static String tagID 	= "t0";
    private static String algoType 	= "computer";
    private static String algoTitle = null;
    private static String algoColor = "yellow";
    private static String algoComp 	= "segmentation";
    private static String nameSpace = "http://u24.bmi.stonybrook.edu/v1";
    
    private static boolean doSimplify = false; 
    private static double  minSize    = 0.0;
    private static double  maxSize    = 40000000000.0; // large number 

	private static void defineDBOptions() {
		Option dbport = Option.builder()
				.longOpt("dbport")
				.desc("Database port.")
				.hasArg()
				.argName("dbport")
				.build();
		Option dbhost = Option.builder()
				.longOpt("dbhost")
				.desc("Database host.")
				.hasArg()
				.argName("dbhost")
				.build();
		Option dbname = Option.builder()
				.longOpt("dbname")
				.desc("Database name (required, if --dest is db).")
				.hasArg()
				.argName("dbname")
				.build();
		
		Option dbuser = Option.builder()
				.longOpt("dbuser")
				.desc("Database user.")
				.hasArg()
				.argName("dbuser")
				.build();
		
		Option dbpasswd = Option.builder()
				.longOpt("dbpasswd")
				.desc("Database password.")
				.hasArg()
				.argName("dbpasswd")
				.build();
		
		allOpts.addOption(dbhost);
		allOpts.addOption(dbport);
		allOpts.addOption(dbname);
		allOpts.addOption(dbuser);
		allOpts.addOption(dbpasswd);
	}

	/**
	 * Input options.
	 */
	private static void defineInputTypeOptions() {
		Option inpType = Option
				.builder()
				.longOpt("inptype")
				.desc("Input type: maskfile (binary mask file(s)), csv (QUIP CSV format), tsv (tab-separated value files.), aperio (Aperio XML markup files.)")
				.hasArg()
				.argName("maskfile|csv|tsv|aperio")
				.required(true)
				.build();
		allOpts.addOption(inpType);

		OptionGroup inpOptGrp = new OptionGroup();
		Option inpFile = Option.builder()
				.longOpt("inpfile")
				.desc("Input file.")
				.hasArg()
				.argName("filename")
				.build();
		Option inpList = Option
				.builder()
				.longOpt("inplist")
				.desc("File containing a list of masks with QUIP filename format or QUIP CSV/TSV files.")
				.hasArg()
				.argName("filename")
				.build();
		inpOptGrp.addOption(inpFile);
		inpOptGrp.addOption(inpList);
		allOpts.addOptionGroup(inpOptGrp);
	}

	/**
	 * Shift and normalization options for input segmentations.
	 */
	private static void defineShiftNormalizationOptions() {
		Option shift = Option.builder()
				.longOpt("shift")
				.desc("Shift in X and Y dimensions for a mask tile.")
				.numberOfArgs(2)
				.argName("x,y")
				.valueSeparator(',')
				.build();
		Option normalize = Option
				.builder()
				.longOpt("norm")
				.desc("Normalize polygons to [0,1] using width (w) and height (h).")
				.numberOfArgs(2)
				.argName("w,h")
				.valueSeparator(',')
				.build();
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
	 * Options to simplify and filter polygons in CSV files.
	 */
	private static void defineSimplifyFilterOptions() {
		Option simplify = Option.builder()
				.longOpt("simplify")
				.desc("Simplify polygons in CSV files using the JTS library.")
				.build();
		Option areafilter = Option.builder()
				.longOpt("sizefilter")
				.desc("Filter based on area.")
				.numberOfArgs(2)
				.argName("minSize,maxSize")
				.valueSeparator(',')
				.build();
		allOpts.addOption(simplify);
		allOpts.addOption(areafilter);
	}

	/**
	 * Image source options.
	 */
	private static void defineImageSourceOptions() {
		Option caseID = Option.builder()
				.longOpt("cid")
				.desc("Case ID (Image ID)")
				.hasArg()
				.argName("caseid")
				.build();
		Option subjectID = Option.builder().
				longOpt("sid")
				.desc("Subject ID")
				.hasArg()
				.argName("subjectid")
				.build();
		Option imgFile = Option.builder()
				.longOpt("img")
				.desc("Image file from which mask files were generated.")
				.hasArg()
				.argName("filename")
				.build();
		Option getFromDB = Option.builder()
				.longOpt("fromdb")
				.desc("Get image metadata from FeatureDB.")
				.build();
		allOpts.addOption(caseID);
		allOpts.addOption(subjectID);
		normOptGrp.addOption(imgFile);
		normOptGrp.addOption(getFromDB);
	}

	/**
	 * Analysis provenance.
	 */
	private static void defineAnalysisProvenanceOptions() {

		Option algoID = Option.builder()
				.longOpt("eid")
				.desc("Analysis execution id.")
				.hasArg()
				.argName("execid")
				.required(true)
				.build();

		Option studyID = Option.builder()
				.longOpt("studyid")
				.desc("Study id.")
				.hasArg()
				.argName("studyid")
				.required(true)
				.build();

		Option batchID = Option.builder()
				.longOpt("batchid")
				.desc("Batch id.")
				.hasArg()
				.argName("batchid")
				.build();

		Option tagID = Option.builder()
				.longOpt("tagid")
				.desc("Tag id.")
				.hasArg()
				.argName("tagid")
				.build();

		Option algoType = Option.builder()
				.longOpt("etype")
				.desc("Analysis type: human|computer.")
				.hasArg()
				.argName("type")
				.build();

		Option nmSpace = Option.builder()
				.longOpt("namespace")
				.desc("Namespace for feature attribute names.")
				.hasArg()
				.argName("namespace")
				.build();

		Option algoTitle = Option
				.builder()
				.longOpt("etitle")
				.desc("Analysis title for FeatureDB storage and visualization.")
				.hasArg()
				.argName("title")
				.build();

		Option color = Option.builder()
				.longOpt("ecolor")
				.desc("Color of segmentations for visualization.")
				.hasArg()
				.argName("color")
				.build();

		Option algoComp = Option.builder()
				.longOpt("ecomp")
				.desc("Analysis computation type: segmentation")
				.hasArg()
				.argName("computation")
				.build();

		allOpts.addOption(algoID);
		allOpts.addOption(studyID);
		allOpts.addOption(batchID);
		allOpts.addOption(tagID);
		allOpts.addOption(algoType);
		allOpts.addOption(algoTitle);
		allOpts.addOption(nmSpace);
		allOpts.addOption(color);
		allOpts.addOption(algoComp);
	}

	/**
	 * Destination.
	 */
	private static void defineDestinationOptions() {

		Option dest = Option
				.builder()
				.longOpt("dest")
				.desc("Output: JSON file (works with aperio and maskfile options only) or FeatureDB database.")
				.hasArg()
				.argName("file|db")
				.build();
		Option outFile = Option.builder()
				.longOpt("outfolder")
				.desc("Folder where output files will be written.")
				.hasArg()
				.argName("folder")
				.build();
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

		defineSimplifyFilterOptions();

		defineAnalysisProvenanceOptions();
		defineDestinationOptions();
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(100);
		System.out
				.println("Program to process Binary Mask, CSV, TSV, or Aperio XML files. Output is loaded to FeatureDB.\n");
		formatter.printHelp(" ", " ", allOpts, " ", true);
	}

	/**
	 * Get input values.
	 */
	private static boolean parseInputTypeOptions() {

		inpType = cmdLine.getOptionValue("inptype");
		switch (inpType) {
		case "aperio":
			if (cmdLine.hasOption("inpfile")) {
				inpFile = cmdLine.getOptionValue("inpfile");
			} else {
				System.err.println("ERROR: aperio option" + "requires --inpfile parameter.");
			}
			break;
		case "csv":
			if (cmdLine.hasOption("inplist")) {
				inpList = cmdLine.getOptionValue("inplist");
			} else if (cmdLine.hasOption("inpfile")) {
				inpFile = cmdLine.getOptionValue("inpfile");
			} else {
				System.err.println("ERROR: csv option requires --inplist or --inpfile parameter.");
				return false;
			}
			break;
		case "tsv":
			if (cmdLine.hasOption("inplist")) {
				inpList = cmdLine.getOptionValue("inplist");
			} else if (cmdLine.hasOption("inpfile")) {
				inpFile = cmdLine.getOptionValue("inpfile");
			} else {
				System.err.println("ERROR: tsv option requires --inplist or --inpfile parameter.");
				return false;
			}
			break;
		case "masktile":
			if (cmdLine.hasOption("inplist")) {
				inpList = cmdLine.getOptionValue("inplist");
			} else {
				System.err.println("ERROR: masktiles option " + "requires --inplist parameter.");
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
						.println("ERROR: maskfile option requires --inplist or --inpfile parameter.");
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

		if (cmdLine.hasOption("dbhost"))
			dbHost = cmdLine.getOptionValue("dbhost");
		if (cmdLine.hasOption("dbport"))
			dbPort = Integer.parseInt(cmdLine.getOptionValue("dbport"));
		if (cmdLine.hasOption("dbname"))
			dbName = cmdLine.getOptionValue("dbname");
		else {
			System.err.println("No database name is provided.");
			return false;
		}
		
		if (cmdLine.hasOption("dbuser"))
			dbUser = cmdLine.getOptionValue("dbuser");
		if (cmdLine.hasOption("dbpasswd"))
			dbPasswd = cmdLine.getOptionValue("dbpasswd");
		
		if (dbUser!=null)
			dbServer = "mongodb://" + dbUser + ":" + dbPasswd + "@" + dbHost + ":" + dbPort + "/?authSource=" + dbName;
		else
			dbServer = "mongodb://" + dbHost + ":" + dbPort + "/" + dbName;
		
		return true;
	}

	/**
	 * Check destination and get values.
	 */
	private static boolean parseDestinationOptions() {

		if (!cmdLine.hasOption("dest")) 
			return parseDatabaseOptions();

		String destVal = cmdLine.getOptionValue("dest");
		if (destVal.equals("db")) {
			return parseDatabaseOptions();
		} else if (destVal.equals("file")) {
			if (!cmdLine.hasOption("outfolder")) {
				System.err.println("Destination is file, but no foldername given.");
				return false;
			} else {
				outFolder = cmdLine.getOptionValue("outfolder");
				return true;
			}
		} else {
			System.err.println("Unknown destination option.");
			return false;
		}
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
			caseID = "undefined";
		}
		if (cmdLine.hasOption("sid")) {
			subjectID = cmdLine.getOptionValue("sid");
		} else {
			subjectID = "undefined";
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

	private static boolean parseSimplifyFilterOptions() {
		if (cmdLine.hasOption("sizefilter")) {
			minSize = Double
					.parseDouble(cmdLine.getOptionValues("sizefilter")[0]);
			maxSize = Double
					.parseDouble(cmdLine.getOptionValues("sizefilter")[1]);
		}
		if (cmdLine.hasOption("simplify")) {
			doSimplify = true;
		}
		return true;
	}

	/**
	 * Get analysis values.
	 */
	private static boolean parseAnalysisProvenanceOptions() {

		algoID 	 = cmdLine.getOptionValue("eid");
		studyID  = cmdLine.getOptionValue("studyid");

		if (cmdLine.hasOption("etype"))
			algoType = cmdLine.getOptionValue("etype");
		if (cmdLine.hasOption("batchid"))
			batchID  = cmdLine.getOptionValue("batchid");
		if (cmdLine.hasOption("tagid"))
			tagID  = cmdLine.getOptionValue("tagid");
		if (cmdLine.hasOption("ecolor"))
			algoColor  = cmdLine.getOptionValue("ecolor");
		if (cmdLine.hasOption("ecomp"))
			algoComp  = cmdLine.getOptionValue("ecomp");
		if (cmdLine.hasOption("namespace"))
			nameSpace  = cmdLine.getOptionValue("namespace");

		algoTitle = cmdLine.hasOption("etitle") ? cmdLine.getOptionValue("etitle") : "Algorithm: " + algoID;

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
		
		boolean parseRet = parseInputTypeOptions();
		parseRet &= parseDestinationOptions();
		parseRet &= parseImageSourceOptions();
		parseRet &= parseShiftNormalizationOptions();
		parseRet &= parseSimplifyFilterOptions();
		parseRet &= parseAnalysisProvenanceOptions();

		return parseRet; 
	}

	// Getters for DB
	public static String getDBServer() {
		return dbServer;
	}

	public static String getDBHost() {
		return dbHost;
	}

	public static int getDBPort() {
		return dbPort;
	}

	public static String getDBName() {
		return dbName;
	}
	
	public static String getDBUser() {
		return dbUser;
	}
	
	public static String getDBPasswd() {
		return dbPasswd;
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

	public static boolean isSimplify() {
		return doSimplify;
	}

	public static double getMinSize() {
		return minSize;
	}

	public static double getMaxSize() {
		return maxSize;
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

	public static String getStudyID() {
		return studyID;
	}

	public static String getBatchID() {
		return batchID;
	}

	public static String getTagID() {
		return tagID;
	}

	public static String getNamespace() {
		return nameSpace;
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
