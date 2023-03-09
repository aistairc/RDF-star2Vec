package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import java.util.List;
import java.util.Set;

public interface IStarMidTypeWalkDuplicateFreeCapability extends IWalkGenerationCapability {


    /**
     * Generates walks that are ready to be processed further (already concatenated, space-separated).
     * @param entity The entity for which a walk shall be generated.
     * @param numberOfWalks The number of walks to be generated.
     * @param depth The depth of each walk.
     * @param probabilityFromQtToSubject The transition probability from a quoted triple (QT) node to its constituent subjects.
     * @param probabilityFromObjectToQt The transition probability from an object to a quoted triple (QT) node.
     * @return List where every item is a walk separated by spaces.
     */
    List<String> generateMidTypeWalksForEntityDuplicateFree(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt);

    /**
     * Get the properties used to obtain a type.
     * @return The set of type properties.
     */
    Set<String> getTypeProperties();
}
