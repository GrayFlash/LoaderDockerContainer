package u24.mongodb.nuclear.segmentation;

import java.io.File;
import java.io.FileWriter;

class ProcessFileThread implements Runnable {
    private Thread myThread;
    private ProcessFile inpFile;

    public ProcessFileThread(ProcessFile inpFile) {
        this.myThread = new Thread(this, "myThread");
        this.inpFile = inpFile;
        myThread.start();
    }

    public void run() {
        inpFile.processFile();
    }

    public boolean isAlive() {
        return this.myThread.isAlive();
    }
}


public class MongoSimpleLoaderThreaded {

    public static void handleFile(ProcessFile process, int numThreads) {
        String segdbServer = CommandLineArguments.getDBServer();
        String fileList = CommandLineArguments.getInpList();
        String execName = CommandLineArguments.getAnalysisTitle();
        String execId = CommandLineArguments.getAnalysisID();
        String execType = CommandLineArguments.getAnalysisType();
        String execComp = CommandLineArguments.getAnalysisComputation();
        String colorVal = CommandLineArguments.getColor();

        System.out.println("Database: " + segdbServer + " Filelist: " + fileList);

        try {
            ResultsDatabase[] segDB = new ResultsDatabase[numThreads];
            for (int i = 0; i < numThreads; i++)
                segDB[i] = new ResultsDatabase(segdbServer);

            ProcessFileThread[] procFile = new ProcessFileThread[numThreads];

            AnalysisExecutionMetadata executionMetadata = new AnalysisExecutionMetadata(
                    execId, execName, execType, execComp);

            IterateInputData iter = new IterateInputData(fileList);

            String currLine;
            int thread_id = 0;
            int fi = 0;
            while (iter.hasNext()) {
                currLine = iter.next();

                File inputFile = new File(currLine);
                String fileName = inputFile.getName();
                String folderName = inputFile.getParent();

                System.out.println("Processing[" + fi + "]: " + folderName
                        + "   " + fileName);

                String[] caseId = new String[0];
                if (process instanceof ProcessNewCSVFile) {
                    String[] tmpId = folderName.split("/");
                    for (int i = 0; i < tmpId.length; i++) {
                        System.out.println("IDX: " + i + ": " + tmpId[i]);

                    }
                    System.out.println("Count: " + tmpId.length);
                    String tmpVal = tmpId[tmpId.length - 1];
                    System.out.println("Value: " + tmpVal);
                    caseId = tmpVal.split("\\.");
                    System.out.println("COUNT: " + caseId.length);
                    System.out.println("CASEID: " + caseId[0]);
                }

                int check_done = 0;
                while (check_done == 0) {
                    if (procFile[thread_id] == null
                            || !procFile[thread_id].isAlive()) {

                        if (process instanceof ProcessTSVSBUFile) {
                            boolean isGeoJSON = ((ProcessTSVSBUFile) process).isGeoJSON();
                            process = new ProcessTSVSBUFile(folderName, fileName, executionMetadata, colorVal,
                                    segDB[thread_id], isGeoJSON);
                        }

                        if (process instanceof ProcessNewCSVFile) {
                            process = new ProcessNewCSVFile(folderName, fileName, caseId[0], executionMetadata, colorVal,
                                    segDB[thread_id]);
                        }

                        procFile[thread_id] = new ProcessFileThread(process);

                        check_done = 1;
                    }
                    thread_id = (thread_id + 1) % numThreads;
                    Thread.sleep(500);
                }
                fi++;
            }

            // Finishing Threads
            loop(procFile,numThreads);


        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }


    public static void handleMaskTile() {
        String segdbServer = CommandLineArguments.getDBServer();
        String dbConnectionType = CommandLineArguments.getDBConnectionType();
        String fileList = CommandLineArguments.getInpList();
        String execName = CommandLineArguments.getAnalysisTitle();
        String execId = CommandLineArguments.getAnalysisID();
        String execType = CommandLineArguments.getAnalysisType();
        String computation = "segmentation";
        String colorVal = CommandLineArguments.getColor();

        System.out.println("Database: " + segdbServer + " Filelist: " + fileList);

        int numThreads = 6;
        try {
            if (dbConnectionType.equals("mongodb")) {
                ResultsDatabase[] segDB = new ResultsDatabase[numThreads];
                for (int i = 0; i < numThreads; i++)
                    segDB[i] = new ResultsDatabase(segdbServer);

                ProcessFileThread[] procFile = new ProcessFileThread[numThreads];

                AnalysisExecutionMetadata executionMetadata = new AnalysisExecutionMetadata(
                        execId, execName, execType, computation);

                IterateInputData iter = new IterateInputData(fileList);

                String currLine;
                int thread_id = 0;
                int fi = 0;
                while (iter.hasNext()) {
                    currLine = iter.next();
                    System.out.println("Processing [" + fi + "]: " + currLine);

                    String temp = (new File(currLine)).getName();
                    String[] tokens = temp.split("_|-seg.png|.png");
                    String caseID = (tokens[0].split("\\."))[0];
                    int shiftX = Integer.parseInt(tokens[3]) * 4096;
                    int shiftY = Integer.parseInt(tokens[4]) * 4096;

                    System.out.println("fileName: " + temp +
                            " caseID: " + caseID + "shift: " + shiftX + " " + shiftY);

                    int check_done = 0;
                    while (check_done == 0) {
                        if (procFile[thread_id] == null
                                || !procFile[thread_id].isAlive()) {
                            ProcessMaskFile maskFile = new ProcessMaskFile(
                                    currLine, executionMetadata, shiftX, shiftY,
                                    segDB[thread_id]);
                            maskFile.setColor(colorVal);
                            maskFile.setCaseID(caseID);
                            maskFile.doNormalization(true, false);
                            maskFile.setImgMetaFromDB(segDB[thread_id]);

                            procFile[thread_id] = new ProcessFileThread(maskFile);
                            check_done = 1;
                        }
                        thread_id = (thread_id + 1) % numThreads;
                        Thread.sleep(500);
                    }
                    fi++;
                }

                // Finishing Threads
                loop(procFile,numThreads);

            } else {
                ResultsDatabaseHTTP[] segDB = new ResultsDatabaseHTTP[numThreads];
                for (int i = 0; i < numThreads; i++)
                    segDB[i] = new ResultsDatabaseHTTP(segdbServer, "tahsin", "tahsin");

                ProcessFileThread[] procFile = new ProcessFileThread[numThreads];

                AnalysisExecutionMetadata executionMetadata = new AnalysisExecutionMetadata(
                        execId, execName, execType, computation);

                IterateInputData iter = new IterateInputData(fileList);

                String currLine;
                int thread_id = 0;
                int fi = 0;
                while (iter.hasNext()) {
                    currLine = iter.next();
                    System.out.println("Processing [" + fi + "]: " + currLine);

                    String temp = (new File(currLine)).getName();
                    String[] tokens = temp.split("_|-seg.png|.png");
                    String caseID = (tokens[0].split("\\."))[0];
                    int shiftX = Integer.parseInt(tokens[3]) * 4096;
                    int shiftY = Integer.parseInt(tokens[4]) * 4096;

                    System.out.println("fileName: " + temp +
                            " caseID: " + caseID + "shift: " + shiftX + " " + shiftY);

                    int check_done = 0;
                    while (check_done == 0) {
                        if (procFile[thread_id] == null
                                || !procFile[thread_id].isAlive()) {
                            ProcessMaskFileHTTP maskFile = new ProcessMaskFileHTTP(
                                    currLine, executionMetadata, shiftX, shiftY,
                                    segDB[thread_id]);
                            maskFile.setColor(colorVal);
                            maskFile.setCaseID(caseID);
                            maskFile.doNormalization(true, false);
                            maskFile.setImgMetaFromDB(segDB[thread_id]);

                            procFile[thread_id] = new ProcessFileThread(maskFile);
                            check_done = 1;
                        }
                        thread_id = (thread_id + 1) % numThreads;
                        Thread.sleep(500);
                    }
                    fi++;
                }

                // Finishing Threads
                loop(procFile,numThreads);
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
	public static void handleAperioXMLFile() {
		String outFolder = CommandLineArguments.getOutFoldername();
		if (outFolder == null) outFolder = "./";

		String inpFile = CommandLineArguments.getInpFile();
		if (inpFile == null) {
			System.err.println("Need an input file.");
			return;
		}

		String caseID = CommandLineArguments.getCaseID();
		if (caseID == null) caseID = "NO-CASE-ID";

		boolean normalize = CommandLineArguments.isNormalize();
		int img_width = CommandLineArguments.getWidth();
		int img_height = CommandLineArguments.getHeight();
		if (normalize && (img_width == -1 || img_height == -1)) {
			System.err
					.println("Please enter valid image width and height (>0) values.");
			return;
		}
		int shiftX = CommandLineArguments.getShiftX();
		int shiftY = CommandLineArguments.getShiftY();

		String execId      = CommandLineArguments.getAnalysisID();
		String execType    = CommandLineArguments.getAnalysisType();
		String computation = "markup";
		String execName    = CommandLineArguments.getAnalysisTitle();
		String colorVal    = CommandLineArguments.getColor();

		int numThreads = 1;
		try {
			ProcessFileThread[] procFile = new ProcessFileThread[numThreads];
			AnalysisExecutionMetadata executionMetadata = new AnalysisExecutionMetadata(
					execId, execName, execType, computation);

			int thread_id = 0;
			int fi = 0;
			System.out.println("Processing [" + fi + "]: " + inpFile);
			String fileName = (new File(inpFile)).getName();
			FileWriter outFileWriter = new FileWriter(outFolder + "/" + fileName
					+ ".json");

			int check_done = 0;
			while (check_done == 0) {
				if (procFile[thread_id] == null
						|| !procFile[thread_id].isAlive()) {
					ProcessAperioXMLFile aperioXMLFile;
					aperioXMLFile = new ProcessAperioXMLFile(inpFile,
							executionMetadata, img_width, img_height, shiftX,
							shiftY, normalize, caseID, outFileWriter);

					aperioXMLFile.setColor(colorVal);
					aperioXMLFile.setCaseID(caseID);
					aperioXMLFile.setSubjectID(caseID);

					procFile[thread_id] = new ProcessFileThread(aperioXMLFile);
					check_done = 1;
				}
			}

			// Finishing Threads
			loop(procFile,numThreads);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}
    
    public static void handleMaskFile() {
        String outFolder = CommandLineArguments.getOutFoldername();
        if (outFolder==null) outFolder = "./";
        String inpFile   = CommandLineArguments.getInpFile();
        if (inpFile==null) {
        	System.err.println("Need an input file.");
        	return;
        }
        
		String caseID = CommandLineArguments.getCaseID();
		if (caseID == null) caseID = "NO-CASE-ID"; 

		boolean normalize = CommandLineArguments.isNormalize();
		int img_width     = CommandLineArguments.getWidth();
		int img_height    = CommandLineArguments.getHeight();
		if (normalize && (img_width == -1 || img_height == -1)) {
			System.err
					.println("Please enter valid image width and height (>0) values.");
			return;
		}
		int shiftX = CommandLineArguments.getShiftX();
		int shiftY = CommandLineArguments.getShiftY();

		String execId      = CommandLineArguments.getAnalysisID();
		String execType    = CommandLineArguments.getAnalysisType();
		String computation = "segmentation";
		String execName    = CommandLineArguments.getAnalysisTitle();
		String colorVal    = CommandLineArguments.getColor();
        
        int numThreads = 1;
        try {
            ProcessFileThread[] procFile = new ProcessFileThread[numThreads];
            AnalysisExecutionMetadata executionMetadata = new AnalysisExecutionMetadata(
                    execId, execName, execType, computation);

            int thread_id = 0;
            int fi = 0;
            System.out.println("Processing [" + fi + "]: " + inpFile);
            String fileName = (new File(inpFile)).getName();
            FileWriter outFileWriter = new FileWriter(outFolder + "/" + fileName + ".json");

            int check_done = 0;
            while (check_done == 0) {
            	if (procFile[thread_id] == null || !procFile[thread_id].isAlive()) {
            		ProcessMaskFile maskFile;
            		maskFile = new ProcessMaskFile(inpFile,
            				executionMetadata, 
            				img_width, img_height,shiftX, shiftY,
            				normalize,caseID,
            				outFileWriter);
            		maskFile.setColor(colorVal);
            		maskFile.setCaseID(caseID);
            		maskFile.setSubjectID(caseID);
            		procFile[thread_id] = new ProcessFileThread(maskFile);
            		check_done = 1;
            	}
            	thread_id = (thread_id + 1) % numThreads;
            	Thread.sleep(500);
            }
            
            // Finishing Threads
            loop(procFile,numThreads);
            
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static void loop(ProcessFileThread[] procFile, int numThreads) {
        System.out.println("Finishing threads.");

        int[] check_thread = new int[numThreads];

        for (int i = 0; i < numThreads; i++)
            check_thread[i] = 0;

        int all_done = 0;
        int thread_id = 0;

        while (all_done < numThreads) {
            if (procFile[thread_id] == null && check_thread[thread_id] == 0) {
                check_thread[thread_id] = 1;
                all_done++;
            }
            if (procFile[thread_id] != null && check_thread[thread_id] == 0
                    && !procFile[thread_id].isAlive()) {
                check_thread[thread_id] = 1;
                all_done++;
            }
            thread_id = (thread_id + 1) % numThreads;
            try {
                Thread.sleep(500);
            } catch (java.lang.InterruptedException insomnia) {
                insomnia.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {

        CommandLineArguments.initCommandLineOptions();
        System.out.println("Parsing the command line arguments\n");
        if (!CommandLineArguments.parseCommandLineArgs(args)) {
            CommandLineArguments.printUsage();
            return;
        }

        try {
            if (CommandLineArguments.isTSV()) {
                boolean isGeoJSON = true;
                handleFile(new ProcessTSVSBUFile(isGeoJSON),6);

                //isGeoJSON = false;
                //handleFile(new ProcessTSVSBUFile(isGeoJSON));


            } else if (CommandLineArguments.isCSV()) {
                handleFile(new ProcessNewCSVFile(),6);
            } else if (CommandLineArguments.isMaskTile()) {
                handleMaskTile();
            } else if (CommandLineArguments.isMaskFile()) {
                handleMaskFile();
            } else if (CommandLineArguments.isAperio()) {
            	handleAperioXMLFile();
            } else {
                System.err.println("Unknown input type.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
