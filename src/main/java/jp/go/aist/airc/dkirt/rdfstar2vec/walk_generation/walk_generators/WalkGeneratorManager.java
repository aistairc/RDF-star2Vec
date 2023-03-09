package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.riot.Lang;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.entity_selector.EntitySelector;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.entity_selector.HdtEntitySelector;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.entity_selector.MemoryEntitySelector;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.entity_selector.MemoryStarEntitySelector;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.entity_selector.OntModelEntitySelector;

import static jp.go.aist.airc.dkirt.rdfstar2vec.util.Util.readOntology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

/**
 * This class provides management (utility) functions for different walk generators, for example when it comes to
 * deciding which walk generator to use for which file.
 */
public class WalkGeneratorManager {


    private final static Logger LOGGER = LoggerFactory.getLogger(WalkGeneratorManager.class);

    /**
     * Given a triple file, this method determines the appropriate parser and entity selector.
     * @param tripleFile The triple file path of the file to be processed.
     * @return Pair with parser and entity selector.
     */
    public static Pair<IWalkGenerator, EntitySelector> parseSingleFile(String tripleFile){
        return parseSingleFile(new File(tripleFile), false);
    }

    /**
     * Given a triple file, this method determines the appropriate parser and entity selector.
     * @param tripleFile The triple file path of the file to be processed.
     * @param isParseDatatypeTriples True if datatype triples shall also be parsed.
     * @return Pair with parser and entity selector.
     */
    public static Pair<IWalkGenerator, EntitySelector> parseSingleFile(String tripleFile, boolean isParseDatatypeTriples){
        return parseSingleFile(new File(tripleFile), isParseDatatypeTriples);
    }

    /**
     * Given a triple file, this method determines the appropriate parser and entity selector.
     * @param tripleFile The triple file to be processed.
     * @param isParseDatatypeTriples True if datatype properties shall also be parsed.
     * @return Pair with parser and entity selector.
     */
    public static Pair<IWalkGenerator, EntitySelector> parseSingleFile(File tripleFile, boolean isParseDatatypeTriples){
        IWalkGenerator parser = null;
        EntitySelector entitySelector = null;
        try {
            String pathToTripleFile = tripleFile.getAbsolutePath();
            String fileName = tripleFile.getName();
            
            boolean rdfstar = false;
			try {
				 FileReader fr = new FileReader(tripleFile);
				 BufferedReader br = new BufferedReader(fr);
				 String line;
				 while((line = br.readLine()) != null) {
					 if (line.contains("<<")) {
						 rdfstar = true;
						 break;
					 }
				 }
				 br.close();
				 fr.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            
            if (fileName.toLowerCase().endsWith(".nt") | fileName.toLowerCase().endsWith(".nq")) {
                if(fileName.toLowerCase().endsWith(".nq")){
                    LOGGER.info("NQ File detected: Please note that the graph information will be skipped.");
                }
                if (rdfstar) {
                	LOGGER.info("Load RDF-star using NtParser.");
                	parser = new NtStarMemoryWalkGenerator(pathToTripleFile, isParseDatatypeTriples);
                    entitySelector = new MemoryStarEntitySelector(((NtStarMemoryWalkGenerator) parser).getData());
                } else {
                	try {
                        LOGGER.info("Using NxParser.");
                        parser = new NxMemoryWalkGenerator(pathToTripleFile, isParseDatatypeTriples);
                        entitySelector = new MemoryEntitySelector(((NxMemoryWalkGenerator) parser).getData());
                    } catch (Exception e) {
                        LOGGER.error("There was a problem using the default NxParser. Retry with slower NtParser.");
                        parser = new NtMemoryWalkGenerator(pathToTripleFile, isParseDatatypeTriples);
                        entitySelector = new MemoryEntitySelector(((NtMemoryWalkGenerator) parser).getData());
                    }
                    if (((MemoryWalkGenerator) parser).getDataSize() == 0L) {
                        LOGGER.error("There was a problem using the default NxParser. Retry with slower NtParser.");
                        parser = new NtMemoryWalkGenerator(pathToTripleFile, isParseDatatypeTriples);
                        entitySelector = new MemoryEntitySelector(((NtMemoryWalkGenerator) parser).getData());
                    }
                }
            } else if (fileName.toLowerCase().endsWith(".ttl")) {
                OntModel model = readOntology(pathToTripleFile, Lang.TTL);
                entitySelector = new OntModelEntitySelector(model);
                File newResourceFile = new File(tripleFile.getParent(), fileName.substring(0, fileName.length() - 3) + "nt");
                if (rdfstar) {
                	NtStarMemoryWalkGenerator.saveAsNt(model, newResourceFile); 
                	parser = new NtStarMemoryWalkGenerator(newResourceFile, isParseDatatypeTriples);
                } else {
                	NtMemoryWalkGenerator.saveAsNt(model, newResourceFile);
                    parser = new NtMemoryWalkGenerator(newResourceFile, isParseDatatypeTriples);
                }
            } else if (fileName.toLowerCase().endsWith(".xml") || fileName.toLowerCase().endsWith(".rdf")) {
                OntModel model = readOntology(pathToTripleFile, Lang.RDFXML);
                entitySelector = new OntModelEntitySelector(model);
                File newResourceFile = new File(tripleFile.getParent(), fileName.substring(0, fileName.length() - 3) + "nt");
                //this.parser = new JenaOntModelMemoryParser(this.model, this);
                if (rdfstar) {
                	NtStarMemoryWalkGenerator.saveAsNt(model, newResourceFile);
                    parser = new NtStarMemoryWalkGenerator(newResourceFile, isParseDatatypeTriples);
                } else {
                	NtMemoryWalkGenerator.saveAsNt(model, newResourceFile);
                    parser = new NtMemoryWalkGenerator(newResourceFile, isParseDatatypeTriples);
                }
            } else if (fileName.toLowerCase().endsWith(".hdt") || fileName.toLowerCase().endsWith(".hdt.index.v1-1")) {
                LOGGER.info("HDT file detected. Using HDT parser.");
                try {
                    parser = new HdtWalkGenerator(pathToTripleFile);
                    entitySelector = new HdtEntitySelector(pathToTripleFile);
                } catch (IOException ioe) {
                    LOGGER.error("Propagated HDT Initializer Exception", ioe);
                }
            }
            LOGGER.info("Model read into memory.");
        } catch (MalformedURLException mue) {
            LOGGER.error("Path seems to be invalid. Generator not functional.", mue);
        }
        return new Pair<>(parser, entitySelector);
    }
}
