package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.vocabulary.OWL;
import org.semanticweb.yars.nx.namespace.RDF;
import org.semanticweb.yars.nx.namespace.RDFS;

public class Prefix {

	public Map<String, String> prefixMap = new HashMap<String,String>();
	public Prefix() {
		final String kgc = "http://kgc.knowledge-graph.jp/ontology/kgc.owl#";
		final String sb = "http://kgc.knowledge-graph.jp/data/SpeckledBand/";
		final String dm = "http://kgc.knowledge-graph.jp/data/DancingMen/";
		final String ci = "http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/";
		final String df = "http://kgc.knowledge-graph.jp/data/DevilsFoot/";
		final String cm = "http://kgc.knowledge-graph.jp/data/CrookedMan/";
		final String ag = "http://kgc.knowledge-graph.jp/data/AbbeyGrange/";
		final String sl = "http://kgc.knowledge-graph.jp/data/SilverBlaze/";
		final String rp = "http://kgc.knowledge-graph.jp/data/ResidentPatient/";
		final String kdp = "http://kgc.knowledge-graph.jp/data/predicate/";
		final String wd = "http://www.wikidata.org/entity/";
		final String ex = "http://example.com/";
//		final String bkr = "http://mor.nlm.nih.gov/bkr/";
//		final String provnir = "http://knoesis.wright.edu/provenir/";
//		final String umls = "http://mor.nlm.nih.gov/umls/";
		
		prefixMap.put(kgc, "kgc:");
		prefixMap.put(sb, "sb:");
		prefixMap.put(dm, "dm:");
		prefixMap.put(ci, "ci:");
		prefixMap.put(df, "df:");
		prefixMap.put(cm, "cm:");
		prefixMap.put(ag, "ag:");
		prefixMap.put(sl, "sl:");
		prefixMap.put(rp, "rp:");
		prefixMap.put(kdp, "kdp:");
		prefixMap.put(RDF.NS, "rdf:");
		prefixMap.put(RDFS.NS, "rdfs:");
		prefixMap.put(OWL.NS, "owl:");
		prefixMap.put(wd, "wd:");
		prefixMap.put(ex, "ex:");
//		prefixMap.put(bkr, "bkr:");
//		prefixMap.put(provnir, "provnir:");
//		prefixMap.put(umls, "umls:");
		
	}

	public String replaceURI(String entity) {
		String qname = entity;
		for (String key : prefixMap.keySet()) {
			if (entity.contains(key)) {
				qname = entity.replace(key, prefixMap.get(key));
				break;
			}
		}
		return qname;
	}
}
