package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import java.util.List;

public interface IRandomWalkDuplicateFreeCapability extends IWalkGenerationCapability{


    List<String> generateDuplicateFreeRandomWalksForEntity(String entity, int numberOfWalks, int depth);
}
