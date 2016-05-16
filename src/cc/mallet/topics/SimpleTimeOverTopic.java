package cc.mallet.topics;

import cc.mallet.types.*;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.Randoms;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Created by changun on 2016/3/16.
 */
public class SimpleTimeOverTopic {
    private static Logger logger = MalletLogger.getLogger(SimpleLDA.class.getName());

    // the training instances and their topic assignments
    protected ArrayList<TopicAssignment> data;

    // the alphabet for the input data
    protected Alphabet alphabet;

    // the alphabet for the topics
    protected LabelAlphabet topicAlphabet;

    // The number of topics requested
    protected int numTopics;

    // The size of the vocabulary
    protected int numTypes;

    // Prior parameters
    protected double alpha;	 // Dirichlet(alpha,alpha,...) is the distribution over topics
    protected double alphaSum;
    protected double beta;   // Prior on per-topic multinomial distribution over words
    protected double betaSum;
    public static final double DEFAULT_BETA = 0.01;

    // Hyper parameter
    protected double expotentialWeightForTime = 0.5;

    // An array to put the topic counts for the current document.
    // Initialized locally below.  Defined here to avoid
    // garbage collection overhead.
    protected int[] oneDocTopicCounts; // indexed by <document index, topic index>

    // Statistics needed for sampling.
    protected int[][] typeTopicCounts; // indexed by <feature index, topic index>
    protected int[] tokensPerTopic; // indexed by <topic index>
    // Statistics needed for computing prob of topic given type
    protected  int[] typeCounts; // indexed by <feature index>
    // Time-related variables
    long minTime, maxTime;

    protected BetaDistribution[] betaDistributionPerTopic; // indexed by <topic index>
    protected double[] timeMeanTopic; // indexed by <topic index>
    protected double[] timeM2PerTopic; // indexed by <topic index>
    protected int[] nPerTopic;


    public int showTopicsInterval = 10;
    public int wordsPerTopic = 10;

    protected Randoms random;
    protected NumberFormat formatter;
    protected boolean printLogLikelihood = false;

    public SimpleTimeOverTopic(int numberOfTopics, double alphaSum, double beta, double expForTime) {
        this (numberOfTopics, alphaSum, beta, expForTime, new Randoms());
    }

    private static LabelAlphabet newLabelAlphabet (int numTopics) {
        LabelAlphabet ret = new LabelAlphabet();
        for (int i = 0; i < numTopics; i++)
            ret.lookupIndex("topic"+i);
        return ret;
    }

    public SimpleTimeOverTopic(int numberOfTopics, double alphaSum, double beta , double expForTime, Randoms random) {
        this (newLabelAlphabet (numberOfTopics), alphaSum, beta, expForTime, random);
    }

    public SimpleTimeOverTopic(LabelAlphabet topicAlphabet, double alphaSum, double beta, double expForTime, Randoms random)
    {
        this.data = new ArrayList<TopicAssignment>();
        this.topicAlphabet = topicAlphabet;
        this.numTopics = topicAlphabet.size();
        this.expotentialWeightForTime = expForTime;
        this.alphaSum = alphaSum;
        this.alpha = alphaSum / numTopics;
        this.beta = beta;
        this.random = random;

        oneDocTopicCounts = new int[numTopics];
        tokensPerTopic = new int[numTopics];
        betaDistributionPerTopic = new BetaDistribution[numTopics];
        timeMeanTopic = new double[numTopics];
        timeM2PerTopic = new double[numTopics];
        nPerTopic = new int[numTopics];

        formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(5);

        logger.info("Simple Time Over Topics: " + numTopics + " topics");
    }

    public Alphabet getAlphabet() { return alphabet; }
    public LabelAlphabet getTopicAlphabet() { return topicAlphabet; }
    public int getNumTopics() { return numTopics; }
    public ArrayList<TopicAssignment> getData() { return data; }

    public void setTopicDisplay(int interval, int n) {
        this.showTopicsInterval = interval;
        this.wordsPerTopic = n;
    }

    public void setRandomSeed(int seed) {
        random = new Randoms(seed);
    }

    public int[][] getTypeTopicCounts() { return typeTopicCounts; }
    public int[] getTopicTotals() { return tokensPerTopic; }
    public double getInstanceTime(Instance instance){
        long timestamp = (long) instance.getProperty("timestamp");
        return (timestamp - minTime) / (double) (maxTime - minTime);

    }
    public void addInstances (InstanceList training) {

        alphabet = training.getDataAlphabet();
        numTypes = alphabet.size();
	
        betaSum = beta * numTypes;

        typeTopicCounts = new int[numTypes][numTopics];
        typeCounts = new int[numTypes];
        minTime = Long.MAX_VALUE;
        maxTime = Long.MIN_VALUE;

        int doc = 0;
        for (Instance instance : training) {
            if (instance.getProperty("timestamp") == null || !(instance.getProperty("timestamp") instanceof Long)) {
                throw new IllegalStateException("Instances need to have property \"timestamp\"");
            }
            long time = (long) instance.getProperty("timestamp");
            if (time < minTime) {
                minTime = time;
            }
            if (time > maxTime) {
                maxTime = time;
            }
        }
        for (Instance instance : training) {

            FeatureSequence tokens = (FeatureSequence) instance.getData();
            LabelSequence topicSequence =
                    new LabelSequence(topicAlphabet, new int[ tokens.size() ]);
            double time = getInstanceTime(instance);
            int[] topics = topicSequence.getFeatures();
            for (int position = 0; position < tokens.size(); position++) {

                int topic = random.nextInt(numTopics);
                topics[position] = topic;
                tokensPerTopic[topic]++;

                int type = tokens.getIndexAtPosition(position);
                typeTopicCounts[type][topic]++;
                typeCounts[type]++;
                // update the time-related statistics of the new topics

                nPerTopic[topic] ++;
                double delta = time - timeMeanTopic[topic];
                timeMeanTopic[topic] += delta/nPerTopic[topic];
                timeM2PerTopic[topic] += delta*(time - timeMeanTopic[topic]);
            }

            TopicAssignment t = new TopicAssignment (instance, topicSequence);
            data.add (t);
        }

        // update beta distribution of each topic
        updateBetaDistribution();

        // clear statistics for time for each topic
        Arrays.fill(timeMeanTopic, 0.0);
        Arrays.fill(timeM2PerTopic, 0.0);
        Arrays.fill(nPerTopic, 0);

        // extend the time range slightly to avoid numerical issue. (Beta distribution)
        minTime -= (maxTime - minTime)/10;
        maxTime += (maxTime - minTime)/10;


    }

    public void sample (int iterations) throws IOException {

        for (int iteration = 1; iteration <= iterations; iteration++) {

            long iterationStart = System.currentTimeMillis();

            // Loop over every document in the corpus
            for (TopicAssignment aData : data) {
                FeatureSequence tokenSequence =
                        (FeatureSequence) aData.instance.getData();
                LabelSequence topicSequence =
                        (LabelSequence) aData.topicSequence;
                sampleTopicsForOneDoc(tokenSequence, topicSequence, getInstanceTime(aData.instance));
            }
            // update beta distribution of each topic
            updateBetaDistribution();

            // clear statistics for time for each topic
            Arrays.fill(timeMeanTopic, 0.0);
            Arrays.fill(timeM2PerTopic, 0.0);
            Arrays.fill(nPerTopic, 0);

            long elapsedMillis = System.currentTimeMillis() - iterationStart;
            logger.fine(iteration + "\t" + elapsedMillis + "ms\t");

            // Occasionally print more information
            if (showTopicsInterval != 0 && iteration % showTopicsInterval == 0) {
                logger.info("<" + iteration + "> Log Likelihood: " + modelLogLikelihood() + "\n" +
                        topWords (wordsPerTopic));
            }

        }
    }

    protected void sampleTopicsForOneDoc (FeatureSequence tokenSequence,
                                          FeatureSequence topicSequence,
                                          double t) {

        int[] oneDocTopics = topicSequence.getFeatures();

        int[] currentTypeTopicCounts;
        int type, oldTopic, newTopic;
        double topicWeightsSum;
        int docLength = tokenSequence.getLength();

        int[] localTopicCounts = new int[numTopics];

        //		populate topic counts
        for (int position = 0; position < docLength; position++) {
            localTopicCounts[oneDocTopics[position]]++;
        }

        double score, sum;
        double[] topicTermScores = new double[numTopics];

        //	Iterate over the positions (words) in the document
        for (int position = 0; position < docLength; position++) {
            type = tokenSequence.getIndexAtPosition(position);
            oldTopic = oneDocTopics[position];

            // Grab the relevant row from our two-dimensional array
            currentTypeTopicCounts = typeTopicCounts[type];

            //	Remove this token from all counts.
            localTopicCounts[oldTopic]--;
            tokensPerTopic[oldTopic]--;
            assert(tokensPerTopic[oldTopic] >= 0) : "old Topic " + oldTopic + " below 0";
            currentTypeTopicCounts[oldTopic]--;

            // Now calculate and add up the scores for each topic for this word
            sum = 0.0;

            // Here's where the math happens! Note that overall performance is
            //  dominated by what you do in this loop.
            for (int topic = 0; topic < numTopics; topic++) {
                double timeProb =
                        (betaDistributionPerTopic[topic] == null ? 1 : betaDistributionPerTopic[topic].density(t));
                score =
                        (alpha + localTopicCounts[topic]) *
                                ((beta + currentTypeTopicCounts[topic]) /
                                        (betaSum + tokensPerTopic[topic])) *
                                Math.pow(timeProb,expotentialWeightForTime);
                if(Double.isNaN(score)){
                    score = 0;
                }

                sum += score;
                topicTermScores[topic] = score;
            }

            // Choose a random point between 0 and the sum of all topic scores
            double sample = random.nextUniform() * sum;

            // Figure out which topic contains that point
            newTopic = -1;
            while (sample > 0.0) {
                newTopic++;
                sample -= topicTermScores[newTopic];
            }

            // Make sure we actually sampled a topic
            if (newTopic == -1) {
                throw new IllegalStateException ("SimpleLDA: New topic not sampled.");
            }

            // Put that new topic into the counts
            oneDocTopics[position] = newTopic;
            localTopicCounts[newTopic]++;
            tokensPerTopic[newTopic]++;
            currentTypeTopicCounts[newTopic]++;

            // update the time-related statistics of the new topics
            nPerTopic[newTopic] ++;
            double delta = t - timeMeanTopic[newTopic];
            timeMeanTopic[newTopic] += delta/nPerTopic[newTopic];
            timeM2PerTopic[newTopic] += delta*(t - timeMeanTopic[newTopic]);


        }
    }
    public void updateBetaDistribution(){
        for(int i=0; i<numTopics; i++){
            // sample mean and variance
            double m = timeMeanTopic[i];
            double v = timeM2PerTopic[i]/ (nPerTopic[i]-1);

            // method of moments: derive parameters of beta distribution from the sample mean and variance
            double alphaPlusBeta = ((m * (1-m) / v) - 1);
            double alpha = m * alphaPlusBeta;
            double beta = alphaPlusBeta - alpha;
            if(Double.isInfinite(alpha)){
                System.out.println("?????");

            }
            betaDistributionPerTopic[i] = new BetaDistribution(alpha, beta);
        }
    }
    public double modelLogLikelihood() {
        double logLikelihood = 0.0;
        int nonZeroTopics;

        // The likelihood of the model is a combination of a
        // Dirichlet-multinomial for the words in each topic
        // and a Dirichlet-multinomial for the topics in each
        // document.

        // The likelihood function of a dirichlet multinomial is
        //	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
        //	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )

        // So the log likelihood is
        //	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) +
        //	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]

        // Do the documents first

        int[] topicCounts = new int[numTopics];
        double[] topicLogGammas = new double[numTopics];
        int[] docTopics;

        for (int topic=0; topic < numTopics; topic++) {
            topicLogGammas[ topic ] = Dirichlet.logGamma( alpha );
        }

        for (int doc=0; doc < data.size(); doc++) {
            LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;

            docTopics = topicSequence.getFeatures();

            for (int token=0; token < docTopics.length; token++) {
                topicCounts[ docTopics[token] ]++;
            }

            for (int topic=0; topic < numTopics; topic++) {
                if (topicCounts[topic] > 0) {
                    logLikelihood += (Dirichlet.logGamma(alpha + topicCounts[topic]) -
                            topicLogGammas[ topic ]);
                    // the part of log-likelihood for time
                    double logDensity =
                            betaDistributionPerTopic[topic].logDensity(getInstanceTime(data.get(doc).instance));
                    logLikelihood += (expotentialWeightForTime * logDensity * topicCounts[topic]);
                }
            }

            // subtract the (count + parameter) sum term
            logLikelihood -= Dirichlet.logGamma(alphaSum + docTopics.length);

            Arrays.fill(topicCounts, 0);
        }

        // add the parameter sum term
        logLikelihood += data.size() * Dirichlet.logGamma(alphaSum);

        // And the topics

        // Count the number of type-topic pairs
        int nonZeroTypeTopics = 0;

        for (int type=0; type < numTypes; type++) {
            // reuse this array as a pointer

            topicCounts = typeTopicCounts[type];

            for (int topic = 0; topic < numTopics; topic++) {
                if (topicCounts[topic] == 0) { continue; }

                nonZeroTypeTopics++;
                logLikelihood += Dirichlet.logGamma(beta + topicCounts[topic]);

                if (Double.isNaN(logLikelihood)) {
                    System.out.println(topicCounts[topic]);
                    System.exit(1);
                }
            }
        }

        for (int topic=0; topic < numTopics; topic++) {
            logLikelihood -=
                    Dirichlet.logGamma( (beta * numTopics) +
                            tokensPerTopic[ topic ] );
            if (Double.isNaN(logLikelihood)) {
                System.out.println("after topic " + topic + " " + tokensPerTopic[ topic ]);
                System.exit(1);
            }

        }

        logLikelihood +=
                (Dirichlet.logGamma(beta * numTopics)) -
                        (Dirichlet.logGamma(beta) * nonZeroTypeTopics);

        if (Double.isNaN(logLikelihood)) {
            System.out.println("at the end");
            System.exit(1);
        }


        return logLikelihood;
    }

    //
    // Methods for displaying and saving results
    //
    public String topWords (int numWords) {

        StringBuilder output = new StringBuilder();

        IDSorter[] sortedWords = new IDSorter[numTypes];
        double typeEntropy[] = new double[numTypes];

        for (int topic = 0; topic < numTopics; topic++) {
            for (int type = 0; type < numTypes; type++) {
                sortedWords[type] = new IDSorter(type, typeTopicCounts[type][topic] / Math.log(typeCounts[type] + 5));
            }
            Arrays.sort(sortedWords);

            output.append(topic + "\t" + tokensPerTopic[topic] + "\t");
            long meanTime = (long) (((betaDistributionPerTopic[topic].getNumericalMean()) * (maxTime - minTime)) + minTime);
            output.append("Mean Time:" + new DateTime(meanTime) + "\t");
            output.append("Dispersion:" + betaDistributionPerTopic[topic].getNumericalVariance() + "\t");

            for (int i=0; i < numWords; i++) {
                output.append(alphabet.lookupObject(sortedWords[i].getID()) + " ");
            }
            output.append("\n");
        }

        return output.toString();
    }


    /**
     *  @param file        The filename to print to
     *  @param threshold   Only print topics with proportion greater than this number
     *  @param max         Print no more than this many topics
     */
    public void printDocumentTopics (File file, double threshold, int max) throws IOException {
        PrintWriter out = new PrintWriter(file);

        out.print ("#doc source topic proportion ...\n");
        int docLen;
        int[] topicCounts = new int[ numTopics ];

        IDSorter[] sortedTopics = new IDSorter[ numTopics ];
        for (int topic = 0; topic < numTopics; topic++) {
            // Initialize the sorters with dummy values
            sortedTopics[topic] = new IDSorter(topic, topic);
        }

        if (max < 0 || max > numTopics) {
            max = numTopics;
        }

        for (int doc = 0; doc < data.size(); doc++) {
            LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;
            int[] currentDocTopics = topicSequence.getFeatures();

            out.print (doc); out.print (' ');

            if (data.get(doc).instance.getSource() != null) {
                out.print (data.get(doc).instance.getSource());
            }
            else {
                out.print ("null-source");
            }

            out.print (' ');
            docLen = currentDocTopics.length;

            // Count up the tokens
            for (int token=0; token < docLen; token++) {
                topicCounts[ currentDocTopics[token] ]++;
            }

            // And normalize
            for (int topic = 0; topic < numTopics; topic++) {
                sortedTopics[topic].set(topic, (float) topicCounts[topic] / docLen);
            }

            Arrays.sort(sortedTopics);

            for (int i = 0; i < max; i++) {
                if (sortedTopics[i].getWeight() < threshold) { break; }

                out.print (sortedTopics[i].getID() + " " +
                        sortedTopics[i].getWeight() + " ");
            }
            out.print (" \n");

            Arrays.fill(topicCounts, 0);
        }

    }

    public void printState (File f) throws IOException {
        PrintStream out =
                new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(f))));
        printState(out);
        out.close();
    }

    public void printState (PrintStream out) {

        out.println ("#doc source pos typeindex type topic");

        for (int doc = 0; doc < data.size(); doc++) {
            FeatureSequence tokenSequence =	(FeatureSequence) data.get(doc).instance.getData();
            LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

            String source = "NA";
            if (data.get(doc).instance.getSource() != null) {
                source = data.get(doc).instance.getSource().toString();
            }

            for (int position = 0; position < topicSequence.getLength(); position++) {
                int type = tokenSequence.getIndexAtPosition(position);
                int topic = topicSequence.getIndexAtPosition(position);
                out.print(doc); out.print(' ');
                out.print(source); out.print(' ');
                out.print(position); out.print(' ');
                out.print(type); out.print(' ');
                out.print(alphabet.lookupObject(type)); out.print(' ');
                out.print(topic); out.println();
            }
        }
    }


    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;
    private static final int NULL_INTEGER = -1;

    public void write (File f) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream(f));
            oos.writeObject(this);
            oos.close();
        }
        catch (IOException e) {
            System.err.println("Exception writing file " + f + ": " + e);
        }
    }

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt (CURRENT_SERIAL_VERSION);

        // Instance lists
        out.writeObject (data);
        out.writeObject (alphabet);
        out.writeObject (topicAlphabet);

        out.writeInt (numTopics);
        out.writeObject (alpha);
        out.writeDouble (beta);
        out.writeDouble (betaSum);

        out.writeInt(showTopicsInterval);
        out.writeInt(wordsPerTopic);

        out.writeObject(random);
        out.writeObject(formatter);
        out.writeBoolean(printLogLikelihood);

        out.writeObject (typeTopicCounts);

        for (int ti = 0; ti < numTopics; ti++) {
            out.writeInt (tokensPerTopic[ti]);
        }
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
        int featuresLength;
        int version = in.readInt ();

        data = (ArrayList<TopicAssignment>) in.readObject ();
        alphabet = (Alphabet) in.readObject();
        topicAlphabet = (LabelAlphabet) in.readObject();

        numTopics = in.readInt();
        alpha = in.readDouble();
        alphaSum = alpha * numTopics;
        beta = in.readDouble();
        betaSum = in.readDouble();

        showTopicsInterval = in.readInt();
        wordsPerTopic = in.readInt();

        random = (Randoms) in.readObject();
        formatter = (NumberFormat) in.readObject();
        printLogLikelihood = in.readBoolean();

        int numDocs = data.size();
        this.numTypes = alphabet.size();

        typeTopicCounts = (int[][]) in.readObject();
        tokensPerTopic = new int[numTopics];
        for (int ti = 0; ti < numTopics; ti++) {
            tokensPerTopic[ti] = in.readInt();
        }
    }

    public static void main (String[] args) throws IOException {

        InstanceList training = InstanceList.load (new File(args[0]));

	for (Instance instance : training) {
		//System.out.println(instance.getName().toString());
		//System.out.println(name);
		String name = instance.getName().toString();
		instance.setProperty("timestamp", Long.valueOf(name.substring(name.length() - 17, name.length() - 4)));
		//System.out.println(instance.getProperty("timestamp"));
	}

        int numTopics = args.length > 1 ? Integer.parseInt(args[1]) : 50;

        SimpleTimeOverTopic tot = new SimpleTimeOverTopic (numTopics, 50.0, 0.01, 0.5);
        tot.addInstances(training);
        tot.sample(1000);
    }

}
