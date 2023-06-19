package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.base.ISearchCondition;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.QuotedTripleDataSetMemory;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.TripleDataSetMemory;

import org.apache.jena.ontology.OntModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A parser for NT files. Mainly implemented to support {@link NtStarMemoryWalkGenerator#getRandomTripleForSubject(String)} in
 * an efficient way.
 */
public class NtStarMemoryWalkGenerator extends MemoryStarWalkGenerator {

   	Prefix pref = new Prefix();

    /**
     * Default logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NtStarMemoryWalkGenerator.class);

    /**
     * returns true if a triple shall be excluded.
     */
    private ISearchCondition skipCondition;


    /**
     * Indicator whether an optimized file shall be written for quick parsing later on (will be written in ./optimized/)
     */
    boolean isWriteOptimizedFile = false;

    /**
     * Default Constructor
     */
    public NtStarMemoryWalkGenerator(){
        this(false);
    }

    /**
     * Constructor
     * @param isParseDatatypeTriples True if datatype triples shall be parsed.
     */
    public NtStarMemoryWalkGenerator(boolean isParseDatatypeTriples) {
        data = new QuotedTripleDataSetMemory();

        // set parsing option:
        setParseDatatypePropertiesNoCheck(isParseDatatypeTriples);

        // set default function (do nothing)
        uriShortenerFunction = s -> s;
    }

    /**
     * Constructor
     *
     * @param pathToTripleFile The nt file to be read (not zipped).
     * @param uriShortenerFunction The URI shortener function.
     */
    public NtStarMemoryWalkGenerator(String pathToTripleFile, UnaryOperator<String> uriShortenerFunction) {
        this();
        this.uriShortenerFunction = uriShortenerFunction;
        readNTriples(pathToTripleFile);
    }

    /**
     * Constructor
     *
     * @param pathToTripleFile The nt file to be read (not zipped).
     */
    public NtStarMemoryWalkGenerator(String pathToTripleFile) {
        this(pathToTripleFile, false);
    }

    /**
     * Constructor
     *
     * @param pathToTripleFile The nt file to be read (not zipped).
     * @param isParseDatatypeTriples True if datatype properties shall also be parsed.
     */
    public NtStarMemoryWalkGenerator(String pathToTripleFile, boolean isParseDatatypeTriples) {
        this(isParseDatatypeTriples);
        readNTriples(pathToTripleFile);
    }

    /**
     * Constructor
     *
     * @param tripleFile The nt file to be read (not zipped).
     * @param uriShortenerFunction The URI shortener function which maps from String to String.
     */
    public NtStarMemoryWalkGenerator(File tripleFile, UnaryOperator<String> uriShortenerFunction) {
        this();
        this.uriShortenerFunction = uriShortenerFunction;
        readNTriples(tripleFile, false);
    }

    public NtStarMemoryWalkGenerator(File tripleFile){
        this(tripleFile, false);
    }

    /**
     * Constructor
     *
     * @param tripleFile The nt file to be read (not zipped).
     * @param isParseDatatypeTriples True if datatype triples shall also be parsed.
     */
    public NtStarMemoryWalkGenerator(File tripleFile, boolean isParseDatatypeTriples) {
        this(isParseDatatypeTriples);
        readNTriples(tripleFile, false);
    }

    /**
     * Save an ontModel as TTL file.
     *
     * @param ontModel              Model to Write.
     * @param filePathToFileToWrite File that shall be written.
     */
    public static void saveAsNt(OntModel ontModel, String filePathToFileToWrite) {
        saveAsNt(ontModel, new File(filePathToFileToWrite));
    }

    /**
     * Save an ontModel as TTL file.
     *
     * @param ontModel    Model to Write.
     * @param fileToWrite File that shall be written.
     */
    public static void saveAsNt(OntModel ontModel, File fileToWrite) {
        try {
            ontModel.write(new FileWriter(fileToWrite), "N-Triples");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the given nt file into memory.
     *
     * @param pathToFile Path to the file.
     */
    public void readNTriples(String pathToFile) {
        readNTriples(pathToFile, false);
    }

    public void readNTriples(File pathToFile) {
        readNTriples(pathToFile, false);
    }

    /**
     * Will load all .nt and .gz files from the given directory.
     *
     * @param pathToDirectory The directory where the n-triple files reside.
     */
    public void readNTriplesFilesFromDirectory(String pathToDirectory) {
        File directoryOfDataSets = new File(pathToDirectory);
        if (!directoryOfDataSets.isDirectory()) {
            LOGGER.error("The given pathToDirectory is no directory, aborting. (given: " + pathToDirectory + ")");
            return;
        }
        for (File file : directoryOfDataSets.listFiles()) {
            LOGGER.info("Processing file " + file.getName());
            if (file.getName().endsWith(".gz")) {
                readNTriples(file, true);
            } else if (file.getName().endsWith(".nt") || file.getName().endsWith(".ttl")) {
                readNTriples(file, false);
            } else {
                LOGGER.info("Skipping file: " + file.getName());
            }
        }
    }

    /**
     * A new thread will be opened for each file.
     *
     * @param pathToDirectory      The path to the directory in which the individual data files reside.
     * @param isWriteOptimizedFile Indicator whether an optimized file shall be written for quick parsing later on
     *                             (will be written in ./optimized/)
     */
    public void readNtTriplesFromDirectoryMultiThreaded(String pathToDirectory, boolean isWriteOptimizedFile) {
        this.readNtTriplesFromDirectoryMultiThreaded(new File(pathToDirectory), isWriteOptimizedFile);
    }

    /**
     * A new thread will be opened for each file.
     *
     * @param directoryOfDataSets  The directory in which the individual data files reside.
     * @param isWriteOptimizedFile Indicator whether an optimized file shall be written for quick parsing later on
     *                             (will be written in ./optimized/)
     */
    public void readNtTriplesFromDirectoryMultiThreaded(File directoryOfDataSets, boolean isWriteOptimizedFile) {
        String pathToDirectory = directoryOfDataSets.getAbsolutePath();
        this.isWriteOptimizedFile = isWriteOptimizedFile;
        if (!directoryOfDataSets.isDirectory()) {
            LOGGER.error("The given pathToDirectory is no directory, aborting. (given: " + pathToDirectory + ")");
            return;
        }

        HashMap<String, File> optimizedFiles = new HashMap<>();
        // check for optimized files
        File optimizedDirectory = new File("./optimized");
        if (optimizedDirectory.exists() && optimizedDirectory.isDirectory()) {
            LOGGER.info("Found optimized directory. Will use it for reading.");
            for (File optimizedFile : optimizedDirectory.listFiles()) {
                optimizedFiles.put(optimizedFile.getName(), optimizedFile);
            }
        }

        ArrayList<Thread> allThreads = new ArrayList<>();
        for (File fileOriginal : directoryOfDataSets.listFiles()) {
            if (optimizedFiles.containsKey(fileOriginal.getName())) {
                LOGGER.info("Found optimized file for " + fileOriginal.getName() + ", will use that one.");
                FileReaderThread zThread = new FileReaderThread(this, optimizedFiles.get(fileOriginal.getName()), true, true);
                zThread.start();
                allThreads.add(zThread);
            } else if (fileOriginal.getName().endsWith(".gz")) {
                FileReaderThread zThread = new FileReaderThread(this, fileOriginal, true, false);
                zThread.start();
                allThreads.add(zThread);
            } else if (fileOriginal.getName().endsWith(".nt") || fileOriginal.getName().endsWith(".ttl")) {
                FileReaderThread zThread = new FileReaderThread(this, fileOriginal, false, false);
                zThread.start();
                allThreads.add(zThread);
            } else {
                LOGGER.info("Skipping file: " + fileOriginal.getName());
            }
        }

        // wait for thread completion
        try {
            for (Thread thread : allThreads) {
                thread.join();
            }
        } catch (InterruptedException ie) {
            LOGGER.error("Problem waiting for thread...", ie);
        }
        LOGGER.info("Data read.");
    }

    /**
     * Thread that allows concurrent file parsing (used for data sets that consist of multiple, potentially zipped
     * files).
     */
    static class FileReaderThread extends Thread {


        public FileReaderThread(NtStarMemoryWalkGenerator parser, File fileToRead, boolean gzipped, boolean optimized) {
            this.fileToRead = fileToRead;
            this.parser = parser;
            this.isGzipped = gzipped;
            this.isOptimizedFile = optimized;
        }

        private final boolean isOptimizedFile;
        private final NtStarMemoryWalkGenerator parser;
        private final File fileToRead;
        private final boolean isGzipped;

        @Override
        public void run() {
            if (!isOptimizedFile) {
                LOGGER.info("STARTED thread for file " + fileToRead.getName());
                parser.readNTriples(fileToRead, isGzipped);
            } else {
                LOGGER.info("STARTED (optimized) thread for file " + fileToRead.getName());
                parser.readNTriplesOptimized(fileToRead);
            }
            LOGGER.info("Thread for file " + fileToRead.getName() + " completed.");
        }
    }

    /**
     * Read the given nt file into memory. This method will add the data in the file to the existing {@link NtStarMemoryWalkGenerator#data} store.
     *
     * @param pathToFile    Path to the file.
     * @param isGzippedFile Indicator whether the given file is gzipped.
     */
    public void readNTriples(String pathToFile, boolean isGzippedFile) {
        File fileToReadFrom = new File(pathToFile);
        readNTriples(fileToReadFrom, isGzippedFile);
    }

    /**
     * read form an optimized file.
     *
     * @param fileToReadFrom Optimized file.
     */
    public void readNTriplesOptimized(File fileToReadFrom) {
        if (!fileToReadFrom.exists()) {
            LOGGER.error("File does not exist. Cannot parse.");
            return;
        }
        try {
            GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(fileToReadFrom));
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));

            String readLine;
            //int lineNumber = 0;
            while ((readLine = reader.readLine()) != null) {
                //lineNumber += 1;
                String[] parsed = readLine.split(" ");
                if (parsed.length != 3) {
                    LOGGER.error("Problem with line: \n" + readLine);
                } else {
                    String subject = parsed[0];
                    String predicate = parsed[1];
                    String object = parsed[2];
                    data.addObjectTriple(subject, predicate, object);
                }
            }
        } catch (IOException ioe) {
            LOGGER.error("Could not initialize optimized reader for file " + fileToReadFrom.getName());
        }
    }
    
    public boolean checkQtIsTail(String[] spo, int i) {
    	boolean qtIsTail = false;
		if (i == spo.length-1) {
			qtIsTail = true;
		} else {
			if (spo[i+1].equals(">>")) {
				qtIsTail = true;
			} else {
				qtIsTail = false;
			}
		}
		return qtIsTail;
    }
    
    public void readQuotedTriples(String[] spo, BufferedWriter writer) {
    	try {
	    	Deque<String> subjectStack = new ArrayDeque<>();
	    	Deque<String> predicateStack = new ArrayDeque<>();
	    	Deque<String> objectStack = new ArrayDeque<>();
	    	String qt = "";
	    	String subject = "";
	    	String predicate = "";
	    	String[] objectTokens = {};
	    	String object = "";
//	    	System.out.println(String.join(" ", spo));
    		for (int i=0; i<spo.length; i++) {
    	    	if (spo[i].equals("<<")) {
    				subject = "";
    				predicate = "";
    				object = "";
    			} else if(spo[i].equals(">>")) {
					// create a quoted triple
					qt = "<<" + subject + "-" + predicate + "-" + object + ">>";
//					if (!(object.endsWith("\"@en") || object.endsWith("\"@ja") || object.endsWith("\""))) {
						// not datatype triple
					data.addObjectQuotedTriple(qt, subject, predicate, object);
//					}
					boolean qtIsTail = checkQtIsTail(spo, i);
    				if (qtIsTail) {
    					// The quoted triple node is an object of a statement.
    					subject = subjectStack.pop();
    					predicate = predicateStack.pop();
    					object = qt;
    					data.addObjectTriple(subject, predicate, object);
    					if (isWriteOptimizedFile) {
                            writer.write(subject + " " + predicate + " " + object + "\n");
                        }
    				} else {
    					// The quoted triple node is a subject of a statement.
    					subjectStack.push(qt);
    					subject = qt;
    					predicate = "";
        				object = "";
    				}
    			} else {
    				// qt内のsubject, predicate, objectをそれぞれstackに追加
    				if (subject.equals("")) {
    					subject = uriShortenerFunction.apply(removeTags(spo[i]));
    					subject = pref.replaceURI(subject).intern();
    					subjectStack.push(subject);
    				} else if (predicate.equals("")) {
    					predicate = uriShortenerFunction.apply(removeTags(spo[i]));
    					predicate = pref.replaceURI(predicate).intern();
    					predicateStack.push(predicate);
    				} else if (object.equals("")) {
    					if (spo[i].startsWith("\"")) {
    						//Literal
    						int literalEnd = 0;
    						for (int j=0; j<spo.length; j++) {
    							if ((spo[i+j].endsWith("\"@en") || spo[i+j].endsWith("\"@ja") || spo[i+j].endsWith("\""))) {
    								literalEnd = i + j;
    								break;
    							}
    						}
    						objectTokens = Arrays.copyOfRange(spo, i, literalEnd);
    						if (objectTokens.length > 1) {
    							object = textProcessingFunction.apply(String.join(" ", objectTokens));
    						} else {
    							object = spo[i];
    						}
    						
    						subject = subjectStack.pop();
    						predicate = predicateStack.pop();
    								
//    						data.addDatatypeTriple(subject, predicate, object);
    						if (isWriteOptimizedFile) {
                                writer.write(subject + " " + predicate + " " + object + "\n");
                            }
    					} else {
    						object = uriShortenerFunction.apply(removeTags(spo[i]));
    						object = pref.replaceURI(object).intern();
    						
    						subject = subjectStack.pop();
    						predicate = predicateStack.pop();
    						
    						data.addObjectTriple(subject, predicate, object);
    						if (isWriteOptimizedFile) {
                                writer.write(subject + " " + predicate + " " + object + "\n");
                            }
    					}
    					
    				}
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }

    /**
     * Read the given nt file into memory. This method will add the data in the file to the existing {@link NtStarMemoryWalkGenerator#data} store.
     *
     * @param fileToReadFrom the file.
     * @param isGzippedFile  Indicator whether the given file is gzipped.
     */
    public void readNTriples(File fileToReadFrom, boolean isGzippedFile) {
        if (!fileToReadFrom.exists()) {
            LOGGER.error("File does not exist. Cannot parse.");
            return;
        }
        BufferedWriter writer = null; // the writer used to write the optimized file
        if (isWriteOptimizedFile) {
            try {
                File fileToWrite = new File("./optimized/" + fileToReadFrom.getName());
                fileToWrite.getParentFile().mkdirs();
                GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(fileToWrite));
                writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8));
                LOGGER.info("Writer initialized.");
            } catch (FileNotFoundException fnfe) {
                LOGGER.error("Could not initialize gzip output stream.", fnfe);
            } catch (IOException e) {
                LOGGER.error("Problem initializing gzip output stream.", e);
            }
        }

        try {
            BufferedReader reader;
            if (isGzippedFile) {
                GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(fileToReadFrom));
                reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToReadFrom), StandardCharsets.UTF_8));
            }
            String readLine;
            long lineNumber = 0;
            Matcher datatypeMatcher; // only required if datatype properties shall be included
            nextLine:
            while ((readLine = reader.readLine()) != null) {
                try {
                    lineNumber++;
                    if (skipCondition.isHit(readLine)) {
                        continue nextLine;
                    }

                    // remove the dot at the end of a statement
                    readLine = readLine.replaceAll("(?<=>)*[ ]*.[ ]*$", "");

                    // legacy function:
                    /*
                    if (isIncludeDatatypeProperties) {
                        datatypeMatcher = datatypePattern.matcher(readLine);
                        if (datatypeMatcher.find()) {
                            String datatypeValue = datatypeMatcher.group(0);
                            String newDatatypeValue = datatypeValue.replaceAll(" ", "_");
                            readLine = readLine.replace(datatypeValue, newDatatypeValue);
                        }
                    }
                    */
                    
                    String[] spo = readLine.split(" ");

                    // There are quoted triples in the statement.
                    if (readLine.contains("<<")) {
                    	readQuotedTriples(spo, writer);
                    } else {
                    	if (spo[2].startsWith("\"")) {
                            String subject = uriShortenerFunction.apply(removeTags(spo[0]));
                            subject = pref.replaceURI(subject).intern();
                            String predicate = uriShortenerFunction.apply(removeTags(spo[1]));
                            predicate = pref.replaceURI(predicate).intern();
                            String[]  objectTokens = Arrays.copyOfRange(spo, 2, spo.length);
                            String object = textProcessingFunction.apply(String.join(" ", objectTokens));
                            if (isWriteOptimizedFile) {
                                writer.write(subject + " " + predicate + " " + object + "\n");
                            }
//                            data.addDatatypeTriple(subject, predicate, object);

                        } else {
                            if (spo.length != 3) {
                                LOGGER.error("Error in file " + fileToReadFrom.getName() + " in line " + lineNumber + " while parsing the following line:\n" + readLine + "\n Required tokens: 3\nActual tokens: " + spo.length);
                                int i = 1;
                                for (String token : spo) {
                                    LOGGER.error("Token " + i++ + ": " + token);
                                }
                                LOGGER.error("Line is ignored. Parsing continues.");
                                continue nextLine;
                            }
                            String subject = uriShortenerFunction.apply(removeTags(spo[0]));
                            subject = pref.replaceURI(subject).intern();
                            String predicate = uriShortenerFunction.apply(removeTags(spo[1]));
                            predicate = pref.replaceURI(predicate).intern();
                            String object = uriShortenerFunction.apply(removeTags(spo[2]));
                            object = pref.replaceURI(object).intern();
                            data.addObjectTriple(subject, predicate, object);
                            if (isWriteOptimizedFile) {
                                writer.write(subject + " " + predicate + " " + object + "\n");
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    // it is important that the parsing continues no matter what happens
                    LOGGER.error("A problem occurred while parsing line number " + lineNumber + " of file " + fileToReadFrom.getName(), e);
                    LOGGER.error("The problem occurred in the following line:\n" + readLine);
                }
            } // end of while loop
            LOGGER.info("File " + fileToReadFrom.getName() + " successfully read. " + data.getObjectTripleSize() + " subjects loaded.");
            if (isWriteOptimizedFile) {
                writer.flush();
                writer.close();
            }
            reader.close();
        } catch (Exception e) {
            LOGGER.error("Error while parsing file.", e);
        }
    }

    public ISearchCondition getSkipCondition() {
        return skipCondition;
    }

    public void setSkipCondition(ISearchCondition skipCondition) {
        this.skipCondition = skipCondition;
    }

    /**
     * This method will remove a leading less-than and a trailing greater-than sign (tags).
     *
     * @param stringToBeEdited The string that is to be edited.
     * @return String without tags.
     */
    public static String removeTags(String stringToBeEdited) {
        if (stringToBeEdited.startsWith("<")) stringToBeEdited = stringToBeEdited.substring(1);
        if (stringToBeEdited.endsWith(">"))
            stringToBeEdited = stringToBeEdited.substring(0, stringToBeEdited.length() - 1);
        return stringToBeEdited;
    }

    /**
     * Note that this function will overwrite the skip condition.
     *
     * @param includeDatatypeProperties Indicator whether data type properties shall be included in the walk generation.
     */
    @Override
    public void setParseDatatypeProperties(boolean includeDatatypeProperties) {
        // return if nothing changed:
        if(isParseDatatypeProperties() == includeDatatypeProperties) return;
        LOGGER.warn("Overwriting skip condition.");
        setParseDatatypePropertiesNoCheck(includeDatatypeProperties);
    }

    /**
     * Overwrite the skip condition without checking for current state.
     * @param includeDatatypeProperties Indicator whether data type properties shall be included in the walk generation.
     */
    private void setParseDatatypePropertiesNoCheck(boolean includeDatatypeProperties){
        // from false to true:
        if(includeDatatypeProperties) {
            skipCondition = input -> {
                if (input.trim().startsWith("#")) return true; // just a comment line
                return input.trim().equals(""); // empty line
            };
        } else {
            skipCondition = new ISearchCondition() {
                final Pattern pattern = Pattern.compile("\".*\"");

                @Override
                public boolean isHit(String input) {
                	if (input.contains("<<")) return false;
                    if (input.trim().startsWith("#")) return true; // just a comment line
                    if (input.trim().equals("")) return true; // empty line
                    Matcher matcher = pattern.matcher(input);
                    return matcher.find();
                }
            };
        }
        super.isParseDatatypeProperties = includeDatatypeProperties;
    }

	@Override
	public List<String> generateDuplicateFreeStarRandomWalksForEntity(String entity, int numberOfWalks, int depth,
			double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject,
			double probabilityFromSubjectToQt) {
		// TODO Auto-generated method stub
		return null;
	}
}
