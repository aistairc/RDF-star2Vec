package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.entity_selector;

import java.util.HashSet;
import java.util.Set;

import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.QuotedTripleDataSetMemory;
import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures.TripleDataSetMemory;

/**
 * This entity selector selects all unique nodes.
 */
public class MemoryStarEntitySelector implements EntitySelector {


    /**
     * Constructor.
     * @param data Triple data set to be used.
     */
    public MemoryStarEntitySelector(QuotedTripleDataSetMemory data){
        this.data = data;
    }

    private QuotedTripleDataSetMemory data;

    @Override
    public Set<String> getEntities() {
        Set<String> result = new HashSet<>();
        result.addAll(data.getUniqueSubjects());
        result.addAll(data.getUniqueObjectTripleObjects());
        return result;
    }
}
