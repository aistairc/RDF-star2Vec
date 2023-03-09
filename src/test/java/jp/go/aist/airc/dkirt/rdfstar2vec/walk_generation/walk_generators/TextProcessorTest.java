package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators;

import org.junit.jupiter.api.Test;

import jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.walk_generators.TextProcessor;

import static org.junit.jupiter.api.Assertions.*;

class TextProcessorTest {


    @Test
    void apply(){
       TextProcessor tp = new TextProcessor();
       assertEquals("hello world", tp.apply("\"Hello World!\"@de ."));
       assertEquals("hello world", tp.apply("\" Hello World!\". "));
       assertEquals("151", tp.apply("\"1.51\"^^<something> . "));
       assertEquals("gedichte", tp.apply("\"Gedichte\"@de ."));
    }
}