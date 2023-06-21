package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import jp.go.aist.airc.dkirt.rdfstar2vec.util.Util;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.QuotedTriple;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.QuotedTripleDataSetMemory;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.Triple;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.TripleDataSetMemory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Memory based walk generator using the {@link QuotedTripleDataSetMemory} data structure.
 * These kind of walk generators load the complete model into memory.
 */
public abstract class MemoryStarWalkGenerator implements IWalkGenerator,
        IMidWalkCapability, IMidWalkDuplicateFreeCapability, IRandomWalkDuplicateFreeCapability,
        IMidWalkWeightedCapability, IMidEdgeWalkDuplicateFreeCapability, IRandomWalkCapability,
        IMidTypeWalkDuplicateFreeCapability, INodeWalksDuplicateFreeCapability, 
        IStarMidWalkCapability, IStarMidWalkDuplicateFreeCapability, 
        IStarRandomWalkCapability, IStarRandomWalkDuplicateFreeCapability {


    /**
     * The actual data structure, i.e. a set of triples.
     */
    QuotedTripleDataSetMemory data;

    /**
     * Default logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryStarWalkGenerator.class);

    /**
     * Indicator whether anonymous nodes shall be handled as if they were just one node.
     * E.g. _:genid413438 is handled like -&gt; ANODE
     */
    boolean isUnifyAnonymousNodes = false;

    /**
     * By default false.
     */
    boolean isParseDatatypeProperties = false;

    /**
     * Function to transform URIs while parsing.
     */
    UnaryOperator<String> uriShortenerFunction;

    /**
     * Function to transform data type text.
     */
    UnaryOperator<String> textProcessingFunction = new TextProcessor();

    /**
     * Only required for {@link IMidTypeWalkDuplicateFreeCapability}.
     */
    private Set<String> typeProperties = new HashSet<>();

    private static final String[] DEFAULT_TYPE_PROPERTIES = {"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"};

    /**
     * Constructor
     */
    public MemoryStarWalkGenerator(){
        typeProperties.addAll(Arrays.asList(DEFAULT_TYPE_PROPERTIES));
    }

    /**
     * Weighted mid walk: If there are more options to go forward, it is more likely to go forward.
     * The walks are duplicate free.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks Number of walks to be performed per entity.
     * @param depth         The depth of the walk. Depth is defined as hop to the next node. A walk of depth 1 will have three walk components.
     * @return List of walks.
     */
    @Override
    public List<String> generateWeightedMidWalksForEntity(String entity, int numberOfWalks, int depth) {
        return Util.convertToStringWalksDuplicateFree(generateWeightedMidWalkForEntityAsArray(entity, numberOfWalks,
                depth));
    }

    /**
     * Walks of length 1, i.e., walks that contain only one node, are ignored.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks The number of walks to be performed.
     * @param depth         The depth of each walk (where the depth is the number of hops).
     * @return A data structure describing the walks.
     */
    public List<List<String>> generateWeightedMidWalkForEntityAsArray(String entity, int numberOfWalks, int depth) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < numberOfWalks; i++) {
            List<String> walk = generateWeightedMidWalkForEntity(entity, depth);
            if (walk.size() > 1) {
                result.add(walk);
            }
        }
        return result;
    }

    /**
     * Generates a single walk for the given entity with the given depth.
     *
     * @param entity The entity for which a walk shall be generated.
     * @param depth  The depth of the walk. Depth is defined as hop to the next node. A walk of depth 1 will have three walk components.
     * @return One walk as list where each element is a walk component.
     */
    public List<String> generateWeightedMidWalkForEntity(String entity, int depth) {
        LinkedList<String> result = new LinkedList<>();

        String nextElementPredecessor = entity;
        String nextElementSuccessor = entity;

        // initialize result
        result.add(entity);

        // variable to store the number of iterations performed so far
        int currentDepth = 0;

        while (currentDepth < depth) {
            currentDepth++;

            // randomly decide whether to use predecessors or successors
            double randomPickZeroOne = ThreadLocalRandom.current().nextDouble(0.0, 1.00000001);

            // predecessor candidates
            List<Triple> candidatesPredecessor = data.getObjectTriplesInvolvingObject(nextElementPredecessor);

            // successor candidates
            List<Triple> candidatesSuccessor = data.getObjectTriplesInvolvingSubject(nextElementSuccessor);

            double numberOfPredecessors = 0.0;
            double numberOfSuccessors = 0.0;

            if (candidatesPredecessor != null) numberOfPredecessors = candidatesPredecessor.size();
            if (candidatesSuccessor != null) numberOfSuccessors = candidatesSuccessor.size();

            // if there are no successors and predecessors: return current walk
            if (numberOfPredecessors == 0 && numberOfSuccessors == 0) return result;

            // determine cut-off point
            double cutOffPoint = numberOfPredecessors / (numberOfPredecessors + numberOfSuccessors);

            if (randomPickZeroOne <= cutOffPoint) {
                // predecessor
                if (candidatesPredecessor != null && candidatesPredecessor.size() > 0) {
                    Triple drawnTriple = randomDrawFromList(candidatesPredecessor);

                    // add walks from the front (walk started before entity)
                    result.addFirst(drawnTriple.predicate);
                    result.addFirst(drawnTriple.subject);
                    nextElementPredecessor = drawnTriple.subject;
                }
            } else {
                // successor
                if (candidatesSuccessor != null && candidatesSuccessor.size() > 0) {
                    Triple tripleToAdd = randomDrawFromList(candidatesSuccessor);

                    // add next walk iteration
                    result.addLast(tripleToAdd.predicate);
                    result.addLast(tripleToAdd.object);
                    nextElementSuccessor = tripleToAdd.object;
                }
            }
        }
        return result;
    }

    @Override
    public List<String> generateNodeWalksForEntity(String entity, int numberOfWalks, int depth){
        List<String> fullWalks = generateDuplicateFreeRandomWalksForEntity(entity, numberOfWalks, depth);
        Set<String> finalWalks = new HashSet<>();
        for (String walk : fullWalks){
            String[] walkComponents = walk.split(" ");
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;
            for(int i = 0; i < walkComponents.length; i++){
                if(i % 2 == 0){
                    if(isFirst) {
                        sb.append(walkComponents[i]);
                        isFirst = false;
                    } else {
                        sb.append(" ").append(walkComponents[i]);
                    }
                }
            }
            finalWalks.add(sb.toString());
        }
        return new ArrayList<>(finalWalks);
    }
    
    /**
     * Generates RDF-star walks that are ready to be processed further (already concatenated, space-separated).
     *
     * @param entity        The entity for which a walk shall be generated.
     * @param depth         The depth of each walk.
     * @param numberOfWalks The number of walks to be generated.
     * @return List where every item is a walk separated by spaces.
     */
    @Override
    public List<String> generateStarMidWalksForEntity(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt) {
        return Util.convertToStringWalks(generateStarMidWalkForEntityAsArray(entity, numberOfWalks, depth, probabilityFromQtToSubject, probabilityFromObjectToQt, probabilityFromQtToObject, probabilityFromSubjectToQt));
    }
    
    /**
     * Walks of length 1, i.e., walks that contain only one node, are ignored.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks The number of walks to be performed.
     * @param depth         The depth of each walk (where the depth is the number of hops).
     * @return A data structure describing the walks.
     */
    public List<List<String>> generateStarMidWalkForEntityAsArray(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < numberOfWalks; i++) {
            List<String> walk = generateStarMidWalkForEntity(entity, depth, probabilityFromQtToSubject, probabilityFromObjectToQt, probabilityFromQtToObject, probabilityFromSubjectToQt);
            if (walk.size() > 1) {
                result.add(walk);
            }
        }
        return result;
    }

    /**
     * Generates walks that are ready to be processed further (already concatenated, space-separated).
     *
     * @param entity        The entity for which a walk shall be generated.
     * @param depth         The depth of each walk.
     * @param numberOfWalks The number of walks to be generated.
     * @return List where every item is a walk separated by spaces.
     */
    @Override
    public List<String> generateMidWalksForEntity(String entity, int numberOfWalks, int depth) {
        return Util.convertToStringWalks(generateMidWalkForEntityAsArray(entity, numberOfWalks, depth));
    }

    /**
     * Walks of length 1, i.e., walks that contain only one node, are ignored.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks The number of walks to be performed.
     * @param depth         The depth of each walk (where the depth is the number of hops).
     * @return A data structure describing the walks.
     */
    public List<List<String>> generateMidWalkForEntityAsArray(String entity, int numberOfWalks, int depth) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < numberOfWalks; i++) {
            List<String> walk = generateMidWalkForEntity(entity, depth);
            if (walk.size() > 1) {
                result.add(walk);
            }
        }
        return result;
    }
    
    /**
     * Generates star-walks that are ready to be processed further (already concatenated, space-separated).
     *
     * @param numberOfWalks The number of walks to be generated.
     * @param entity        The entity for which a walk shall be generated.
     * @param depth         The depth of each walk.
     * @return List where every item is a walk separated by spaces.
     */
    @Override
    public List<String> generateStarMidWalksForEntityDuplicateFree(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt) {
        return Util.convertToStringWalksDuplicateFree(generateStarMidWalkForEntityAsArray(entity, numberOfWalks, depth, probabilityFromQtToSubject, probabilityFromObjectToQt, probabilityFromQtToObject, probabilityFromSubjectToQt));
    }

    /**
     * Generates walks that are ready to be processed further (already concatenated, space-separated).
     *
     * @param numberOfWalks The number of walks to be generated.
     * @param entity        The entity for which a walk shall be generated.
     * @param depth         The depth of each walk.
     * @return List where every item is a walk separated by spaces.
     */
    @Override
    public List<String> generateMidWalksForEntityDuplicateFree(String entity, int numberOfWalks, int depth) {
        return Util.convertToStringWalksDuplicateFree(generateMidWalkForEntityAsArray(entity, numberOfWalks, depth));
    }

    /**
     * Generates walks that are ready to be processed further (already concatenated, space-separated).
     *
     * @param entity        The entity for which a walk shall be generated.
     * @param numberOfWalks The number of walks to be generated.
     * @param depth         The depth of each walk.
     * @return List where every item is a walk separated by spaces.
     */
    @Override
    public List<String> generateMidEdgeWalksForEntityDuplicateFree(String entity, int numberOfWalks, int depth) {
        List<List<String>> walksWithNodes = generateMidWalkForEntityAsArray(entity, numberOfWalks, depth);
        List<List<String>> result = new ArrayList<>();
        for (List<String> walkWithNodes : walksWithNodes) {

            // determine how often the entity appears (this is relevant in case there are "loops" in the walk
            // which involve the entity of interest multiple times!
            int appearances = getNumberOfAppearances(entity, walkWithNodes);

            // draw the desired position to keep
            int choice = getRandomNumberBetweenZeroAndX(appearances);

            List<String> walk = new ArrayList<>();
            int currentNodeOfInterestPosition = 0;
            for (int i = 0; i < walkWithNodes.size(); i++) {
                if (i % 2 == 0) {
                    String node = walkWithNodes.get(i);
                    if (node.equals(entity)) {
                        // we found the node of interest
                        if (currentNodeOfInterestPosition == choice) {
                            walk.add(node);
                            currentNodeOfInterestPosition++;
                        } else {
                            // -> we will not add the node of interest this time
                            currentNodeOfInterestPosition++;
                        }
                    }
                } else {
                    String edge = walkWithNodes.get(i);
                    walk.add(edge);
                }
            }
            result.add(walk);
        }
        return Util.convertToStringWalksDuplicateFree(result);
    }

    /**
     * Count how often entity appears in array and return result.
     *
     * @param entity Entity.
     * @param array  String array.
     * @return Number of times the entity appears in the array.
     */
    static int getNumberOfAppearances(String entity, Iterable<String> array) {
        int result = 0;
        for (String s : array) {
            if (s.equals(entity)) result++;
        }
        return result;
    }

    /**
     * Returns a random number between 0 and x (exclusive!).
     *
     * @param x Integer upper bound (exclusive).
     * @return Integer
     */
    static int getRandomNumberBetweenZeroAndX(int x) {
        return ThreadLocalRandom.current().nextInt(x);
    }
    
    static QuotedTriple expandQuotedTriple(QuotedTriple quotedTriple, Map<String, QuotedTriple> quotedTriples) {
    	// TODO: RDF-star-extかどうかのオプション
    	if (quotedTriple.subject.contains("<<") && quotedTriple.predicate.equals("rdf:value") && quotedTriple.object.endsWith("\"")) {
			quotedTriple = quotedTriples.get(quotedTriple.subject);
			// recursive method
			expandQuotedTriple(quotedTriple, quotedTriples);
		}
    	return quotedTriple;
    }
    
    // Check whether the given QT is wrapped with id, and if not, get the QT wrapped with id.
    static QuotedTriple envelopeQuotedTriple(QuotedTriple quotedTriple, Map<String, QuotedTriple> quotedTriples) {
    	// TODO: 入力がRDF-star-extかどうかのオプション
    	if (!quotedTriple.predicate.equals("rdf:value")) { // ignore the following QT: << << s p o>> rdf:value id >>
    		for (String key : quotedTriples.keySet()) {
    			QuotedTriple qt_tmp = quotedTriples.get(key);
    			if (quotedTriple.qt.equals(qt_tmp.subject)) {
    				return qt_tmp;
    			}
    		}
		}
    	return quotedTriple;
    }
    
    /**
     * Generates a single star-walk for the given entity with the given depth.
     *
     * @param entity The entity for which a walk shall be generated.
     * @param depth  The depth of the walk. Depth is defined as hop to the next node. A walk of depth 1 will have three walk components.
     * @param probabilityFromQtToSubject	The transition probability from a quoted triple (QT) node to its constituent subjects.
     * @param probabilityFromObjectToQt		The transition probability from an object to a quoted triple (QT) node.
     * @param probabilityFromQtToObject		The transition probability from a quoted triple (QT) node to its constituent objects.
     * @param probabilityFromSubjectToQt	The transition probability from a subject to a quoted triple (QT) node.
     * @return One walk as list where each element is a walk component.
     */
    public List<String> generateStarMidWalkForEntity(String entity, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt) {
//    	LOGGER.info(Double.toString(probabilityFromQtToSubject));
        LinkedList<String> result = new LinkedList<>();

        String nextElementPredecessor = entity;
        String nextElementSuccessor = entity;

        // initialize result
        result.add(entity);

        // variable to store the number of iterations performed so far
        int currentDepth = 0;

        Map<String, QuotedTriple> quotedTriples = data.getQuotedTriples();
        
        while (currentDepth < depth) {
            currentDepth++;

            // randomly decide whether to use predecessors or successors
            int randomPickZeroOne = ThreadLocalRandom.current().nextInt(2);

            if (randomPickZeroOne == 0) {
                // predecessor
                List<Triple> candidates = data.getObjectTriplesInvolvingObject(nextElementPredecessor);
                Triple drawnTriple = null;
                
                ArrayList<String> possibleWalkModes = new ArrayList<String>(); //List of possible walk mode to be executed because the set threshold has been exceeded. {qt2subject, qt2object, subject2qt, object2qt}
                QuotedTriple qtInvolvingEntityAsSubject = null;
                QuotedTriple qtInvolvingEntityAsObject = null;
                
                if (candidates != null && candidates.size() > 0) {
                    drawnTriple = randomDrawFromList(candidates);
                    double randFromQtToSubject = Math.random();
                    double randFromQtToObject = Math.random();
                    // Check if the entity is included as an subject in the QT
                    qtInvolvingEntityAsSubject = getRandomQtInvolvingEntityAsSubject(nextElementPredecessor, candidates, quotedTriples);
                    if (qtInvolvingEntityAsSubject != null) {
                    	if (randFromQtToSubject < probabilityFromQtToSubject) { possibleWalkModes.add("qt2subject"); }	//successorの方のトリプルと合わせる必要がある
                    }
                    
                    // Check if the entity is included as an object in the QT
                    qtInvolvingEntityAsObject = getRandomQtInvolvingEntityAsObject(nextElementPredecessor, candidates, quotedTriples);
                    if (qtInvolvingEntityAsObject != null) {
                    	if (randFromQtToObject< probabilityFromQtToObject) { possibleWalkModes.add("qt2object"); }
                    }
                }
                
                // i.e., first entity is QT
                if (nextElementPredecessor.contains("<<")) {
                	double randFromObjectToQt = Math.random();
                    double randFromSubjectToQt = Math.random();
                    if (randFromObjectToQt < probabilityFromObjectToQt) { possibleWalkModes.add("object2qt"); }
                    if (randFromSubjectToQt < probabilityFromSubjectToQt) { possibleWalkModes.add("subject2qt"); }
                }
                
                
                
                String mode = getRandomElement(possibleWalkModes);
                if (mode != null) {
                	if (mode.equals("qt2subject")) {
                		QuotedTriple quotedTriple_enveloped = envelopeQuotedTriple(qtInvolvingEntityAsSubject, quotedTriples);
                		result.addFirst(quotedTriple_enveloped.qt);
                		// context-oriented
                		result.addLast(quotedTriple_enveloped.predicate);
                		result.addLast(quotedTriple_enveloped.object);
                		nextElementPredecessor = quotedTriple_enveloped.qt;
                		nextElementSuccessor = quotedTriple_enveloped.object;
                	} else if (mode.equals("qt2object")) {
                		QuotedTriple quotedTriple_enveloped = envelopeQuotedTriple(qtInvolvingEntityAsObject, quotedTriples);
                		result.addFirst(quotedTriple_enveloped.qt);
                		nextElementPredecessor = quotedTriple_enveloped.qt;
                	} else if (mode.equals("object2qt")) {
                		QuotedTriple quotedTriple = quotedTriples.get(nextElementPredecessor);
                		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                		result.addFirst(quotedTriple_expanded.object);
                		result.addFirst(quotedTriple_expanded.predicate);
                		result.addFirst(quotedTriple_expanded.subject);
                		nextElementPredecessor = quotedTriple_expanded.subject;
                		currentDepth++;
                	} else if (mode.equals("subject2qt")) {
                		QuotedTriple quotedTriple = quotedTriples.get(nextElementPredecessor);
                		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                		result.addFirst(quotedTriple_expanded.subject);
                		nextElementPredecessor = quotedTriple_expanded.subject;
                	}
                } else {
                	if (candidates != null && candidates.size() > 0 && drawnTriple != null) {
                        // add walks from the front (walk started before entity)
                        result.addFirst(drawnTriple.predicate);
                        result.addFirst(drawnTriple.subject);
                        nextElementPredecessor = drawnTriple.subject;
                    }
                }
                
            } else {
                // successor
                List<Triple> candidates = data.getObjectTriplesInvolvingSubject(nextElementSuccessor);
                if (candidates != null && candidates.size() > 0) {
                    Triple tripleToAdd = randomDrawFromList(candidates);
                    
                    String lastTripleSubject = "";
                	String lastTriplePredicate = "";
                    String lastTripleObject = nextElementSuccessor;

                    ArrayList<String> possibleWalkModes = new ArrayList<String>(); //List of possible walk mode to be executed because the set threshold has been exceeded. {qt2subject, qt2object, subject2qt, object2qt}
                    if (nextElementSuccessor.contains("<<")) {
                    	double randFromQtToSubject = Math.random();
                        double randFromQtToObject = Math.random();
                    	if (randFromQtToSubject < probabilityFromQtToSubject) { possibleWalkModes.add("qt2subject"); }
                    	if (randFromQtToObject< probabilityFromQtToObject) { possibleWalkModes.add("qt2object"); }
                    }
                    
                    double randFromObjectToQt = Math.random();
                    double randFromSubjectToQt = Math.random();
                    // Check if the entity is included as an object in the QT
                    // resultの中に3つ以上のエンティティがある場合に限る（context-oriented）
                    if (result.size() >= 3) {
                    	//リストの末尾から３つ取り出し、そのエンティティで構成されるQTがあるかどうか調べる
                    	// get last entity
                    	lastTripleSubject = result.get(result.size() - 3);
                    	lastTriplePredicate = result.get(result.size() - 2);
                        String qt_str = "<<" + lastTripleSubject + "-" + lastTriplePredicate + "-" + nextElementSuccessor + ">>";
                        QuotedTriple quotedTriple = quotedTriples.get(qt_str);
                        if (quotedTriple != null) { 
                        	if (randFromObjectToQt < probabilityFromObjectToQt) { possibleWalkModes.add("object2qt"); }
                        }
                    }
                    
                    // Check if the entity is included as an subject in the QT
                    QuotedTriple qtInvolvingEntityAsSubject = getRandomQtInvolvingEntityAsSubject(nextElementSuccessor, candidates, quotedTriples);
                    if (qtInvolvingEntityAsSubject != null) {
                    	if (randFromSubjectToQt < probabilityFromSubjectToQt) { possibleWalkModes.add("subject2qt"); }
                    }
                    
                    String mode = getRandomElement(possibleWalkModes);
                    if (mode != null) {
                    	if (mode.equals("qt2subject")) {
                    		QuotedTriple quotedTriple = quotedTriples.get(nextElementSuccessor);
                    		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                    		// context-oriented
                    		result.addLast(quotedTriple_expanded.subject);
                    		result.addLast(quotedTriple_expanded.predicate);
                    		result.addLast(quotedTriple_expanded.object);
                    		nextElementSuccessor = quotedTriple_expanded.object;
                    	} else if (mode.equals("qt2object")) {
                    		QuotedTriple quotedTriple = quotedTriples.get(nextElementSuccessor);
                    		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                    		result.addLast(quotedTriple_expanded.object);
                    		nextElementSuccessor = quotedTriple_expanded.object;
                    	} else if (mode.equals("object2qt")) {
                    		String qt_str = "<<" + lastTripleSubject + "-" + lastTriplePredicate + "-" + nextElementSuccessor + ">>";
                            QuotedTriple quotedTriple = quotedTriples.get(qt_str);
                    		QuotedTriple quotedTriple_enveloped = envelopeQuotedTriple(quotedTriple, quotedTriples);
                    		result.addLast(quotedTriple_enveloped.qt);
                    		nextElementSuccessor = quotedTriple_enveloped.qt;
                    	} else if (mode.equals("subject2qt")) {
                    		QuotedTriple quotedTriple_enveloped = envelopeQuotedTriple(qtInvolvingEntityAsSubject, quotedTriples);
                    		result.addLast(quotedTriple_enveloped.qt);
                    		nextElementSuccessor = quotedTriple_enveloped.qt;
                    	}
                    	
                    } else {
                    	// add next walk iteration
                        result.addLast(tripleToAdd.predicate);
                        result.addLast(tripleToAdd.object);
                        nextElementSuccessor = tripleToAdd.object;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Generates a single walk for the given entity with the given depth.
     *
     * @param entity The entity for which a walk shall be generated.
     * @param depth  The depth of the walk. Depth is defined as hop to the next node. A walk of depth 1 will have three walk components.
     * @return One walk as list where each element is a walk component.
     */
    public List<String> generateMidWalkForEntity(String entity, int depth) {
        LinkedList<String> result = new LinkedList<>();

        String nextElementPredecessor = entity;
        String nextElementSuccessor = entity;

        // initialize result
        result.add(entity);

        // variable to store the number of iterations performed so far
        int currentDepth = 0;

        while (currentDepth < depth) {
            currentDepth++;

            // randomly decide whether to use predecessors or successors
            int randomPickZeroOne = ThreadLocalRandom.current().nextInt(2);

            if (randomPickZeroOne == 0) {
                // predecessor
                List<Triple> candidates = data.getObjectTriplesInvolvingObject(nextElementPredecessor);

                if (candidates != null && candidates.size() > 0) {
                    Triple drawnTriple = randomDrawFromList(candidates);

                    // add walks from the front (walk started before entity)
                    result.addFirst(drawnTriple.predicate);
                    result.addFirst(drawnTriple.subject);
                    nextElementPredecessor = drawnTriple.subject;
                }
            } else {
                // successor
                List<Triple> candidates = data.getObjectTriplesInvolvingSubject(nextElementSuccessor);
                if (candidates != null && candidates.size() > 0) {
                    Triple tripleToAdd = randomDrawFromList(candidates);

                    // add next walk iteration
                    result.addLast(tripleToAdd.predicate);
                    result.addLast(tripleToAdd.object);
                    nextElementSuccessor = tripleToAdd.object;
                }
            }
        }
        return result;
    }

    /**
     * Draw a random value from a List. This method is thread-safe.
     *
     * @param listToDrawFrom The list from which shall be drawn.
     * @param <T>            Type
     * @return Drawn value of type T.
     */
    public static <T> T randomDrawFromList(List<T> listToDrawFrom) {
        int randomNumber = ThreadLocalRandom.current().nextInt(listToDrawFrom.size());
        return listToDrawFrom.get(randomNumber);
    }

    /**
     * Draw a random value from a set. This method is thread-safe.
     *
     * @param setToDrawFrom The set from which shall be drawn.
     * @param <T> Type
     * @return Drawn value of type T.
     */
    public static<T> T randomDrawFromSet(Set<T> setToDrawFrom) {
        int randomNumber = ThreadLocalRandom.current().nextInt(setToDrawFrom.size());
        int i = 0;
        for(T t : setToDrawFrom){
            if(i == randomNumber){
                return t;
            }
            i++;
        }
        return null;
    }

    /**
     * Obtain a triple for the given subject.
     *
     * @param subject The subject for which a random predicate and object shall be found.
     * @return Triple, randomly obtained for the given subject.
     */
    public Triple getRandomTripleForSubject(String subject) {
        if (subject == null) return null;
        subject = uriShortenerFunction.apply(removeTags(subject));
        List<Triple> queryResult = data.getObjectTriplesInvolvingSubject(subject);
        if (queryResult == null) {
            // no triple found
            return null;
        }
        int randomNumber = ThreadLocalRandom.current().nextInt(queryResult.size());
        LOGGER.info("(" + Thread.currentThread().getName() + ") " + randomNumber);
        return queryResult.get(randomNumber);
    }

    /**
     * Generate text walks. This only works if datatype triples/properties were parsed previously.
     *
     * @param entity The entity for which walks shall be generated.
     * @param depth  Must be &gt; 2.
     * @return List of walks.
     */
    public List<String> generateTextWalksForEntity(String entity, int depth) {
        List<String> result = new ArrayList<>();
        Set<String> datatypeSubjects = this.data.getUniqueDatatypeTripleSubjects();
        if (!datatypeSubjects.contains(entity)) {
            return result;
        }
        Map<String, Set<String>> tuples = this.data.getDatatypeTuplesForSubject(entity);

        for (Map.Entry<String, Set<String>> entry : tuples.entrySet()) {
            String predicate = entry.getKey();
            Set<String> texts = entry.getValue();
            StringBuffer walk = getNewBufferWalk(entity, predicate);
            int currentWalkLength = 2;
            for (String text : texts) {
                for (String token : text.split(" ")) {
                    walk.append(" ").append(this.textProcessingFunction.apply(token));
                    currentWalkLength++;
                    if (currentWalkLength == depth) {
                        result.add(walk.toString());
                        walk = getNewBufferWalk(entity, predicate);
                        currentWalkLength = 2;
                    }
                }
                if (walk.length() > entity.length() + predicate.length() + 1) {
                    result.add(walk.toString());
                    walk = getNewBufferWalk(entity, predicate);
                    currentWalkLength = 2;
                }
            }
        }
        return result;
    }

    private StringBuffer getNewBufferWalk(String subject, String predicate) {
        StringBuffer walk = new StringBuffer();
        walk.append(subject).append(" ").append(predicate);
        return walk;
    }
    
    /**
     * Generates duplicate-free RDF-star walks for the given entity.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks The number of walks to be generated.
     * @param depth         The number of hops to nodes (!).
     * @param probabilityFromQtToSubject	The transition probability from a quoted triple (QT) node to its constituent subjects.
     * @param probabilityFromObjectToQt		The transition probability from an object to a quoted triple (QT) node.
     * @param probabilityFromQtToObject		The transition probability from a quoted triple (QT) node to its constituent objects.
     * @param probabilityFromSubjectToQt	The transition probability from a subject to a quoted triple (QT) node.
     * @return A list of walks where each element in the list represents a walk. The walk elements are separated by
     * spaces.
     */
    public List<String> generateDuplicateFreeStarRandomWalksForEntity(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt) {
    	List<List<String>> walks = new ArrayList<>();
        boolean isFirstIteration = true;
        Map<String, QuotedTriple> quotedTriples = data.getQuotedTriples();
        for (int currentDepth = 0; currentDepth < depth; currentDepth++) {
            // initialize with first node
            if (isFirstIteration) {
                List<Triple> neighbours = data.getObjectTriplesInvolvingSubject(entity);
                if (neighbours == null || neighbours.size() == 0) {
                    return new ArrayList<>();
                }
                ArrayList<String> possibleWalkModes = new ArrayList<String>(); //List of possible walk mode to be executed because the set threshold has been exceeded. {qt2subject, qt2object, subject2qt, object2qt}
                double randFromObjectToQt = Math.random();
                double randFromSubjectToQt = Math.random();
                
                // Check if the entity is included as an object in the QT
                List<Triple> triplesInvolvingEntityAsObject = data.getObjectTriplesInvolvingObject(entity);
                QuotedTriple qtInvolvingEntityAsObject = getRandomQtInvolvingEntityAsObject(entity, triplesInvolvingEntityAsObject, quotedTriples);
                if (qtInvolvingEntityAsObject != null ) {
                	if (randFromObjectToQt < probabilityFromObjectToQt) { possibleWalkModes.add("object2qt"); }
                }
                
                // Check if the entity is included as an subject in the QT
                QuotedTriple qtInvolvingEntityAsSubject = getRandomQtInvolvingEntityAsSubject(entity, neighbours, quotedTriples);
                if (qtInvolvingEntityAsSubject != null ) {
                	if (randFromSubjectToQt < probabilityFromSubjectToQt) { possibleWalkModes.add("subject2qt"); }
                }

                // i.e., first entity is QT
                if (entity.contains("<<")) {
                	double randFromQtToSubject = Math.random();
                    double randFromQtToObject = Math.random();
                    if (randFromQtToSubject < probabilityFromQtToSubject) { possibleWalkModes.add("qt2subject"); }
                    if (randFromQtToObject < probabilityFromQtToObject) { possibleWalkModes.add("qt2object"); }
                }
                
                String mode = getRandomElement(possibleWalkModes);
                if (mode != null) {
                	ArrayList<String> individualWalk = new ArrayList<>();
                	if (mode.equals("qt2subject")) {
                		QuotedTriple quotedTriple = quotedTriples.get(entity);
            			QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
            			individualWalk.add(quotedTriple.qt);	 // = entity
            			individualWalk.add(quotedTriple_expanded.subject);
            			// context-oriented
            			individualWalk.add(quotedTriple_expanded.predicate);
            			individualWalk.add(quotedTriple_expanded.object);
                	} else if (mode.equals("qt2object")) {
                		QuotedTriple quotedTriple = quotedTriples.get(entity);
                		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);           		
            			individualWalk.add(quotedTriple.qt);	// = entity
            			individualWalk.add(quotedTriple_expanded.object);
                	} else if (mode.equals("object2qt")) {
                		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsObject, quotedTriples);
                		individualWalk.add(entity);
                		individualWalk.add(quotedTriple.qt);
                	} else if (mode.equals("subject2qt")) {
                		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsSubject, quotedTriples);
                		individualWalk.add(entity);
                		individualWalk.add(quotedTriple.qt);
                	}
                	walks.add(individualWalk);
                } else {
                	for (Triple neighbour : neighbours) {
                        ArrayList<String> individualWalk = new ArrayList<>();
                        individualWalk.add(neighbour.subject);
                        individualWalk.add(neighbour.predicate);
                        individualWalk.add(neighbour.object);
                        walks.add(individualWalk);
                    }
                }
                
                isFirstIteration = false;
            } else {
                // create a copy
                List<List<String>> walks_tmp = new ArrayList<>(walks);

                // loop over current walks
                for (List<String> walk : walks_tmp) {
                    // get last entity
                	String lastTripleSubject = "";
                	String lastTriplePredicate = "";
                    String lastTripleObject = walk.get(walk.size() - 1);
                	if(walk.size() >= 3) {
                		lastTripleSubject = walk.get(walk.size() - 3);
                    	lastTriplePredicate = walk.get(walk.size() - 2);
                	}
                	
                	// get neighbors of lastTripleObject
                	List<Triple> nextIteration = data.getObjectTriplesInvolvingSubject(lastTripleObject);
                    if (nextIteration == null) {
                    	continue;
                    }
                    
                    String qt_str = "<<" + lastTripleSubject + "-" + lastTriplePredicate + "-" + lastTripleObject + ">>";
                    
                    ArrayList<String> possibleWalkModes = new ArrayList<String>(); //List of possible walk mode to be executed because the set threshold has been exceeded. {qt2subject, qt2object, subject2qt, object2qt}
                    double randFromObjectToQt = Math.random();
                    double randFromSubjectToQt = Math.random();
                    
                    // Check if the entity is included as an object in the QT
                    QuotedTriple qtInvolvingEntityAsObject = quotedTriples.get(qt_str);
                    if (qtInvolvingEntityAsObject != null ) {
                    	if (randFromObjectToQt < probabilityFromObjectToQt) { possibleWalkModes.add("object2qt"); }
                    }
                    
                    // Check if the entity is included as an subject in the QT
                    QuotedTriple qtInvolvingEntityAsSubject = getRandomQtInvolvingEntityAsSubject(lastTripleObject, nextIteration, quotedTriples);
                    if (qtInvolvingEntityAsSubject != null ) {
                    	if (randFromSubjectToQt < probabilityFromSubjectToQt) { possibleWalkModes.add("subject2qt"); }
                    }
                    
                    // i.e., first entity is QT
                    if (lastTripleObject.contains("<<")) {
                    	double randFromQtToSubject = Math.random();
                        double randFromQtToObject = Math.random();
                        if (randFromQtToSubject < probabilityFromQtToSubject) { possibleWalkModes.add("qt2subject"); }
                        if (randFromQtToObject < probabilityFromQtToObject) { possibleWalkModes.add("qt2object"); }
                    }
                    
                    String mode = getRandomElement(possibleWalkModes);
                    if (mode != null) {
                    	List<String> newWalk = new ArrayList<>(walk);
                    	if (mode.equals("qt2subject")) {
                    		QuotedTriple quotedTriple = quotedTriples.get(lastTripleObject);
                			QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                			newWalk.add(quotedTriple_expanded.subject);
                			// context-oriented
                			newWalk.add(quotedTriple_expanded.predicate);
                			newWalk.add(quotedTriple_expanded.object);
                    	} else if (mode.equals("qt2object")) {
                    		QuotedTriple quotedTriple = quotedTriples.get(lastTripleObject);
                    		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                			newWalk.add(quotedTriple_expanded.object);
                    	} else if (mode.equals("object2qt")) {
                    		// Check whether the given QT is wrapped with id, and if not, get the QT wrapped with id.
                    		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsObject, quotedTriples);
                    		// context-oriented
                        	newWalk.add(quotedTriple.qt);
                        	walks.remove(walk); // check whether this works
                    	} else if (mode.equals("subject2qt")) {
                    		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsSubject, quotedTriples);
                    		newWalk.add(quotedTriple.qt);
                    	}
                    	walks.add(newWalk);
                    } else {
                        if (nextIteration != null) {
                        	walks.remove(walk); // check whether this works
                            for (Triple nextStep : nextIteration) {
                                List<String> newWalk = new ArrayList<>(walk);
                                newWalk.add(nextStep.predicate);
                                newWalk.add(nextStep.object);
                                walks.add(newWalk);
                            }
                        }
                    }
                    
                } // loop over walks
            }

            // trim the list
            while (walks.size() > numberOfWalks) {
                int randomNumber = ThreadLocalRandom.current().nextInt(walks.size());
                walks.remove(randomNumber);
            }
        } // depth loop

        // now we need to translate our walks into strings
        List<String> result = new ArrayList<>();
        for (List<String> walk : walks) {
        	String walk_str = walk.stream().collect(Collectors.joining(" "));
        	result.add(walk_str);
        }
        return result;
    }

    /**
     * Generates duplicate-free walks for the given entity.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks The number of walks to be generated.
     * @param depth         The number of hops to nodes (!).
     * @return A list of walks where each element in the list represents a walk. The walk elements are separated by
     * spaces.
     */
    public List<String> generateDuplicateFreeRandomWalksForEntity(String entity, int numberOfWalks, int depth) {
        List<List<Triple>> walks = new ArrayList<>();
        boolean isFirstIteration = true;
        for (int currentDepth = 0; currentDepth < depth; currentDepth++) {
            // initialize with first node
            if (isFirstIteration) {
                List<Triple> neighbours = data.getObjectTriplesInvolvingSubject(entity);
                if (neighbours == null || neighbours.size() == 0) {
                    return new ArrayList<>();
                }
                for (Triple neighbour : neighbours) {
                    ArrayList<Triple> individualWalk = new ArrayList<>();
                    individualWalk.add(neighbour);
                    walks.add(individualWalk);
                }
                isFirstIteration = false;
            } else {
                // create a copy
                List<List<Triple>> walks_tmp = new ArrayList<>(walks);

                // loop over current walks
                for (List<Triple> walk : walks_tmp) {
                    // get last entity
                    Triple lastTriple = walk.get(walk.size() - 1);
                    List<Triple> nextIteration = data.getObjectTriplesInvolvingSubject(lastTriple.object);
                    if (nextIteration != null) {
                        walks.remove(walk); // check whether this works
                        for (Triple nextStep : nextIteration) {
                            List<Triple> newWalk = new ArrayList<>(walk);
                            newWalk.add(nextStep);
                            walks.add(newWalk);
                        }
                    }
                } // loop over walks
            }

            // trim the list
            while (walks.size() > numberOfWalks) {
                int randomNumber = ThreadLocalRandom.current().nextInt(walks.size());
                walks.remove(randomNumber);
            }
        } // depth loop

        // now we need to translate our walks into strings
        return Util.convertToStringWalks(walks, entity, isUnifyAnonymousNodes());
    }

    @Override
    public List<String> generateMidTypeWalksForEntityDuplicateFree(String entity, int numberOfWalks, int depth){
        List<List<String>> walksWithNodes = generateMidWalkForEntityAsArray(entity, numberOfWalks, depth);
        List<List<String>> result = new ArrayList<>();
        for (List<String> walkWithNodes : walksWithNodes) {

            // determine how often the entity appears
            int appearances = getNumberOfAppearances(entity, walkWithNodes);

            // draw the desired position to keep
            int choice = getRandomNumberBetweenZeroAndX(appearances);

            List<String> walk = new ArrayList<>();
            int currentNodeOfInterestPosition = 0;
            for (int i = 0; i < walkWithNodes.size(); i++) {
                if (i % 2 == 0) {
                    String node = walkWithNodes.get(i);
                    if (node.equals(entity)) {
                        // we found the node of interest
                        if (currentNodeOfInterestPosition == choice) {
                            walk.add(node);
                            currentNodeOfInterestPosition++;
                        } else {
                            // -> we will not add the node of interest this time but instead its supertype
                            String type = getRandomSupertypeOfEntity(node);
                            if(type != null){
                                walk.add(type);
                            }
                            currentNodeOfInterestPosition++;
                        }
                    } else {
                        // we have a normal node that is not a node of interest
                        String type = getRandomSupertypeOfEntity(node);
                        if(type != null){
                            walk.add(type);
                        }
                    }
                } else {
                    String edge = walkWithNodes.get(i);
                    walk.add(edge);
                }
            }
            result.add(walk);
        }
        return Util.convertToStringWalksDuplicateFree(result);
    }

    /**
     * Draw a random supertype. Note that the predicates of {@link MemoryStarWalkGenerator#typeProperties} are used.
     * @param entity The entity for which the type shall be obtained.
     * @return Type. Null if there is no type.
     */
    public String getRandomSupertypeOfEntity(String entity){
        if(entity == null){
            return null;
        }
        Set<String> candidates = new HashSet<>();
        for(String property : getTypeProperties()) {
            Set<Triple> triples = this.getData().getObjectTriplesWithSubjectPredicate(entity, property);
            if(triples != null && triples.size() > 0){
                for(Triple triple : triples){
                    candidates.add(triple.object);
                }
            }
        }
        if(candidates.size() == 0){
            return null;
        } else {
            return randomDrawFromSet(candidates);
        }
    }
    
    /**
     * Generates RDF-star random walks for the given entity.
     *
     * @param entity        The entity for which walks shall be generated.
     * @param numberOfWalks The number of walks to be generated.
     * @param depth         The number of hops to nodes (!).
     * @param probabilityFromQtToSubject	The transition probability from a quoted triple (QT) node to its constituent subjects.
     * @param probabilityFromObjectToQt		The transition probability from an object to a quoted triple (QT) node.
     * @param probabilityFromQtToObject		The transition probability from a quoted triple (QT) node to its constituent objects.
     * @param probabilityFromSubjectToQt	The transition probability from a subject to a quoted triple (QT) node.
     * @return A list of walks where each element in the list represents a walk. The walk elements are separated by
     * spaces.
     */
    @Override
    public List<String> generateStarRandomWalksForEntity(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt){
    	List<List<String>> walks = new ArrayList<>();
        boolean isFirstIteration = true;
        Map<String, QuotedTriple> quotedTriples = data.getQuotedTriples();
        for (int currentDepth = 0; currentDepth < depth; currentDepth++) {
            // initialize with first node
            if (isFirstIteration) {
                List<Triple> neighbours = data.getObjectTriplesInvolvingSubject(entity);
                if (neighbours == null || neighbours.size() == 0) {
                    return new ArrayList<>();
                }
                ArrayList<String> possibleWalkModes = new ArrayList<String>(); //List of possible walk mode to be executed because the set threshold has been exceeded. {qt2subject, qt2object, subject2qt, object2qt}
                double randFromObjectToQt = Math.random();
                double randFromSubjectToQt = Math.random();
                
                // Check if the entity is included as an object in the QT
                List<Triple> triplesInvolvingEntityAsObject = data.getObjectTriplesInvolvingObject(entity);
                QuotedTriple qtInvolvingEntityAsObject = getRandomQtInvolvingEntityAsObject(entity, triplesInvolvingEntityAsObject, quotedTriples);
                if (qtInvolvingEntityAsObject != null ) {
                	if (randFromObjectToQt < probabilityFromObjectToQt) { possibleWalkModes.add("object2qt"); }
                }
                
                // Check if the entity is included as an subject in the QT
                QuotedTriple qtInvolvingEntityAsSubject = getRandomQtInvolvingEntityAsSubject(entity, neighbours, quotedTriples);
                if (qtInvolvingEntityAsSubject != null ) {
                	if (randFromSubjectToQt < probabilityFromSubjectToQt) { possibleWalkModes.add("subject2qt"); }
                }

                // i.e., first entity is QT
                if (entity.contains("<<")) {
                	double randFromQtToSubject = Math.random();
                    double randFromQtToObject = Math.random();
                    if (randFromQtToSubject < probabilityFromQtToSubject) { possibleWalkModes.add("qt2subject"); }
                    if (randFromQtToObject < probabilityFromQtToObject) { possibleWalkModes.add("qt2object"); }
                }
                
                String mode = getRandomElement(possibleWalkModes);
                if (mode != null) {
                	ArrayList<String> individualWalk = new ArrayList<>();
                	if (mode.equals("qt2subject")) {
                		QuotedTriple quotedTriple = quotedTriples.get(entity);
            			QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
            			individualWalk.add(quotedTriple.qt);	 // = entity
            			individualWalk.add(quotedTriple_expanded.subject);
            			// context-oriented
            			individualWalk.add(quotedTriple_expanded.predicate);
            			individualWalk.add(quotedTriple_expanded.object);
                	} else if (mode.equals("qt2object")) {
                		QuotedTriple quotedTriple = quotedTriples.get(entity);
                		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);           		
            			individualWalk.add(quotedTriple.qt);	// = entity
            			individualWalk.add(quotedTriple_expanded.object);
                	} else if (mode.equals("object2qt")) {
                		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsObject, quotedTriples);
                		individualWalk.add(entity);
                		individualWalk.add(quotedTriple.qt);
                	} else if (mode.equals("subject2qt")) {
                		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsSubject, quotedTriples);
                		individualWalk.add(entity);
                		individualWalk.add(quotedTriple.qt);
                	}
                	walks.add(individualWalk);
                } else {
                	for (Triple neighbour : neighbours) {
                        ArrayList<String> individualWalk = new ArrayList<>();
                        individualWalk.add(neighbour.subject);
                        individualWalk.add(neighbour.predicate);
                        individualWalk.add(neighbour.object);
                        walks.add(individualWalk);
                    }
                }
                
                isFirstIteration = false;
            } else {
                // create a copy
                List<List<String>> walks_tmp = new ArrayList<>(walks);

                // loop over current walks
                for (List<String> walk : walks_tmp) {
                    // get last entity
                	String lastTripleSubject = "";
                	String lastTriplePredicate = "";
                    String lastTripleObject = walk.get(walk.size() - 1);
                	if(walk.size() >= 3) {
                		lastTripleSubject = walk.get(walk.size() - 3);
                    	lastTriplePredicate = walk.get(walk.size() - 2);
                	}
                	
                	// get neighbors of lastTripleObject
                	List<Triple> nextIteration = data.getObjectTriplesInvolvingSubject(lastTripleObject);
                    if (nextIteration == null) {
                    	continue;
                    }
                    
                    String qt_str = "<<" + lastTripleSubject + "-" + lastTriplePredicate + "-" + lastTripleObject + ">>";
                    
                    ArrayList<String> possibleWalkModes = new ArrayList<String>(); //List of possible walk mode to be executed because the set threshold has been exceeded. {qt2subject, qt2object, subject2qt, object2qt}
                    double randFromObjectToQt = Math.random();
                    double randFromSubjectToQt = Math.random();
                    
                    // Check if the entity is included as an object in the QT
                    QuotedTriple qtInvolvingEntityAsObject = quotedTriples.get(qt_str);
                    if (qtInvolvingEntityAsObject != null ) {
                    	if (randFromObjectToQt < probabilityFromObjectToQt) { possibleWalkModes.add("object2qt"); }
                    }
                    
                    // Check if the entity is included as an subject in the QT
                    QuotedTriple qtInvolvingEntityAsSubject = getRandomQtInvolvingEntityAsSubject(lastTripleObject, nextIteration, quotedTriples);
                    if (qtInvolvingEntityAsSubject != null ) {
                    	if (randFromSubjectToQt < probabilityFromSubjectToQt) { possibleWalkModes.add("subject2qt"); }
                    }
                    
                    // i.e., first entity is QT
                    if (lastTripleObject.contains("<<")) {
                    	double randFromQtToSubject = Math.random();
                        double randFromQtToObject = Math.random();
                        if (randFromQtToSubject < probabilityFromQtToSubject) { possibleWalkModes.add("qt2subject"); }
                        if (randFromQtToObject < probabilityFromQtToObject) { possibleWalkModes.add("qt2object"); }
                    }
                    
                    String mode = getRandomElement(possibleWalkModes);
                    if (mode != null) {
                    	List<String> newWalk = new ArrayList<>(walk);
                    	if (mode.equals("qt2subject")) {
                    		QuotedTriple quotedTriple = quotedTriples.get(lastTripleObject);
                			QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                			newWalk.add(quotedTriple_expanded.subject);
                			// context-oriented
                			newWalk.add(quotedTriple_expanded.predicate);
                			newWalk.add(quotedTriple_expanded.object);
                    	} else if (mode.equals("qt2object")) {
                    		QuotedTriple quotedTriple = quotedTriples.get(lastTripleObject);
                    		QuotedTriple quotedTriple_expanded = expandQuotedTriple(quotedTriple, quotedTriples);
                			newWalk.add(quotedTriple_expanded.object);
                    	} else if (mode.equals("object2qt")) {
                    		// Check whether the given QT is wrapped with id, and if not, get the QT wrapped with id.
                    		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsObject, quotedTriples);
                    		// context-oriented
                        	newWalk.add(quotedTriple.qt);
                    	} else if (mode.equals("subject2qt")) {
                    		QuotedTriple quotedTriple = envelopeQuotedTriple(qtInvolvingEntityAsSubject, quotedTriples);
                    		newWalk.add(quotedTriple.qt);
                    	}
                    	walks.add(newWalk);
                    } else {
                        if (nextIteration != null) {
                            for (Triple nextStep : nextIteration) {
                                List<String> newWalk = new ArrayList<>(walk);
                                newWalk.add(nextStep.predicate);
                                newWalk.add(nextStep.object);
                                walks.add(newWalk);
                            }
                        }
                    }
                    
                } // loop over walks
            }

            // trim the list
            while (walks.size() > numberOfWalks) {
                int randomNumber = ThreadLocalRandom.current().nextInt(walks.size());
                walks.remove(randomNumber);
            }
        } // depth loop

        // now we need to translate our walks into strings
        List<String> result = new ArrayList<>();
        for (List<String> walk : walks) {
        	String walk_str = walk.stream().collect(Collectors.joining(" "));
        	result.add(walk_str);
        }
        return result;
    }

    @Override
    public List<String> generateRandomWalksForEntity(String entity, int numberOfWalks, int depth){
        List<String> result = new ArrayList<>();
        int currentDepth;
        String currentWalk;
        int currentWalkNumber = 0;

        nextWalk:
        while (currentWalkNumber < numberOfWalks) {
            currentWalkNumber++;
            String lastObject = entity;
            currentWalk = entity;
            currentDepth = 0;
            while (currentDepth < depth) {
                currentDepth++;
                Triple po = getRandomTripleForSubjectWithoutTags(lastObject);
                if(po != null){
                    currentWalk += " " + uriShortenerFunction.apply(po.predicate) + " " + uriShortenerFunction.apply(po.object);
                    lastObject = po.object;
                } else {
                    // The current walk cannot be continued -> add to list (if there is a walk of depth 1) and create next walk.
                    if(currentWalk.length() != entity.length()) result.add(currentWalk);
                    continue nextWalk;
                }
            }
            result.add(currentWalk);
        }
        return result;
    }
    
    /**
     * entityがQTにsubjectとして含まれているかどうかのチェックして、該当するQTをランダムで返す
     */
    public QuotedTriple getRandomQtInvolvingEntityAsSubject(String entity, List<Triple> triplesInvolvingEntityAsSubject, Map<String, QuotedTriple> quotedTriples) {
    	
        List<Triple> neighboursCopy = new ArrayList<>(triplesInvolvingEntityAsSubject);
        Collections.shuffle(neighboursCopy);
        for (Triple tripleInvolvingEntityAsSubject : neighboursCopy) {
        	// このトリプルのQTがあるかどうかチェック
        	String qt_str = "<<" + entity + "-" + tripleInvolvingEntityAsSubject.predicate + "-" + tripleInvolvingEntityAsSubject.object + ">>";
        	if (quotedTriples.containsKey(qt_str)) {
        		return quotedTriples.get(qt_str);
        	}
        }
        return null;
    }
    
    /**
     * entityがQTにobjectとして含まれているかどうかのチェックして、該当するQTをランダムで返す
     */
    public QuotedTriple getRandomQtInvolvingEntityAsObject(String entity, List<Triple> triplesInvolvingEntityAsObject, Map<String, QuotedTriple> quotedTriples) {
    	/*
         * ここから、entityがQTにobjectとして含まれているかどうかのチェック
         */
        // 現在のentityを目的語とするトリプルを取得
        if (triplesInvolvingEntityAsObject != null) {
            Collections.shuffle(triplesInvolvingEntityAsObject);
            for (Triple tripleInvolvingEntityAsObject : triplesInvolvingEntityAsObject) {
            	// このトリプルのQTがあるかどうかチェック
            	String qt_str = "<<" + tripleInvolvingEntityAsObject.subject + "-" + tripleInvolvingEntityAsObject.predicate + "-" + entity + ">>";
            	if (quotedTriples.containsKey(qt_str)) {
            		return quotedTriples.get(qt_str);
            	}
            }
        }
        return null;
    }

    /**
     * Faster version of {@link NtMemoryWalkGenerator#getRandomTripleForSubject(String)}.
     * Note that there cannot be any leading less-than or trailing greater-than signs around the subject.
     * The subject URI should already be shortened.
     *
     * @param subject The subject for which a random predicate and object shall be found.
     * @return Predicate and object, randomly obtained for the given subject.
     */
    public Triple getRandomTripleForSubjectWithoutTags(String subject) {
        if (subject == null) return null;
        List<Triple> queryResult = data.getObjectTriplesInvolvingSubject(subject);
        if (queryResult == null) {
            // no triple found
            return null;
        }
        int randomNumber = ThreadLocalRandom.current().nextInt(queryResult.size());
        //System.out.println("(" + Thread.currentThread().getName() + ") " + randomNumber + "[" + queryResult.size() + "]");
        return queryResult.get(randomNumber);
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

    // getters and setters below

    public boolean isUnifyAnonymousNodes() {
        return isUnifyAnonymousNodes;
    }

    public void setUnifyAnonymousNodes(boolean unifyAnonymousNodes) {
        isUnifyAnonymousNodes = unifyAnonymousNodes;
    }

    public QuotedTripleDataSetMemory getData() {
        return data;
    }

    public boolean isParseDatatypeProperties() {
        return isParseDatatypeProperties;
    }

    public void setParseDatatypeProperties(boolean parseDatatypeProperties) {
        isParseDatatypeProperties = parseDatatypeProperties;
    }

    public long getDataSize() {
        if (data == null) {
            return 0L;
        } else return data.getObjectTripleSize();
    }

    public UnaryOperator<String> getTextProcessingFunction() {
        return textProcessingFunction;
    }

    public void setTextProcessingFunction(UnaryOperator<String> textProcessingFunction) {
        this.textProcessingFunction = textProcessingFunction;
    }

    @Override
    public Set<String> getTypeProperties() {
        return typeProperties;
    }
    
    public static<T> T getRandomElement(List<T> list) {
    	if (list.size() == 0) {
    		return null;
    	}
        Random random = new Random();
        int randomIndex = random.nextInt(list.size());
        return list.get(randomIndex);
    }
}
