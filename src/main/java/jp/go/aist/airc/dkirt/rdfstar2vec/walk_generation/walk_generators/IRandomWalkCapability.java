package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import java.util.List;

public interface IRandomWalkCapability extends IWalkGenerationCapability{


    List<String> generateRandomWalksForEntity(String entity, int numberOfWalks, int depth);
}
