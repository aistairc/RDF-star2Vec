package de.uni_mannheim.informatik.dws.jrdf2vec.util;

import org.junit.jupiter.api.Test;

import jp.go.aist.airc.dkirt.rdfstar2vec.util.Rdf2Pajek;
import jp.go.aist.airc.dkirt.rdfstar2vec.util.Util;

import java.io.File;

import static jp.go.aist.airc.dkirt.rdfstar2vec.util.Util.getPathOfResource;
import static org.junit.jupiter.api.Assertions.*;

class Rdf2PajekTest {


    @Test
    void convert() {
        File pajekFile = new File("./pizza.net");
        String pizzaOntology = getPathOfResource("pizza.ttl");
        Rdf2Pajek.convert(new File(pizzaOntology), pajekFile);
        assertTrue(pajekFile.exists());
        Util.deleteFile(pajekFile);
    }

}