package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import java.util.List;

public interface IMidEdgeWalkDuplicateFreeCapability extends IWalkGenerationCapability {


    /**
     * Generates walks that are ready to be processed further (already concatenated, space-separated).
     * @param entity The entity for which a walk shall be generated.
     * @param numberOfWalks The number of walks to be generated.
     * @param depth The depth of each walk.
     * @return List where every item is a walk separated by spaces.
     */
    List<String> generateMidEdgeWalksForEntityDuplicateFree(String entity, int numberOfWalks, int depth);
}
