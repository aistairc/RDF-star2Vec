package jp.go.aist.airc.dkirt.rdfstar2vec.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.go.aist.airc.dkirt.rdfstar2vec.training.Gensim;

import static jp.go.aist.airc.dkirt.rdfstar2vec.util.Util.isW2Vformat;

import java.io.File;

public class KvConverter {


    private static final Logger LOGGER = LoggerFactory.getLogger(KvConverter.class);

    public static void convert(File txtOrw2vFile, File fileToWrite){
        File w2vFile = null;
        if(txtOrw2vFile.getName().endsWith(".w2v")){
            LOGGER.info("Recognized w2v format. Converting to kv...");
            w2vFile = txtOrw2vFile;
        } else if(txtOrw2vFile.getName().endsWith(".txt")) {
            if(isW2Vformat(txtOrw2vFile)){
                LOGGER.info("Provided file is likely in w2v format despite '.txt' ending. Trying to convert directly " +
                        "to '.kv'.");
                w2vFile = txtOrw2vFile;
            } else {
                LOGGER.info("Recognized txt format. Will convert to w2v and then to kv.");
                w2vFile = new File(fileToWrite.getParentFile(), txtOrw2vFile.getName().substring(0,
                        (int) (txtOrw2vFile.getName().length()) - 4) + ".w2v");
                VectorTxtToW2v.convert(txtOrw2vFile, w2vFile);
            }
        } else {
            LOGGER.error("Neither .txt nor .w2v file provided (make sure you use correct file endings). ABORTING " +
                    "program.");
            return;
        }
        Gensim.getInstance().convertW2vToKv(w2vFile.getAbsolutePath(), fileToWrite.getAbsolutePath());
    }

}
