package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import java.util.List;

public interface IStarRandomWalkCapability extends IWalkGenerationCapability{


    List<String> generateStarRandomWalksForEntity(String entity, int numberOfWalks, int depth, double probabilityFromQtToSubject, double probabilityFromObjectToQt, double probabilityFromQtToObject, double probabilityFromSubjectToQt);
}
