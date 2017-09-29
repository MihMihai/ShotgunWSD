package shotgunwsd.relatedness.embeddings;

import edu.smu.tspell.wordnet.Synset;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import shotgunwsd.relatedness.SynsetRelatedness;
import shotgunwsd.relatedness.embeddings.sense.computations.SenseComputation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;


/**
 * Created by Butnaru Andrei-Madalin.
 */
public class WordEmbeddingRelatedness extends SynsetRelatedness {
    public static WordVectors wordVectors = null;
    private HashMap<String, Double[]> wordClusters = null;
    static WordEmbeddingRelatedness instance = null;
    static SenseComputation senseComputation;

    protected WordEmbeddingRelatedness(){}
    public static WordEmbeddingRelatedness getInstance(String wePath, String weType, SenseComputation senseComputation) {
        if(instance == null) {
            instance = new WordEmbeddingRelatedness();
        }

        if(wordVectors == null) {
            WordEmbeddingRelatedness.loadWordEmbeddings(wePath, weType);
        }

        WordEmbeddingRelatedness.senseComputation = senseComputation;

        return instance;
    }

    public static WordEmbeddingRelatedness getInstance(SenseComputation senseComputation) {
        if(instance == null) {
            instance = new WordEmbeddingRelatedness();
        }

        WordEmbeddingRelatedness.senseComputation = senseComputation;

        return instance;
    }

    public void setWordClusters(HashMap<String, Double[]> wordClusters) {
        this.wordClusters = wordClusters;
    }

    public double computeSimilarity(Object[] synsetRepresentation, String[] windowWords, int[] synset2WordIndex, int k, int j){
        INDArray[] windowWordsSenseEmbeddings = (INDArray[])synsetRepresentation;

        double score = Transforms.cosineSim(windowWordsSenseEmbeddings[k], windowWordsSenseEmbeddings[j]);

        if(Double.isNaN(score))
            score = 0;

        return score;
    }

    public double computeSimilarity(Synset synset1, String word1, Synset synset2, String word2){
        INDArray[] senseEmbeddings = new INDArray[2];

        if(wordClusters == null) {
            senseEmbeddings[0] = Nd4j.create(SenseEmbedding.getSenseEmbedding(wordVectors, synset1, word1, senseComputation));
            senseEmbeddings[1] = Nd4j.create(SenseEmbedding.getSenseEmbedding(wordVectors, synset2, word2, senseComputation));
        } else {
            senseEmbeddings[0] = Nd4j.create(SenseEmbedding.getSenseEmbedding(wordClusters, synset1, word1, senseComputation));
            senseEmbeddings[1] = Nd4j.create(SenseEmbedding.getSenseEmbedding(wordClusters, synset2, word2, senseComputation));
        }
        return computeSimilarity(senseEmbeddings, null, null, 0, 1);
    }

    /**
     * Generates the sense embedding for each synset of every word from the context window
     */
    public Object[] computeSynsetRepresentations(Synset[] windowWordsSynsets, String[] windowWords, int[] synset2WordIndex) {
        Object[] windowWordsSenseEmbeddings = new INDArray[windowWordsSynsets.length];
        for (int k = 0; k < windowWordsSynsets.length; k++) {
            if(wordClusters == null) {
                windowWordsSenseEmbeddings[k] = Nd4j.create(SenseEmbedding.getSenseEmbedding(wordVectors, windowWordsSynsets[k], windowWords[synset2WordIndex[k]], senseComputation));
            } else {
                windowWordsSenseEmbeddings[k] = Nd4j.create(SenseEmbedding.getSenseEmbedding(wordClusters, windowWordsSynsets[k], windowWords[synset2WordIndex[k]], senseComputation));
            }
        }

        return windowWordsSenseEmbeddings;
    }

    public static void loadWordEmbeddings(String wePath, String weType) {
        try {
            switch (weType) {
                case "Google":
                    WordEmbeddingRelatedness.wordVectors = WordVectorSerializer.loadGoogleModel(new File(wePath), true);
                    break;
                case "Glove":
                    WordEmbeddingRelatedness.wordVectors = WordVectorSerializer.loadTxtVectors(new File(wePath));
                    break;
                default:
                    System.out.println("Word Embeddings type is invalid! " + weType + " is not a valid type. Please use Google or Glove model.");
                    System.exit(0);
            }
        } catch (IOException e) {
            System.out.println("Could not find Word Embeddings file in " + wePath);
        }
    }
}
