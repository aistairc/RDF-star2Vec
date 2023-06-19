package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.runnables;

import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.base.WalkGenerationManager;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.base.WalkGenerationMode;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEntityWalkRunnable implements Runnable {


    /**
     * Default Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEntityWalkRunnable.class);

    /**
     * Entity that is processed by this thread.
     */
    String entity;

    /**
     * Length of each walk.
     */
    int depth;

    /**
     * Number of walks to be performed per entity.
     */
    int numberOfWalks;

    WalkGenerationMode walkGenerationMode;

    /**
     * The walk generator for which this parser works.
     */
    WalkGenerationManager walkGenerationManager;
    
    double probabilityFromQtToSubject;
    double probabilityFromObjectToQt;
    double probabilityFromQtToObject;
    double probabilityFromSubjectToQt;

    /**
     * Constructor.
     *
     * @param generator     Generator to be used.
     * @param entity        The entity this particular thread shall handle.
     * @param numberOfWalks The number of walks to be performed per entity.
     * @param depth         Desired length of the walk. Defines how many entity steps are allowed. Note that
     *                      this leads to more walk components than the specified depth.
     * @param mode          The walk generation mode.
     */
    public DefaultEntityWalkRunnable(WalkGenerationManager generator, String entity, int numberOfWalks, int depth,
                                     WalkGenerationMode mode) {
        this.entity = entity;
        this.numberOfWalks = numberOfWalks;
        this.depth = depth;
        this.walkGenerationManager = generator;
        this.walkGenerationMode = mode;
    }
    
    /**
     * Constructor for RDF-star
     *
     * @param generator     				Generator to be used.
     * @param entity        				The entity this particular thread shall handle.
     * @param numberOfWalks 				The number of walks to be performed per entity.
     * @param depth         				Desired length of the walk. Defines how many entity steps are allowed. Note that
     *                      				this leads to more walk components than the specified depth.
     * @param mode          				The walk generation mode.
     * @param probabilityFromQtToSubject	The transition probability from quoted triple node to subject node.
     * @param probabilityFromObjectToQt		The transition probability from an object to a quoted triple (QT) node.
     */
    public DefaultEntityWalkRunnable(WalkGenerationManager generator, String entity, int numberOfWalks, int depth,
                                     WalkGenerationMode mode, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt) {
    	this.entity = entity;
        this.numberOfWalks = numberOfWalks;
        this.depth = depth;
        this.walkGenerationManager = generator;
        this.walkGenerationMode = mode;
        this.probabilityFromQtToSubject = probabilityFromQtToSubject;
        this.probabilityFromObjectToQt = probabilityFromObjectToQt;
        this.probabilityFromQtToObject = probabilityFromQtToObject;
        this.probabilityFromSubjectToQt = probabilityFromSubjectToQt;
    }

    /**
     * Actual thread execution.
     */
    public void run() {
        switch (walkGenerationMode) {
        	case STAR_MID_WALKS:
        		if (walkGenerationManager.getWalkGenerator() instanceof IStarMidWalkCapability) {
                    walkGenerationManager.writeToFile(((IStarMidWalkCapability) walkGenerationManager.getWalkGenerator()).generateStarMidWalksForEntity(walkGenerationManager.shortenUri(entity), this.numberOfWalks, this.depth, this.probabilityFromQtToSubject, this.probabilityFromObjectToQt, this.probabilityFromQtToObject, this.probabilityFromSubjectToQt));
                } else {
                    printNotImplementedWarning();
                }
                break;
        	case STAR_MID_WALKS_DUPLICATE_FREE:
                if(walkGenerationManager.getWalkGenerator() instanceof IStarMidWalkDuplicateFreeCapability){
                    walkGenerationManager.writeToFile(((IStarMidWalkDuplicateFreeCapability) walkGenerationManager.getWalkGenerator()).generateStarMidWalksForEntityDuplicateFree(walkGenerationManager.shortenUri(entity), this.numberOfWalks, this.depth, this.probabilityFromQtToSubject, this.probabilityFromObjectToQt, this.probabilityFromQtToObject, this.probabilityFromSubjectToQt));
                } else LOGGER.error("NOT YET IMPLEMENTED FOR THE CURRENT WALK GENERATOR " + walkGenerationManager.getWalkGenerator().getClass().toString() + "!");
                break;
        	case STAR_RANDOM_WALKS:
        		if(walkGenerationManager.getWalkGenerator() instanceof IStarRandomWalkCapability){
                    walkGenerationManager.writeToFile(
                            ((IStarRandomWalkCapability) walkGenerationManager.getWalkGenerator())
                                    .generateStarRandomWalksForEntity(walkGenerationManager.shortenUri(entity),
                                            this.numberOfWalks, this.depth, this.probabilityFromQtToSubject, this.probabilityFromObjectToQt, this.probabilityFromQtToObject, this.probabilityFromSubjectToQt));
                } else {
                    LOGGER.error("NOT YET IMPLEMENTED FOR THE CURRENT WALK GENERATOR " + walkGenerationManager.getWalkGenerator().getClass().toString() + "!");
                }
                break;
        	case STAR_RANDOM_WALKS_DUPLICATE_FREE:
                if (walkGenerationManager.getWalkGenerator() instanceof IStarRandomWalkDuplicateFreeCapability) {
                    walkGenerationManager
                            .writeToFile(
                                    ((IStarRandomWalkDuplicateFreeCapability) walkGenerationManager.getWalkGenerator())
                                            .generateDuplicateFreeStarRandomWalksForEntity(walkGenerationManager.shortenUri(entity), numberOfWalks, this.depth, this.probabilityFromQtToSubject, this.probabilityFromObjectToQt, this.probabilityFromQtToObject, this.probabilityFromSubjectToQt));
                } else {
                    LOGGER.error("NOT YET IMPLEMENTED FOR THIS WALK GENERATOR (" + walkGenerationManager.getWalkGenerator().getClass() + ")!" +
                            " Make sure" +
                            " it implements IRandomWalkDuplicateFreeCapability.");
                }
                break;
        	case RANDOM_WALKS_DUPLICATE_FREE:
                if (walkGenerationManager.getWalkGenerator() instanceof IRandomWalkDuplicateFreeCapability) {
                    walkGenerationManager
                            .writeToFile(
                                    ((IRandomWalkDuplicateFreeCapability) walkGenerationManager.getWalkGenerator())
                                            .generateDuplicateFreeRandomWalksForEntity(walkGenerationManager.shortenUri(entity), numberOfWalks, this.depth));
                } else {
                    LOGGER.error("NOT YET IMPLEMENTED FOR THIS WALK GENERATOR (" + walkGenerationManager.getWalkGenerator().getClass() + ")!" +
                            " Make sure" +
                            " it implements IRandomWalkDuplicateFreeCapability.");
                }
                break;
            case MID_WALKS_DUPLICATE_FREE:
                if(walkGenerationManager.getWalkGenerator() instanceof IMidWalkDuplicateFreeCapability){
                    walkGenerationManager.writeToFile(((IMidWalkDuplicateFreeCapability) walkGenerationManager.getWalkGenerator()).generateMidWalksForEntityDuplicateFree(walkGenerationManager.shortenUri(entity), this.numberOfWalks, this.depth));
                } else LOGGER.error("NOT YET IMPLEMENTED FOR THE CURRENT WALK GENERATOR " + walkGenerationManager.getWalkGenerator().getClass().toString() + "!");
                break;
            case RANDOM_WALKS:
                if(walkGenerationManager.getWalkGenerator() instanceof IRandomWalkCapability){
                    walkGenerationManager.writeToFile(
                            ((IRandomWalkCapability) walkGenerationManager.getWalkGenerator())
                                    .generateRandomWalksForEntity(walkGenerationManager.shortenUri(entity),
                                            numberOfWalks, depth));
                } else {
                    LOGGER.error("NOT YET IMPLEMENTED FOR THE CURRENT WALK GENERATOR " + walkGenerationManager.getWalkGenerator().getClass().toString() + "!");
                }
                break;
            case MID_WALKS:
                if (walkGenerationManager.getWalkGenerator() instanceof IMidWalkCapability) {
                    walkGenerationManager.writeToFile(((IMidWalkCapability) walkGenerationManager.getWalkGenerator()).generateMidWalksForEntity(walkGenerationManager.shortenUri(entity), this.numberOfWalks, this.depth));
                } else {
                    printNotImplementedWarning();
                }
                break;
            case MID_WALKS_WEIGHTED:
                if (walkGenerationManager.getWalkGenerator() instanceof IMidWalkWeightedCapability) {
                    walkGenerationManager.writeToFile(((IMidWalkWeightedCapability) walkGenerationManager.getWalkGenerator()).generateWeightedMidWalksForEntity(walkGenerationManager.shortenUri(entity), this.numberOfWalks, this.depth));
                } else {
                    printNotImplementedWarning();
                }
                break;
            case EXPERIMENTAL_MID_TYPE_WALKS_DUPLICATE_FREE:
                if (walkGenerationManager.getWalkGenerator() instanceof IMidTypeWalkDuplicateFreeCapability) {
                    walkGenerationManager.writeToFile(
                            ((IMidTypeWalkDuplicateFreeCapability) walkGenerationManager.getWalkGenerator())
                                    .generateMidTypeWalksForEntityDuplicateFree(
                                            walkGenerationManager.shortenUri(entity),
                                            this.numberOfWalks, this.depth)
                    );
                } else {
                    printNotImplementedWarning();
                }
                break;
            case EXPERIMENTAL_MID_EDGE_WALKS_DUPLICATE_FREE:
                if (walkGenerationManager.getWalkGenerator() instanceof IMidEdgeWalkDuplicateFreeCapability) {
                    walkGenerationManager
                            .writeToFile(
                                    ((IMidEdgeWalkDuplicateFreeCapability) walkGenerationManager.getWalkGenerator())
                                            .generateMidEdgeWalksForEntityDuplicateFree(
                                                    walkGenerationManager.shortenUri(entity),
                                                    this.numberOfWalks, this.depth)
                            );
                } else {
                    printNotImplementedWarning();
                }
                break;
            case EXPERIMENTAL_NODE_WALKS_DUPLICATE_FREE:
                if(walkGenerationManager.getWalkGenerator() instanceof INodeWalksDuplicateFreeCapability) {
                    walkGenerationManager
                            .writeToFile(
                                    ((INodeWalksDuplicateFreeCapability) walkGenerationManager.getWalkGenerator())
                                    .generateNodeWalksForEntity(walkGenerationManager.shortenUri(entity),
                                            this.numberOfWalks, this.depth)
                            );
                } else {
                    printNotImplementedWarning();
                }
        }
    }

    private static void printNotImplementedWarning() {
        LOGGER.error("NOT YET IMPLEMENTED FOR THE CURRENT WALK GENERATOR!");
    }
}
