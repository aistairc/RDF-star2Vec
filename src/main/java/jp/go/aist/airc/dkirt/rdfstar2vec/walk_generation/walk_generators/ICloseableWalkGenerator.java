package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

public interface ICloseableWalkGenerator extends IWalkGenerator {


    /**
     * Close open resources.
     */
    void close();
}
